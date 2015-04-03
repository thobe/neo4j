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

package org.neo4j.kernel.api;

import org.neo4j.graphdb.Direction;

public interface RelationshipSelector
{
    /**
     * Select which direction to traverse relationships of a given type.
     *
     * @param type     the type of relationship to determine traversal direction of.
     * @param outgoing the number of outgoing relationships of the given type (excluding loops).
     * @param incoming the number of incoming relationships of the given type (excluding loops).
     * @param loops    the number of loop relationships of the given type.
     * @return the direction in which to traverse relationships of the given type, {@code null} to not traverse this
     * relationship type.
     */
    Direction selectRelationship( int type, int outgoing, int incoming, int loops );
}
