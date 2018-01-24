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

abstract class DurationBuilder<Input, Result> extends StructureBuilder<Input,Result>
{
    private State<Input> state;

    @Override
    public Result build( Input single )
    {
        if ( state != null )
        {
            throw new IllegalStateException( "Cannot build from single parameter after supplying named parameters" );
        }
        return createDuration( single );
    }

    abstract Result createDuration( Input input );

    @Override
    public final StructureBuilder<Input,Result> add( String field, Input value )
    {
        state = Field.valueOf( field ).assign( state, value );
        return this;
    }

    @Override
    public final Result build()
    {
        if ( state == null )
        {
            throw new IllegalArgumentException( "No arguments given." );
        }
        return state.build( this );
    }

    abstract Result create(
            Input years,
            Input months,
            Input weeks,
            Input days,
            Input hours,
            Input minutes,
            Input seconds,
            Input milliseconds,
            Input microseconds,
            Input nanoseconds );

    abstract Result between( DurationValue.Difference type, Input from, Input to );

    private enum Field
    {
        // construction
        years( Type.CONSTRUCTION, null ),
        months( Type.CONSTRUCTION, null ),
        weeks( Type.CONSTRUCTION, null ),
        days( Type.CONSTRUCTION, null ),
        hours( Type.CONSTRUCTION, null ),
        minutes( Type.CONSTRUCTION, null ),
        seconds( Type.CONSTRUCTION, null ),
        milliseconds( Type.CONSTRUCTION, null ),
        microseconds( Type.CONSTRUCTION, null ),
        nanoseconds( Type.CONSTRUCTION, null ),
        // between
        from( Type.FROM, DurationValue.Difference.DEFAULT ),
        to( Type.TO, null ),
        yearsFrom( Type.FROM, DurationValue.Difference.years ),
        monthsFrom( Type.FROM, DurationValue.Difference.months ),
        weeksFrom( Type.FROM, DurationValue.Difference.weeks ),
        daysFrom( Type.FROM, DurationValue.Difference.days ),
        hoursFrom( Type.FROM, DurationValue.Difference.hours ),
        minutesFrom( Type.FROM, DurationValue.Difference.minutes ),
        secondsFrom( Type.FROM, DurationValue.Difference.seconds );
        private final Type type;
        private final DurationValue.Difference difference;

        Field( Type type, DurationValue.Difference difference )
        {
            this.type = type;
            this.difference = difference;
        }

        <Input> State<Input> assign( State<Input> state, Input value )
        {
            if ( state == null )
            {
                state = type.state();
            }
            type.assign( this, state, value );
            return state;
        }

        private enum Type
        {
            CONSTRUCTION // <pre>
            { //</pre>

                @Override
                <Input> State<Input> state()
                {
                    return new State.Construction<>();
                }

                @Override
                <Input> void assign( Field field, State<Input> state, Input value )
                {
                    if ( state instanceof State.Construction<?> )
                    {
                        @SuppressWarnings( "unchecked" )
                        State.Construction<Input> construction = (State.Construction<Input>) state;
                        switch ( field )
                        {
                        case years:
                            construction.years = value;
                            break;
                        case months:
                            construction.months = value;
                            break;
                        case weeks:
                            construction.weeks = value;
                            break;
                        case days:
                            construction.days = value;
                            break;
                        case hours:
                            construction.hours = value;
                            break;
                        case minutes:
                            construction.minutes = value;
                            break;
                        case seconds:
                            construction.seconds = value;
                            break;
                        case milliseconds:
                            construction.milliseconds = value;
                            break;
                        case microseconds:
                            construction.microseconds = value;
                            break;
                        case nanoseconds:
                            construction.nanoseconds = value;
                            break;
                        default:
                            throw new IllegalStateException( "Should have received a construction field." );
                        }
                    }
                    else
                    {
                        throw new IllegalArgumentException(
                                "Cannot assign " + field + " along with from/to fields." );
                    }
                }
            },
            FROM // <pre>
            { //</pre>

                @Override
                <Input> State<Input> state()
                {
                    return new State.Between<>();
                }

                @Override
                <Input> void assign( Field field, State<Input> state, Input value )
                {
                    if ( state instanceof State.Between<?> )
                    {
                        @SuppressWarnings( "unchecked" )
                        State.Between<Input> between = (State.Between<Input>) state;
                        if ( between.from == null )
                        {
                            between.from = value;
                            between.type = field.difference;
                        }
                        else
                        {
                            throw new IllegalArgumentException(
                                    "Cannot assign both " + between.type + " and " + field );
                        }
                    }
                    else
                    {
                        throw new IllegalArgumentException(
                                "Cannot assign " + field + " along with direct construction fields." );
                    }
                }
            },
            TO // <pre>
            { //</pre>

                @Override
                <Input> State<Input> state()
                {
                    return new State.Between<>();
                }

                @Override
                <Input> void assign( Field field, State<Input> state, Input value )
                {
                    if ( state instanceof State.Between<?> )
                    {
                        @SuppressWarnings( "unchecked" )
                        State.Between<Input> between = (State.Between<Input>) state;
                        if ( between.to == null )
                        {
                            between.to = value;
                        }
                        else
                        {
                            throw new IllegalArgumentException(
                                    "Cannot assign both " + between.type + " and " + field );
                        }
                    }
                    else
                    {
                        throw new IllegalArgumentException(
                                "Cannot assign " + field + " along with direct construction fields." );
                    }
                }
            };

            abstract <Input> State<Input> state();

            abstract <Input> void assign( Field field, State<Input> state, Input value );
        }
    }

    private abstract static class State<Input>
    {
        abstract <Result> Result build( DurationBuilder<Input,Result> builder );

        static class Construction<Input> extends State<Input>
        {
            Input years;
            Input months;
            Input weeks;
            Input days;
            Input hours;
            Input minutes;
            Input seconds;
            Input milliseconds;
            Input microseconds;
            Input nanoseconds;

            @Override
            <Result> Result build( DurationBuilder<Input,Result> builder )
            {
                return builder.create(
                        years,
                        months,
                        weeks,
                        days,
                        hours,
                        minutes,
                        seconds,
                        milliseconds,
                        microseconds,
                        nanoseconds );
            }
        }

        static class Between<Input> extends State<Input>
        {
            Input from;
            Input to;
            DurationValue.Difference type;

            @Override
            <Result> Result build( DurationBuilder<Input,Result> builder )
            {
                return builder.between( type, from, to );
            }
        }
    }
}
