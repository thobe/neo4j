/*
 * Copyright (c) 2002-2017 "Neo Technology,"
 * Network Engine for Objects in Lund AB [http://neotechnology.com]
 *
 * This file is part of Neo4j.
 *
 * Neo4j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.neo4j.io.cursor.highlimit;

import org.neo4j.io.cursor.api.EdgeCursor;
import org.neo4j.io.cursor.api.EdgeGroupCursor;
import org.neo4j.io.cursor.api.LabelCursor;
import org.neo4j.io.cursor.api.NodeCursor;
import org.neo4j.io.cursor.api.PropertyCursor;

public class HighLimitNodeCursor extends NodeCursor
{
    @Override
    public void labels( LabelCursor labels )
    {
        throw new UnsupportedOperationException( "not implemented" );
    }

    @Override
    public void edges( EdgeGroupCursor groups )
    {
        throw new UnsupportedOperationException( "not implemented" );
    }

    @Override
    public void edges( EdgeCursor edges )
    {
        throw new UnsupportedOperationException( "not implemented" );
    }

    @Override
    public void properties( PropertyCursor cursor )
    {
        throw new UnsupportedOperationException( "not implemented" );
    }
}
