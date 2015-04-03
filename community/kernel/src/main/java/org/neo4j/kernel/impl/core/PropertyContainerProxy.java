/*
 * Copyright (c) 2002-2015 "Neo Technology,"
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

package org.neo4j.kernel.impl.core;

import java.util.ArrayList;
import java.util.List;

import org.neo4j.graphdb.NotFoundException;
import org.neo4j.graphdb.PropertyContainer;
import org.neo4j.helpers.ThisShouldNotHappenError;
import org.neo4j.kernel.api.PropertyCursor;
import org.neo4j.kernel.api.ReadOperations;
import org.neo4j.kernel.api.Statement;
import org.neo4j.kernel.api.exceptions.PropertyKeyIdNotFoundKernelException;

import static java.lang.String.format;

abstract class PropertyContainerProxy implements PropertyContainer
{
    @Override
    public final boolean hasProperty( String key )
    {
        if ( null == key )
        {
            return false;
        }

        try ( Statement statement = statement();
              PropertyCursor properties = propertyCursor( statement.readOperations() )  )
        {
            return properties.propertyFind( statement.readOperations().propertyKeyGetForName( key ) );
        }
    }

    @Override
    public final Object getProperty( String key )
    {
        if ( null == key )
        {
            throw new IllegalArgumentException( "(null) property key is not allowed" );
        }

        try ( Statement statement = statement();
              PropertyCursor properties = propertyCursor( statement.readOperations() ) )
        {
            if ( properties.propertyFind( statement.readOperations().propertyKeyGetForName( key ) ) )
            {
                return properties.propertyValue();
            }
            else
            {
                throw new NotFoundException( format( "No such property, '%s'.", key ) );
            }
        }
    }

    @Override
    public final Object getProperty( String key, Object defaultValue )
    {
        if ( null == key )
        {
            throw new IllegalArgumentException( "(null) property key is not allowed" );
        }

        try ( Statement statement = statement();
              PropertyCursor properties = propertyCursor( statement.readOperations() ) )
        {
            if ( properties.propertyFind( statement.readOperations().propertyKeyGetForName( key ) ) )
            {
                return properties.propertyValue();
            }
            else
            {
                return defaultValue;
            }
        }
    }

    @Override
    public final Iterable<String> getPropertyKeys()
    {
        try ( Statement statement = statement() )
        {
            List<String> keys = new ArrayList<>();
            try ( PropertyCursor properties = propertyCursor( statement.readOperations() ) )
            {
                while ( properties.propertyNext() )
                {
                    keys.add( statement.readOperations().propertyKeyGetName( properties.propertyKeyId() ) );
                }
            }
            return keys;
        }
        catch ( PropertyKeyIdNotFoundKernelException e )
        {
            throw new ThisShouldNotHappenError( "Jake",
                                                "Property key retrieved through kernel API should exist.", e );
        }
    }

    abstract Statement statement();

    abstract PropertyCursor propertyCursor( ReadOperations read );
}
