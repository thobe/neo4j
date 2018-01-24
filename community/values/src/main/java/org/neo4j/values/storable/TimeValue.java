/*
 * Copyright (c) 2002-2018 "Neo Technology,"
 * Network Engine for Objects in Lund AB [http://neotechnology.com]
 *
 * This file is part of Neo4j.
 *
 * Neo4j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.neo4j.values.storable;

import java.lang.invoke.MethodHandle;
import java.time.Clock;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.OffsetTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalUnit;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.neo4j.values.AnyValue;
import org.neo4j.values.ValueMapper;

import static java.lang.Integer.parseInt;
import static java.time.ZoneOffset.UTC;
import static java.util.Objects.requireNonNull;
import static org.neo4j.values.storable.DurationValue.SECONDS_PER_DAY;
import static org.neo4j.values.storable.LocalTimeValue.optInt;
import static org.neo4j.values.storable.LocalTimeValue.parseTime;

public final class TimeValue extends TemporalValue<OffsetTime,TimeValue>
{
    public static TimeValue time( OffsetTime time )
    {
        return new TimeValue( requireNonNull( time, "OffsetTime" ) );
    }

    public static TimeValue time( int hour, int minute, int second, int nanosOfSecond, ZoneOffset offset )
    {
        return new TimeValue( OffsetTime.of( hour, minute, second, nanosOfSecond, offset ) );
    }

    public static TimeValue time( long nanosOfDayUTC, ZoneOffset offset )
    {
        return new TimeValue( OffsetTime.ofInstant( Instant.ofEpochSecond( 0, nanosOfDayUTC ), offset ) );
    }

    public static TimeValue parse( CharSequence text, Supplier<ZoneId> defaultZone )
    {
        return parse( TimeValue.class, PATTERN, TimeValue::parse, text, defaultZone );
    }

    public static TimeValue parse( TextValue text, Supplier<ZoneId> defaultZone )
    {
        return parse( TimeValue.class, PATTERN, TimeValue::parse, text, defaultZone );
    }

    public static StructureBuilder<AnyValue,TimeValue> builder( Function<String,Clock> clockProvider )
    {
        return new TimeBuilder<AnyValue,Clock,TimeValue>()
        {
            @Override
            protected TimeValue fromSingle( AnyValue input )
            {
                return singleValue( TimeValue::parse, "time", input );
            }

            @Override
            protected Clock clock( AnyValue when, AnyValue timezone )
            {
                Clock clock = clockProvider.apply( when( when ) );
                if ( timezone != null )
                {
                    clock = clock.withZone( timezoneOf( timezone ) );
                }
                return clock;
            }

            @Override
            protected ZoneId timezone( AnyValue timezone )
            {
                return timezone == null ? clockProvider.apply( when( null ) ).getZone() : timezoneOf( timezone );
            }

            @Override
            protected TimeValue now()
            {
                return time( OffsetTime.now( clock() ) );
            }

            @Override
            protected TimeValue fromEpoch( AnyValue epoch, boolean milli, AnyValue nano )
            {
                throw new UnsupportedOperationException( "not implemented" );
            }

            @Override
            protected TimeValue selectTime( AnyValue temporal )
            {
                OffsetTime time = offsetTime( temporal );
                return time != null ? time( time ) : null;
            }

            @Override
            protected TimeValue constructTime(
                    AnyValue hour,
                    AnyValue minute,
                    AnyValue second,
                    AnyValue millisecond,
                    AnyValue microsecond,
                    AnyValue nanosecond )
            {
                throw new UnsupportedOperationException( "not implemented" );
            }

            @Override
            protected TimeValue truncate( TemporalUnit unit, AnyValue temporal )
            {
                OffsetTime time = offsetTime( temporal );
                return time != null ? time( time.truncatedTo( unit ) ) : null;
            }

            private OffsetTime offsetTime( AnyValue value )
            {
                if ( value instanceof TemporalValue )
                {
                    try
                    {
                        return OffsetTime.from( ((TemporalValue) value).temporal() );
                    }
                    catch ( DateTimeException e )
                    {
                        return null;
                    }
                }
                else
                {
                    return null;
                }
            }
        };
    }

    public abstract static class Compiler<Input> extends TimeBuilder<Input,Void,MethodHandle>
    {
    }

    private final OffsetTime value;

    private TimeValue( OffsetTime value )
    {
        this.value = value;
    }

    @Override
    OffsetTime temporal()
    {
        return value;
    }

    @Override
    public boolean equals( Value other )
    {
        // TODO: do we want equality to be this permissive?
        // This means that time("14:30+0100") = time("15:30+0200")
        return other instanceof TimeValue && value.isEqual( ((TimeValue) other).value );
    }

    @Override
    public <E extends Exception> void writeTo( ValueWriter<E> writer ) throws E
    {
        int offset = value.getOffset().getTotalSeconds();
        long seconds = value.getLong( ChronoField.SECOND_OF_DAY );
        seconds = ((-offset % SECONDS_PER_DAY) + seconds + SECONDS_PER_DAY) % SECONDS_PER_DAY;
        long nano = seconds * DurationValue.NANOS_PER_SECOND + value.getNano();
        writer.writeTime( nano, offset );
    }

    @Override
    public String prettyPrint()
    {
        return value.format( DateTimeFormatter.ISO_TIME );
    }

    @Override
    public ValueGroup valueGroup()
    {
        return ValueGroup.ZONED_TIME;
    }

    @Override
    protected int computeHash()
    {
        return Long.hashCode( value.toLocalTime().toNanoOfDay() - value.getOffset().getTotalSeconds() * 1000_000_000L );
    }

    @Override
    public <T> T map( ValueMapper<T> mapper )
    {
        return mapper.mapTime( this );
    }

    @Override
    public TimeValue add( DurationValue duration )
    {
        return replacement( value.plusNanos( duration.nanosOfDay() ) );
    }

    @Override
    public TimeValue sub( DurationValue duration )
    {
        return replacement( value.minusNanos( duration.nanosOfDay() ) );
    }

    @Override
    TimeValue replacement( OffsetTime time )
    {
        return time == value ? this : new TimeValue( time );
    }

    private static final String OFFSET_PATTERN = "(?<zone>Z|[+-](?<zoneHour>[0-9]{2})(?::?(?<zoneMinute>[0-9]{2}))?)";
    static final String TIME_PATTERN = LocalTimeValue.TIME_PATTERN + "(?:" + OFFSET_PATTERN + ")?";
    private static final Pattern PATTERN = Pattern.compile( "(?:T)?" + TIME_PATTERN );

    static ZoneOffset parseOffset( Matcher matcher )
    {
        String zone = matcher.group( "zone" );
        if ( null == zone )
        {
            return null;
        }
        if ( "Z".equalsIgnoreCase( zone ) )
        {
            return UTC;
        }
        int factor = zone.charAt( 0 ) == '+' ? 1 : -1;
        int hours = parseInt( matcher.group( "zoneHour" ) );
        int minutes = optInt( matcher.group( "zoneMinute" ) );
        return ZoneOffset.ofHoursMinutes( factor * hours, factor * minutes );
    }

    private static TimeValue parse( Matcher matcher, Supplier<ZoneId> defaultZone )
    {
        return new TimeValue( OffsetTime.of( parseTime( matcher ), zoneOffset( parseOffset( matcher, defaultZone ) ) ) );
    }

    private static ZoneId parseOffset( Matcher matcher, Supplier<ZoneId> defaultZone )
    {
        ZoneOffset offset = parseOffset( matcher );
        return offset != null ? offset : defaultZone.get();
    }

    private static ZoneOffset zoneOffset( ZoneId zoneId )
    {
        return zoneId instanceof ZoneOffset ? (ZoneOffset) zoneId : zoneId.getRules().getOffset( Instant.now() );
    }

    abstract static class TimeBuilder<Input, CLOCK, Result> extends Builder<Input,CLOCK,Result>
    {
        @Override
        protected final boolean supportsDate()
        {
            return false;
        }

        @Override
        protected final boolean supportsTime()
        {
            return true;
        }

        @Override
        protected Result selectDateTime( Input temporal )
        {
            throw new IllegalStateException( "date not supported" );
        }

        @Override
        protected Result selectDateAndTime( Input date, Input time )
        {
            throw new IllegalStateException( "date not supported" );
        }

        @Override
        protected Result selectDateWithConstructedTime(
                Input date,
                Input hour,
                Input minute,
                Input second,
                Input millisecond,
                Input microsecond,
                Input nanosecond )
        {
            throw new IllegalStateException( "date not supported" );
        }

        @Override
        protected Result selectDate( Input temporal )
        {
            throw new IllegalStateException( "date not supported" );
        }

        @Override
        protected Result constructYear( Input year )
        {
            throw new IllegalStateException( "date not supported" );
        }

        @Override
        protected Result constructCalendarDate( Input year, Input month, Input day )
        {
            throw new IllegalStateException( "date not supported" );
        }

        @Override
        protected Result constructCalendarDateWithSelectedTime( Input year, Input month, Input day, Input time )
        {
            throw new IllegalStateException( "date not supported" );
        }

        @Override
        protected Result constructCalendarDateWithConstructedTime(
                Input year,
                Input month,
                Input day,
                Input hour,
                Input minute,
                Input second,
                Input millisecond,
                Input microsecond,
                Input nanosecond )
        {
            throw new IllegalStateException( "date not supported" );
        }

        @Override
        protected Result constructWeekDate( Input year, Input week, Input dayOfWeek )
        {
            throw new IllegalStateException( "date not supported" );
        }

        @Override
        protected Result constructWeekDateWithSelectedTime( Input year, Input week, Input dayOfWeek, Input time )
        {
            throw new IllegalStateException( "date not supported" );
        }

        @Override
        protected Result constructWeekDateWithConstructedTime(
                Input year,
                Input week,
                Input dayOfWeek,
                Input hour,
                Input minute,
                Input second,
                Input millisecond,
                Input microsecond,
                Input nanosecond )
        {
            throw new IllegalStateException( "date not supported" );
        }

        @Override
        protected Result constructQuarterDate( Input year, Input quarter, Input dayOfQuarter )
        {
            throw new IllegalStateException( "date not supported" );
        }

        @Override
        protected Result constructQuarterDateWithSelectedTime(
                Input year,
                Input quarter,
                Input dayOfQuarter,
                Input time )
        {
            throw new IllegalStateException( "date not supported" );
        }

        @Override
        protected Result constructQuarterDateWithConstructedTime(
                Input year,
                Input quarter,
                Input dayOfQuarter,
                Input hour,
                Input minute,
                Input second,
                Input millisecond,
                Input microsecond,
                Input nanosecond )
        {
            throw new IllegalStateException( "date not supported" );
        }

        @Override
        protected Result constructOrdinalDate( Input year, Input ordinalDay )
        {
            throw new IllegalStateException( "date not supported" );
        }

        @Override
        protected Result constructOrdinalDateWithSelectedTime( Input year, Input ordinalDay, Input time )
        {
            throw new IllegalStateException( "date not supported" );
        }

        @Override
        protected Result constructOrdinalDateWithConstructedTime(
                Input year,
                Input ordinalDay,
                Input hour,
                Input minute,
                Input second,
                Input millisecond,
                Input microsecond,
                Input nanosecond )
        {
            throw new IllegalStateException( "date not supported" );
        }

        @Override
        protected Result truncateWithSelectedTime( TemporalUnit unit, Input temporal, Input time )
        {
            throw new IllegalStateException( "cannot truncate time and select time" );
        }

        @Override
        protected Result truncateWithCalendarDateAndSelectedTime(
                TemporalUnit unit, Input temporal, Input month, Input day, Input time )
        {
            throw new IllegalStateException( "date not supported" );
        }

        @Override
        protected Result truncateWithWeekDateAndSelectedTime(
                TemporalUnit unit, Input temporal, Input week, Input dayOfWeek, Input time )
        {
            throw new IllegalStateException( "date not supported" );
        }

        @Override
        protected Result truncateWithQuarterDateAndSelectedTime(
                TemporalUnit unit, Input temporal, Input quarter, Input dayOfQuarter, Input time )
        {
            throw new IllegalStateException( "date not supported" );
        }

        @Override
        protected Result truncateWithOrdinalDateAndSelectedTime(
                TemporalUnit unit, Input temporal, Input ordinalDay, Input time )
        {
            throw new IllegalStateException( "date not supported" );
        }

        @Override
        protected Result truncateWithConstructedTime(
                TemporalUnit unit,
                Input temporal,
                Input hour,
                Input minute,
                Input second,
                Input millisecond,
                Input microsecond,
                Input nanosecond )
        {
            throw new IllegalStateException( "date not supported" );
        }

        @Override
        protected Result truncateWithCalendarDateAndConstructedTime(
                TemporalUnit unit,
                Input temporal,
                Input month,
                Input day,
                Input hour,
                Input minute,
                Input second,
                Input millisecond,
                Input microsecond,
                Input nanosecond )
        {
            throw new IllegalStateException( "date not supported" );
        }

        @Override
        protected Result truncateWithWeekDateAndConstructedTime(
                TemporalUnit unit,
                Input temporal,
                Input week,
                Input dayOfWeek,
                Input hour,
                Input minute,
                Input second,
                Input millisecond,
                Input microsecond,
                Input nanosecond )
        {
            throw new IllegalStateException( "date not supported" );
        }

        @Override
        protected Result truncateWithQuarterDateAndConstructedTime(
                TemporalUnit unit,
                Input temporal,
                Input quarter,
                Input dayOfQuarter,
                Input hour,
                Input minute,
                Input second,
                Input millisecond,
                Input microsecond,
                Input nanosecond )
        {
            throw new IllegalStateException( "date not supported" );
        }

        @Override
        protected Result truncateWithOrdinalDateAndConstructedTime(
                TemporalUnit unit,
                Input temporal,
                Input ordinalDay,
                Input hour,
                Input minute,
                Input second,
                Input millisecond,
                Input microsecond,
                Input nanosecond )
        {
            throw new IllegalStateException( "date not supported" );
        }

        @Override
        protected Result truncateWithCalendarDate(
                TemporalUnit unit, Input temporal, Input month, Input day )
        {
            throw new IllegalStateException( "date not supported" );
        }

        @Override
        protected Result truncateWithWeekDate(
                TemporalUnit unit, Input temporal, Input week, Input dayOfWeek )
        {
            throw new IllegalStateException( "date not supported" );
        }

        @Override
        protected Result truncateWithQuarterDate(
                TemporalUnit unit, Input temporal, Input quarter, Input dayOfQuarter )
        {
            throw new IllegalStateException( "date not supported" );
        }

        @Override
        protected Result truncateWithOrdinalDate( TemporalUnit unit, Input temporal, Input ordinalDay )
        {
            throw new IllegalStateException( "date not supported" );
        }
    }
}
