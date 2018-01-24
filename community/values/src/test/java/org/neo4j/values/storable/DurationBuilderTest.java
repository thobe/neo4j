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

import java.time.ZoneId;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.neo4j.helpers.collection.MapUtil.entry;
import static org.neo4j.values.storable.DateTimeValue.datetime;
import static org.neo4j.values.storable.DateValue.date;
import static org.neo4j.values.storable.DurationValue.build;
import static org.neo4j.values.storable.DurationValue.parse;
import static org.neo4j.values.storable.LocalTimeValue.localTime;
import static org.neo4j.values.storable.Values.of;

public class DurationBuilderTest
{
    @Test
    public void shouldBuildDuration() throws Exception
    {
        assertEquals( parse( "P17Y" ), build( entry( "years", of( 17 ) ).create() ) );
        assertEquals( parse( "P3M" ), build( entry( "months", of( 3 ) ).create() ) );
        assertEquals( parse( "P18W" ), build( entry( "weeks", of( 18 ) ).create() ) );
        assertEquals( parse( "P7D" ), build( entry( "days", of( 7 ) ).create() ) );
        assertEquals( parse( "PT5H" ), build( entry( "hours", of( 5 ) ).create() ) );
        assertEquals( parse( "PT7M" ), build( entry( "minutes", of( 7 ) ).create() ) );
        assertEquals( parse( "PT2352S" ), build( entry( "seconds", of( 2352 ) ).create() ) );
        assertEquals( parse( "PT0.001S" ), build( entry( "milliseconds", of( 1 ) ).create() ) );
        assertEquals( parse( "PT0.000001S" ), build( entry( "microseconds", of( 1 ) ).create() ) );
        assertEquals( parse( "PT0.000000001S" ), build( entry( "nanoseconds", of( 1 ) ).create() ) );
        assertEquals( parse( "PT4.003002001S" ), build(
                entry( "nanoseconds", of( 1 ) )
                        .entry( "microseconds", of( 2 ) )
                        .entry( "milliseconds", of( 3 ) )
                        .entry( "seconds", of( 4 ) )
                        .create() ) );
        assertEquals( parse( "P1Y2M3W4DT5H6M7.800000009S" ), build(
                entry( "years", of( 1 ) )
                        .entry( "months", of( 2 ) )
                        .entry( "weeks", of( 3 ) )
                        .entry( "days", of( 4 ) )
                        .entry( "hours", of( 5 ) )
                        .entry( "minutes", of( 6 ) )
                        .entry( "seconds", of( 7 ) )
                        .entry( "milliseconds", of( 800 ) )
                        .entry( "microseconds", of( -900_000 ) )
                        .entry( "nanoseconds", of( 900_000_009 ) )
                        .create() ) );
    }

    @Test
    public void shouldBuildDurationBetween() throws Exception
    {
        // given
        Value a = datetime(
                date( 1984, 10, 11 ),
                localTime( 21, 30, 42, 123_000_000 ),
                ZoneId.of( "Europe/Stockholm" ) );
        Value b = datetime(
                date( 2017, 12, 23 ),
                localTime( 11, 30, 19, 456_000_000 ),
                ZoneId.of( "Europe/Stockholm" ) );

        // then
        assertEquals( parse( "P33Y2M12DT-10H-21.667S" ), build( entry( "from", a ).entry( "to", b ).create() ) );
        assertEquals( parse( "P33Y" ), build( entry( "yearsFrom", a ).entry( "to", b ).create() ) );
        assertEquals( parse( "P1732W" ), build( entry( "weeksFrom", a ).entry( "to", b ).create() ) );
        assertEquals( parse( "P12125D" ), build( entry( "daysFrom", a ).entry( "to", b ).create() ) );
        assertEquals( parse( "PT291013H" ), build( entry( "hoursFrom", a ).entry( "to", b ).create() ) );
        assertEquals( parse( "PT17460839M" ), build( entry( "minutesFrom", a ).entry( "to", b ).create() ) );
        assertEquals( parse( "PT1047650377.333S" ), build( entry( "secondsFrom", a ).entry( "to", b ).create() ) );
    }
}
