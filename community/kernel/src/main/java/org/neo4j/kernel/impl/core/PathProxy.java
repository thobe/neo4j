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
package org.neo4j.kernel.impl.core;

import java.util.Arrays;
import java.util.Iterator;

import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.PropertyContainer;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.traversal.Paths;

import static org.neo4j.helpers.collection.Iterators.iteratorsEqual;

public class PathProxy implements Path
{
    private final EmbeddedProxySPI proxySPI;
    private final long[] nodes;
    private final long[] relationships;

    public PathProxy( EmbeddedProxySPI proxySPI, long[] nodes, long[] relationships )
    {
        assert nodes.length == relationships.length + 1;
        this.proxySPI = proxySPI;
        this.nodes = nodes;
        this.relationships = relationships;
    }

    @Override
    public String toString()
    {
        return Paths.defaultPathToString( this );
    }

    @Override
    public int hashCode()
    {
        if ( relationships.length == 0 )
        {
            return Long.hashCode( nodes[0] );
        }
        else
        {
            return Arrays.hashCode( relationships );
        }
    }

    @Override
    public boolean equals( Object obj )
    {
        if ( this == obj )
        {
            return true;
        }
        else if ( obj instanceof Path )
        {
            Path other = (Path) obj;
            if ( nodes[0] != other.startNode().getId() )
            {
                return false;
            }
            return iteratorsEqual( this.relationships().iterator(), other.relationships().iterator() );
        }
        else
        {
            return false;
        }
    }

    @Override
    public Node startNode()
    {
        return new NodeProxy( proxySPI, nodes[0] );
    }

    @Override
    public Node endNode()
    {
        return new NodeProxy( proxySPI, nodes[nodes.length - 1] );
    }

    @Override
    public Relationship lastRelationship()
    {
        return relationships.length == 0
                ? null
                : new RelationshipProxy( proxySPI, relationships[relationships.length - 1] );
    }

    @Override
    public Iterable<Relationship> relationships()
    {
        return () -> new Iterator<Relationship>()
        {
            int i;

            @Override
            public boolean hasNext()
            {
                return i < relationships.length;
            }

            @Override
            public Relationship next()
            {
                return new RelationshipProxy( proxySPI, relationships[i++] );
            }
        };
    }

    @Override
    public Iterable<Relationship> reverseRelationships()
    {
        return () -> new Iterator<Relationship>()
        {
            int i = relationships.length;

            @Override
            public boolean hasNext()
            {
                return i > 0;
            }

            @Override
            public Relationship next()
            {
                return new RelationshipProxy( proxySPI, relationships[--i] );
            }
        };
    }

    @Override
    public Iterable<Node> nodes()
    {
        return () -> new Iterator<Node>()
        {
            int i;

            @Override
            public boolean hasNext()
            {
                return i < nodes.length;
            }

            @Override
            public Node next()
            {
                return new NodeProxy( proxySPI, nodes[i++] );
            }
        };
    }

    @Override
    public Iterable<Node> reverseNodes()
    {
        return () -> new Iterator<Node>()
        {
            int i = nodes.length;

            @Override
            public boolean hasNext()
            {
                return i > 0;
            }

            @Override
            public Node next()
            {
                return new NodeProxy( proxySPI, nodes[--i] );
            }
        };
    }

    @Override
    public int length()
    {
        return relationships.length;
    }

    @Override
    public Iterator<PropertyContainer> iterator()
    {
        return new Iterator<PropertyContainer>()
        {
            int i;
            boolean relationship;

            @Override
            public boolean hasNext()
            {
                return i < relationships.length || !relationship;
            }

            @Override
            public PropertyContainer next()
            {
                if ( relationship )
                {
                    relationship = false;
                    return new RelationshipProxy( proxySPI, relationships[i++] );
                }
                else
                {
                    relationship = true;
                    return new NodeProxy( proxySPI, nodes[i] );
                }
            }
        };
    }
}
