/*
 * Copyright (c) 2002-2016 "Neo Technology,"
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
package org.neo4j.kernel.impl.api.store;

import org.neo4j.graphdb.Resource;
import org.neo4j.kernel.impl.store.RecordCursor;
import org.neo4j.kernel.impl.store.record.Record;
import org.neo4j.kernel.impl.store.record.RecordLoad;
import org.neo4j.kernel.impl.store.record.RelationshipRecord;
import org.neo4j.storageengine.api.Direction;

// TODO: this API needs a way to proceed to the "next" relationship chain for when a (dense) node has many chains
// TODO: this API needs a way to handle transaction state
// TODO: along with the handling of multiple chains we also need a way to select which types and directions to traverse
public class RelationshipCursor implements Resource
{
    private final RelationshipRecord record = new RelationshipRecord( -1 );
    private RecordCursor<RelationshipRecord> cursor;

    public final boolean matching( long node, Direction direction, int type )
    {
        if ( record.getType() != type )
        {
            return false;
        }
        switch ( direction )
        {
        case OUTGOING:
            return record.getFirstNode() == node;
        case INCOMING:
            return record.getSecondNode() == node;
        default:
            return record.getFirstNode() == node || record.getSecondNode() == node;
        }
    }

    // position
    public final boolean available()
    {
        return record.getId() != Record.NO_NEXT_RELATIONSHIP.intValue();
    }

    public final long id()
    {
        return record.getId();
    }

    // field access
    public final int type()
    {
        return record.getType();
    }

    public final long source()
    {
        return record.getFirstNode();
    }

    public final long target()
    {
        return record.getSecondNode();
    }

    public final long sourceNext()
    {
        return record.getFirstNextRel();
    }

    public final long sourcePrev()
    {
        return record.isFirstInFirstChain() ? Record.NO_NEXT_RELATIONSHIP.intValue() : record.getFirstPrevRel();
    }

    public final long targetNext()
    {
        return record.getSecondNextRel();
    }

    public final long targetPrev()
    {
        return record.isFirstInSecondChain() ? Record.NO_NEXT_RELATIONSHIP.intValue() : record.getSecondPrevRel();
    }

    // cursor progression
    public final boolean nextSource()
    {
        return jump( sourceNext() );
    }

    public final boolean prevSource()
    {
        return jump( sourcePrev() );
    }

    public final boolean nextTarget()
    {
        return jump( targetNext() );
    }

    public final boolean prevTarget()
    {
        return jump( targetPrev() );
    }

    public final boolean jump( long relationshipId )
    {
        return cursor.next( relationshipId, record, RecordLoad.NORMAL );
    }

    @Override
    public final void close()
    {
        this.record.clear();
        this.record.setId( -1 );
        if ( cursor != null )
        {
//            this.cursor.close();
            this.cursor = null;
        }
    }

    public int init( RecordCursor<RelationshipRecord> cursor, long id, long node, Direction direction, int type )
    {
        close(); // ensure this cursor does not have any previous state
        this.cursor = cursor;
        if ( cursor.next( id, record, RecordLoad.NORMAL ) )
        {
            int degree;
            if ( record.getFirstNode() == node )
            {
                assert record.isFirstInFirstChain();
                degree = (int) record.getFirstPrevRel();
            }
            else if ( record.getSecondNode() == node )
            {
                assert record.isFirstInSecondChain();
                degree = (int) record.getSecondPrevRel();
            }
            else
            {
                throw new IllegalStateException( String
                        .format( "Relationship [%d] not in chain for node [%d]", id, node ) );
            }
            while ( !matching( node, direction, type ) )
            {
                degree--;
                long next;
                if ( record.getFirstNode() == node )
                {
                    next = record.getFirstNextRel();
                }
                else if ( record.getSecondNode() == node )
                {
                    next = record.getSecondNextRel();
                }
                else
                {
                    throw new IllegalStateException( String
                            .format( "Relationship [%d] not in chain for node [%d]", id, node ) );
                }
                if ( !cursor.next( next, record, RecordLoad.NORMAL ) )
                {
                    return 0;
                }
            }
            return degree;
        }
        close();
        return 0;
    }
}
