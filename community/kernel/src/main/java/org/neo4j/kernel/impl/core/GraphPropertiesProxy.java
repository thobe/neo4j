/**
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

import org.neo4j.graphdb.ConstraintViolationException;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.kernel.api.PropertyCursor;
import org.neo4j.kernel.api.ReadOperations;
import org.neo4j.kernel.api.Statement;
import org.neo4j.kernel.api.exceptions.InvalidTransactionTypeKernelException;
import org.neo4j.kernel.api.exceptions.schema.IllegalTokenNameException;
import org.neo4j.kernel.api.properties.Property;

import static java.lang.String.format;

public class GraphPropertiesProxy extends PropertyContainerProxy implements GraphProperties
{
    private final GraphPropertiesActions actions;

    public interface GraphPropertiesActions
    {
        Statement statement();

        GraphDatabaseService getGraphDatabaseService();

        void failTransaction();

        void assertInUnterminatedTransaction();
    }

    public GraphPropertiesProxy( GraphPropertiesActions actions )
    {
        this.actions = actions;
    }

    @Override
    public GraphDatabaseService getGraphDatabase()
    {
        return actions.getGraphDatabaseService();
    }


    @Override
    public void setProperty( String key, Object value )
    {
        try ( Statement statement = actions.statement() )
        {
            int propertyKeyId = statement.tokenWriteOperations().propertyKeyGetOrCreateForName( key );
            try
            {
                statement.dataWriteOperations().graphSetProperty( Property.property( propertyKeyId, value ) );
            }
            catch ( IllegalArgumentException e )
            {
                // Trying to set an illegal value is a critical error - fail this transaction
                actions.failTransaction();
                throw e;
            }
        }
        catch ( IllegalTokenNameException e )
        {
            throw new IllegalArgumentException( format( "Invalid property key '%s'.", key ), e );
        }
        catch ( InvalidTransactionTypeKernelException e )
        {
            throw new ConstraintViolationException( e.getMessage(), e );
        }
    }

    @Override
    public Object removeProperty( String key )
    {
        try ( Statement statement = actions.statement() )
        {
            int propertyKeyId = statement.tokenWriteOperations().propertyKeyGetOrCreateForName( key );
            return statement.dataWriteOperations().graphRemoveProperty( propertyKeyId ).value( null );
        }
        catch ( IllegalTokenNameException e )
        {
            throw new IllegalArgumentException( format( "Invalid property key '%s'.", key ), e );
        }
        catch ( InvalidTransactionTypeKernelException e )
        {
            throw new ConstraintViolationException( e.getMessage(), e );
        }
    }


    @Override
    public boolean equals( Object o )
    {
        // Yeah, this is breaking transitive equals, but should be OK anyway.
        // Also, we're checking == (not .equals) on GDS since that seems to be what the tests are asserting
        return o instanceof GraphPropertiesProxy &&
                actions.getGraphDatabaseService() == ((GraphPropertiesProxy)o).actions.getGraphDatabaseService();
    }

    @Override
    public int hashCode()
    {
        return actions.getGraphDatabaseService().hashCode();
    }

    @Override
    Statement statement()
    {
        return actions.statement();
    }

    @Override
    PropertyCursor propertyCursor( ReadOperations read )
    {
        return read.graphProperties();
    }
}
