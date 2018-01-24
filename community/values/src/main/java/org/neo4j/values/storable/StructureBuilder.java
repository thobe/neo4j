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

import java.util.Map;

import org.neo4j.values.AnyValue;

public abstract class StructureBuilder<Input, Result>
{
    public abstract Result build( Input single );

    public final Result build( Map<String,? extends Input> input )
    {
        StructureBuilder<Input,Result> builder = this;
        for ( Map.Entry<String,? extends Input> entry : input.entrySet() )
        {
            builder = builder.add( entry.getKey(), entry.getValue() );
        }
        return builder.build();
    }

    public abstract StructureBuilder<Input,Result> add( String field, Input value );

    public abstract Result build();

    StructureBuilder()
    {
        // Particular subclasses defined in this package
    }

    protected static String unpackString( String name, AnyValue value )
    {
        if ( value == null )
        {
            return null;
        }
        if ( value instanceof TextValue )
        {
            return ((TextValue) value).stringValue();
        }
        else
        {
            throw new IllegalArgumentException(
                    name + " must be an string value, but was a " + value.getClass().getSimpleName() );
        }
    }

    static long unpackInteger( String name, AnyValue value )
    {
        if ( value == null )
        {
            return 0;
        }
        if ( value instanceof IntegralValue )
        {
            return ((IntegralValue) value).longValue();
        }
        throw new IllegalArgumentException(
                name + " must be an integer value, but was a " + value.getClass().getSimpleName() );
    }
}
