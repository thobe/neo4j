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
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalUnit;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.neo4j.values.AnyValue;
import org.neo4j.values.ValueMapper;

import static java.time.Instant.ofEpochSecond;
import static java.time.LocalDateTime.ofInstant;
import static java.time.ZoneOffset.UTC;
import static java.util.Objects.requireNonNull;
import static org.neo4j.values.storable.DateValue.DATE_PATTERN;
import static org.neo4j.values.storable.DateValue.parseDate;
import static org.neo4j.values.storable.LocalTimeValue.TIME_PATTERN;
import static org.neo4j.values.storable.LocalTimeValue.parseTime;

public final class LocalDateTimeValue extends TemporalValue<LocalDateTime,LocalDateTimeValue>
{
    public static LocalDateTimeValue localDateTime( DateValue date, LocalTimeValue time )
    {
        return new LocalDateTimeValue( LocalDateTime.of( date.temporal(), time.temporal() ) );
    }

    public static LocalDateTimeValue localDateTime(
            int year, int month, int day, int hour, int minute, int second, int nanoOfSecond )
    {
        return new LocalDateTimeValue( LocalDateTime.of( year, month, day, hour, minute, second, nanoOfSecond ) );
    }

    public static LocalDateTimeValue localDateTime( LocalDateTime value )
    {
        return new LocalDateTimeValue( requireNonNull( value, "LocalDateTime" ) );
    }

    public static LocalDateTimeValue localDateTime( long epochSecond, long nano )
    {
        return new LocalDateTimeValue( ofInstant( ofEpochSecond( epochSecond, nano ), UTC ) );
    }

    public static LocalDateTimeValue inUTC( DateTimeValue datetime )
    {
        return new LocalDateTimeValue( datetime.temporal().withZoneSameInstant( UTC ).toLocalDateTime() );
    }

    public static LocalDateTimeValue parse( CharSequence text )
    {
        return parse( LocalDateTimeValue.class, PATTERN, LocalDateTimeValue::parse, text );
    }

    public static LocalDateTimeValue parse( TextValue text )
    {
        return parse( LocalDateTimeValue.class, PATTERN, LocalDateTimeValue::parse, text );
    }

