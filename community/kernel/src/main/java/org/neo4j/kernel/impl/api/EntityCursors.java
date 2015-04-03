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

package org.neo4j.kernel.impl.api;

import org.neo4j.collection.primitive.PrimitiveIntIterator;
import org.neo4j.function.Predicate;
import org.neo4j.graphdb.Direction;
import org.neo4j.kernel.api.NodeCursor;
import org.neo4j.kernel.api.PropertyCursor;
import org.neo4j.kernel.api.RelationshipCursor;
import org.neo4j.kernel.api.RelationshipSelector;

public class EntityCursors
{
    public static final NodeCursor NO_NODES = new NodeCursor()
    {
        @Override
        public void addFilter( Predicate<NodeCursor> filter )
        {
            throw new UnsupportedOperationException( "not implemented" );
        }

        @Override
        public boolean nodeNext()
        {
            throw new UnsupportedOperationException( "not implemented" );
        }

        @Override
        public long nodeId()
        {
            throw new UnsupportedOperationException( "not implemented" );
        }

        @Override
        public boolean nodeHasLabel( int labelId )
        {
            throw new UnsupportedOperationException( "not implemented" );
        }

        @Override
        public PrimitiveIntIterator nodeLabels()
        {
            throw new UnsupportedOperationException( "not implemented" );
        }

        @Override
        public PropertyCursor nodeProperties()
        {
            throw new UnsupportedOperationException( "not implemented" );
        }

        @Override
        public int nodeDegree( Direction direction )
        {
            throw new UnsupportedOperationException( "not implemented" );
        }

        @Override
        public int nodeDegree( int type, Direction direction )
        {
            throw new UnsupportedOperationException( "not implemented" );
        }

        @Override
        public RelationshipCursor nodeRelationships( Direction direction )
        {
            throw new UnsupportedOperationException( "not implemented" );
        }

        @Override
        public RelationshipCursor nodeRelationships( Direction direction, int[] types )
        {
            throw new UnsupportedOperationException( "not implemented" );
        }

        @Override
        public RelationshipCursor nodeRelationships( RelationshipSelector selector )
        {
            throw new UnsupportedOperationException( "not implemented" );
        }

        @Override
        public void close()
        {
            throw new UnsupportedOperationException( "not implemented" );
        }
    };
}