    public static StructureBuilder<AnyValue,LocalDateTimeValue> builder( Function<String,Clock> clockProvider )
    {
        return new DateTimeValue.DateTimeBuilder<AnyValue,Clock,LocalDateTimeValue>()
        {
            @Override
            protected LocalDateTimeValue fromSingle( AnyValue input )
            {
                return singleValue( ( text, zone ) -> parse( text ), "localdatetime", input );
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
            protected LocalDateTimeValue now()
            {
                return localDateTime( LocalDateTime.now( clock() ) );
            }

            @Override
            protected LocalDateTimeValue fromEpoch( AnyValue epoch, boolean milli, AnyValue nano )
            {
                throw new UnsupportedOperationException( "not implemented" );
            }

            @Override
            protected LocalDateTimeValue selectDateTime( AnyValue temporal )
            {
                throw new UnsupportedOperationException( "not implemented" );
            }

            @Override
            protected LocalDateTimeValue selectDateAndTime( AnyValue date, AnyValue time )
            {
                throw new UnsupportedOperationException( "not implemented" );
            }

            @Override
            protected LocalDateTimeValue selectDateWithConstructedTime(
                    AnyValue date,
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
            protected LocalDateTimeValue selectDate( AnyValue temporal )
            {
                throw new UnsupportedOperationException( "not implemented" );
            }

            @Override
            protected LocalDateTimeValue constructYear( AnyValue year )
            {
                throw new UnsupportedOperationException( "not implemented" );
            }

            @Override
            protected LocalDateTimeValue constructCalendarDate( AnyValue year, AnyValue month, AnyValue day )
            {
                throw new UnsupportedOperationException( "not implemented" );
            }

            @Override
            protected LocalDateTimeValue constructCalendarDateWithSelectedTime(
                    AnyValue year, AnyValue month, AnyValue day, AnyValue time )
            {
                throw new UnsupportedOperationException( "not implemented" );
            }

            @Override
            protected LocalDateTimeValue constructCalendarDateWithConstructedTime(
                    AnyValue year,
                    AnyValue month,
                    AnyValue day,
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
            protected LocalDateTimeValue constructWeekDate( AnyValue year, AnyValue week, AnyValue dayOfWeek )
            {
                throw new UnsupportedOperationException( "not implemented" );
            }

            @Override
            protected LocalDateTimeValue constructWeekDateWithSelectedTime(
                    AnyValue year, AnyValue week, AnyValue dayOfWeek, AnyValue time )
            {
                throw new UnsupportedOperationException( "not implemented" );
            }

            @Override
            protected LocalDateTimeValue constructWeekDateWithConstructedTime(
                    AnyValue year,
                    AnyValue week,
                    AnyValue dayOfWeek,
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
            protected LocalDateTimeValue constructQuarterDate(
                    AnyValue year, AnyValue quarter, AnyValue dayOfQuarter )
            {
                throw new UnsupportedOperationException( "not implemented" );
            }

            @Override
            protected LocalDateTimeValue constructQuarterDateWithSelectedTime(
                    AnyValue year, AnyValue quarter, AnyValue dayOfQuarter, AnyValue time )
            {
                throw new UnsupportedOperationException( "not implemented" );
            }

            @Override
            protected LocalDateTimeValue constructQuarterDateWithConstructedTime(
                    AnyValue year,
                    AnyValue quarter,
                    AnyValue dayOfQuarter,
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
            protected LocalDateTimeValue constructOrdinalDate( AnyValue year, AnyValue ordinalDay )
            {
                throw new UnsupportedOperationException( "not implemented" );
            }

            @Override
            protected LocalDateTimeValue constructOrdinalDateWithSelectedTime(
                    AnyValue year, AnyValue ordinalDay, AnyValue time )
            {
                throw new UnsupportedOperationException( "not implemented" );
            }

            @Override
            protected LocalDateTimeValue constructOrdinalDateWithConstructedTime(
                    AnyValue year,
                    AnyValue ordinalDay,
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
            protected LocalDateTimeValue truncate( TemporalUnit unit, AnyValue temporal )
            {
                throw new UnsupportedOperationException( "not implemented" );
            }

            @Override
            protected LocalDateTimeValue truncateWithSelectedTime(
                    TemporalUnit unit, AnyValue temporal, AnyValue time )
            {
                throw new UnsupportedOperationException( "not implemented" );
            }

            @Override
            protected LocalDateTimeValue truncateWithCalendarDateAndSelectedTime(
                    TemporalUnit unit, AnyValue temporal, AnyValue month, AnyValue day, AnyValue time )
            {
                throw new UnsupportedOperationException( "not implemented" );
            }

            @Override
            protected LocalDateTimeValue truncateWithWeekDateAndSelectedTime(
                    TemporalUnit unit, AnyValue temporal, AnyValue week, AnyValue dayOfWeek, AnyValue time )
            {
                throw new UnsupportedOperationException( "not implemented" );
            }

            @Override
            protected LocalDateTimeValue truncateWithQuarterDateAndSelectedTime(
                    TemporalUnit unit,
                    AnyValue temporal,
                    AnyValue quarter,
                    AnyValue dayOfQuarter,
                    AnyValue time )
            {
                throw new UnsupportedOperationException( "not implemented" );
            }

            @Override
            protected LocalDateTimeValue truncateWithOrdinalDateAndSelectedTime(
                    TemporalUnit unit, AnyValue temporal, AnyValue ordinalDay, AnyValue time )
            {
                throw new UnsupportedOperationException( "not implemented" );
            }

            @Override
            protected LocalDateTimeValue truncateWithConstructedTime(
                    TemporalUnit unit,
                    AnyValue temporal,
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
            protected LocalDateTimeValue truncateWithCalendarDate(
                    TemporalUnit unit, AnyValue temporal, AnyValue month, AnyValue day )
            {
                throw new UnsupportedOperationException( "not implemented" );
            }

            @Override
            protected LocalDateTimeValue truncateWithCalendarDateAndConstructedTime(
                    TemporalUnit unit,
                    AnyValue temporal,
                    AnyValue month,
                    AnyValue day,
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
            protected LocalDateTimeValue truncateWithWeekDate(
                    TemporalUnit unit, AnyValue temporal, AnyValue week, AnyValue dayOfWeek )
            {
                throw new UnsupportedOperationException( "not implemented" );
            }

            @Override
            protected LocalDateTimeValue truncateWithWeekDateAndConstructedTime(
                    TemporalUnit unit,
                    AnyValue temporal,
                    AnyValue week,
                    AnyValue dayOfWeek,
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
            protected LocalDateTimeValue truncateWithQuarterDate(
                    TemporalUnit unit, AnyValue temporal, AnyValue quarter, AnyValue dayOfQuarter )
            {
                throw new UnsupportedOperationException( "not implemented" );
            }

            @Override
            protected LocalDateTimeValue truncateWithQuarterDateAndConstructedTime(
                    TemporalUnit unit,
                    AnyValue temporal,
                    AnyValue quarter,
                    AnyValue dayOfQuarter,
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
            protected LocalDateTimeValue truncateWithOrdinalDate(
                    TemporalUnit unit, AnyValue temporal, AnyValue ordinalDay )
            {
                throw new UnsupportedOperationException( "not implemented" );
            }

            @Override
            protected LocalDateTimeValue truncateWithOrdinalDateAndConstructedTime(
                    TemporalUnit unit,
                    AnyValue temporal,
                    AnyValue ordinalDay,
                    AnyValue hour,
                    AnyValue minute,
                    AnyValue second,
                    AnyValue millisecond,
                    AnyValue microsecond,
                    AnyValue nanosecond )
            {
                throw new UnsupportedOperationException( "not implemented" );
            }
        };
    }

    public abstract static class Compiler<Input> extends DateTimeValue.DateTimeBuilder<Input,Void,MethodHandle>
    {
    }

    private final LocalDateTime value;

    private LocalDateTimeValue( LocalDateTime value )
    {
        this.value = value;
    }

    @Override
    LocalDateTime temporal()
    {
        return value;
    }

    @Override
    public boolean equals( Value other )
    {
        return other instanceof LocalDateTimeValue && value.equals( ((LocalDateTimeValue) other).value );
    }

    @Override
    public <E extends Exception> void writeTo( ValueWriter<E> writer ) throws E
    {
        writer.writeLocalDateTime( value.toEpochSecond( UTC ), value.getNano() );
    }

    @Override
    public String prettyPrint()
    {
        return value.format( DateTimeFormatter.ISO_LOCAL_DATE_TIME );
    }

    @Override
    public ValueGroup valueGroup()
    {
        return ValueGroup.LOCAL_DATE_TIME;
    }

    @Override
    protected int computeHash()
    {
        return Long.hashCode( value.toEpochSecond( UTC ) );
    }

    @Override
    public <T> T map( ValueMapper<T> mapper )
    {
        return mapper.mapLocalDateTime( this );
    }

    @Override
    public LocalDateTimeValue add( DurationValue duration )
    {
        return replacement( value.plus( duration ) );
    }

    @Override
    public LocalDateTimeValue sub( DurationValue duration )
    {
        return replacement( value.minus( duration ) );
    }

    @Override
    LocalDateTimeValue replacement( LocalDateTime dateTime )
    {
        return dateTime == value ? this : new LocalDateTimeValue( dateTime );
    }

    private static final Pattern PATTERN = Pattern.compile(
            DATE_PATTERN + "(?<time>T" + TIME_PATTERN + ")?",
            Pattern.CASE_INSENSITIVE );

    private static LocalDateTimeValue parse( Matcher matcher )
    {
        return localDateTime( LocalDateTime.of( parseDate( matcher ), optTime( matcher ) ) );
    }

    static LocalTime optTime( Matcher matcher )
    {
        return matcher.group( "time" ) != null ? parseTime( matcher ) : LocalTime.MIN;
    }
}
