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
package org.neo4j.kernel.impl.api;

import java.util.Iterator;

import org.neo4j.collection.primitive.PrimitiveIntIterator;
import org.neo4j.kernel.api.EntityType;
import org.neo4j.kernel.api.NodeCursor;
import org.neo4j.kernel.api.PropertyCursor;
import org.neo4j.kernel.api.RelationshipCursor;
import org.neo4j.kernel.api.constraints.UniquenessConstraint;
import org.neo4j.kernel.api.exceptions.EntityNotFoundException;
import org.neo4j.kernel.api.exceptions.index.IndexNotFoundKernelException;
import org.neo4j.kernel.api.exceptions.schema.ConstraintValidationKernelException;
import org.neo4j.kernel.api.exceptions.schema.IndexBrokenKernelException;
import org.neo4j.kernel.api.exceptions.schema.UnableToValidateConstraintKernelException;
import org.neo4j.kernel.api.exceptions.schema.UniqueConstraintViolationKernelException;
import org.neo4j.kernel.api.index.IndexDescriptor;
import org.neo4j.kernel.api.properties.DefinedProperty;
import org.neo4j.kernel.api.properties.Property;
import org.neo4j.kernel.impl.api.operations.EntityOperations;
import org.neo4j.kernel.impl.api.operations.EntityReadOperations;
import org.neo4j.kernel.impl.api.operations.EntityWriteOperations;
import org.neo4j.kernel.impl.api.operations.SchemaReadOperations;
import org.neo4j.kernel.impl.locking.Locks;

import static org.neo4j.kernel.api.StatementConstants.NO_SUCH_NODE;
import static org.neo4j.kernel.impl.locking.ResourceTypes.INDEX_ENTRY;
import static org.neo4j.kernel.impl.locking.ResourceTypes.indexEntryResourceId;

public class ConstraintEnforcingEntityOperations implements EntityOperations
{
    private final EntityWriteOperations entityWriteOperations;
    private final EntityReadOperations entityReadOperations;
    private final SchemaReadOperations schemaReadOperations;

    public ConstraintEnforcingEntityOperations(
            EntityWriteOperations entityWriteOperations,
            EntityReadOperations entityReadOperations,
            SchemaReadOperations schemaReadOperations )
    {
        this.entityWriteOperations = entityWriteOperations;
        this.entityReadOperations = entityReadOperations;
        this.schemaReadOperations = schemaReadOperations;
    }

    @Override
    public boolean nodeAddLabel( KernelStatement state, long nodeId, int labelId )
            throws EntityNotFoundException, ConstraintValidationKernelException
    {
        Iterator<UniquenessConstraint> constraints = schemaReadOperations.constraintsGetForLabel( state, labelId );
        while ( constraints.hasNext() )
        {
            UniquenessConstraint constraint = constraints.next();
            int propertyKeyId = constraint.propertyKeyId();
            try ( NodeCursor cursor = nodeCursor( state, nodeId ); PropertyCursor properties = cursor.nodeProperties() )
            {
                if ( properties.propertyFind( propertyKeyId ) )
                {
                    validateNoExistingNodeWithLabelAndProperty( state, labelId, properties.property(), nodeId );
                }
            }
        }
        return entityWriteOperations.nodeAddLabel( state, nodeId, labelId );
    }

    private NodeCursor nodeCursor( KernelStatement state, long nodeId ) throws EntityNotFoundException
    {
        NodeCursor cursor = entityReadOperations.nodesGetById( state, nodeId );
        if ( !cursor.nodeNext() )
        {
            cursor.close();
            throw new EntityNotFoundException( EntityType.NODE, nodeId );
        }
        return cursor;
    }

    @Override
    public Property nodeSetProperty( KernelStatement state, long nodeId, DefinedProperty property )
            throws EntityNotFoundException, ConstraintValidationKernelException
    {
        try ( NodeCursor cursor = nodeCursor( state, nodeId ) )
        {
            PrimitiveIntIterator labelIds = cursor.nodeLabels();
            while ( labelIds.hasNext() )
            {
                int labelId = labelIds.next();
                int propertyKeyId = property.propertyKeyId();
                Iterator<UniquenessConstraint> constraintIterator =
                        schemaReadOperations.constraintsGetForLabelAndPropertyKey( state, labelId, propertyKeyId );
                if ( constraintIterator.hasNext() )
                {
                    validateNoExistingNodeWithLabelAndProperty( state, labelId, property, nodeId );
                }
            }
            return entityWriteOperations.nodeSetProperty( state, nodeId, property );
        }
    }

    private void validateNoExistingNodeWithLabelAndProperty( KernelStatement state, int labelId,
                                                             DefinedProperty property, long modifiedNode )
            throws ConstraintValidationKernelException
    {
        try
        {
            Object value = property.value();
            int propertyKeyId = property.propertyKeyId();
            IndexDescriptor indexDescriptor = new IndexDescriptor( labelId, propertyKeyId );
            assertIndexOnline( state, indexDescriptor );
            state.locks().acquireExclusive( INDEX_ENTRY,
                    indexEntryResourceId( labelId, propertyKeyId, property.valueAsString() ) );

            long existing = entityReadOperations.nodeGetUniqueFromIndexLookup( state, indexDescriptor, value );
            if ( existing != NO_SUCH_NODE && existing != modifiedNode )
            {
                throw new UniqueConstraintViolationKernelException( labelId, propertyKeyId, value, existing );
            }
        }
        catch ( IndexNotFoundKernelException | IndexBrokenKernelException e )
        {
            throw new UnableToValidateConstraintKernelException( e );
        }
    }

    private void assertIndexOnline( KernelStatement state, IndexDescriptor indexDescriptor )
            throws IndexNotFoundKernelException, IndexBrokenKernelException
    {
        switch ( schemaReadOperations.indexGetState( state, indexDescriptor ) )
        {
            case ONLINE:
                return;
            default:
                throw new IndexBrokenKernelException( schemaReadOperations.indexGetFailure( state, indexDescriptor ) );
        }
    }

    // Simply delegate the rest of the invocations

    @Override
    public void nodeDelete( KernelStatement state, long nodeId ) throws EntityNotFoundException
    {
        entityWriteOperations.nodeDelete( state, nodeId );
    }

    @Override
    public long relationshipCreate( KernelStatement statement, int relationshipTypeId, long startNodeId, long endNodeId )
            throws EntityNotFoundException
    {
        return entityWriteOperations.relationshipCreate( statement, relationshipTypeId, startNodeId, endNodeId );
    }

    @Override
    public void relationshipDelete( KernelStatement state, long relationshipId ) throws EntityNotFoundException
    {
        entityWriteOperations.relationshipDelete( state, relationshipId );
    }

    @Override
    public boolean nodeRemoveLabel( KernelStatement state, long nodeId, int labelId ) throws EntityNotFoundException
    {
        return entityWriteOperations.nodeRemoveLabel( state, nodeId, labelId );
    }

    @Override
    public Property relationshipSetProperty( KernelStatement state, long relationshipId, DefinedProperty property )
            throws EntityNotFoundException
    {
        return entityWriteOperations.relationshipSetProperty( state, relationshipId, property );
    }

    @Override
    public Property graphSetProperty( KernelStatement state, DefinedProperty property )
    {
        return entityWriteOperations.graphSetProperty( state, property );
    }

    @Override
    public Property nodeRemoveProperty( KernelStatement state, long nodeId, int propertyKeyId )
            throws EntityNotFoundException
    {
        return entityWriteOperations.nodeRemoveProperty( state, nodeId, propertyKeyId );
    }

    @Override
    public Property relationshipRemoveProperty( KernelStatement state, long relationshipId, int propertyKeyId )
            throws EntityNotFoundException
    {
        return entityWriteOperations.relationshipRemoveProperty( state, relationshipId, propertyKeyId );
    }

    @Override
    public Property graphRemoveProperty( KernelStatement state, int propertyKeyId )
    {
        return entityWriteOperations.graphRemoveProperty( state, propertyKeyId );
    }

    @Override
    public NodeCursor nodesGetForLabel( KernelStatement state, int labelId )
    {
        return entityReadOperations.nodesGetForLabel( state, labelId );
    }

    @Override
    public NodeCursor nodesGetById( KernelStatement state, long nodeId )
    {
        return entityReadOperations.nodesGetById( state, nodeId );
    }

    @Override
    public NodeCursor nodesGetFromIndexLookup( KernelStatement state, IndexDescriptor index, Object value )
            throws IndexNotFoundKernelException
    {
        return entityReadOperations.nodesGetFromIndexLookup( state, index, value );
    }

    @Override
    public long nodeGetUniqueFromIndexLookup(
            KernelStatement state,
            IndexDescriptor index,
            Object value )
            throws IndexNotFoundKernelException, IndexBrokenKernelException
    {
        assertIndexOnline( state, index );

        int labelId = index.getLabelId();
        int propertyKeyId = index.getPropertyKeyId();
        String stringVal = "";
        if ( null != value )
        {
            DefinedProperty property = Property.property( propertyKeyId, value );
            stringVal = property.valueAsString();
        }

        // If we find the node - hold a shared lock. If we don't find a node - hold an exclusive lock.
        Locks.Client locks = state.locks();
        long indexEntryId = indexEntryResourceId( labelId, propertyKeyId, stringVal );

        locks.acquireShared( INDEX_ENTRY, indexEntryId );

        long nodeId = entityReadOperations.nodeGetUniqueFromIndexLookup( state, index, value );
        if ( NO_SUCH_NODE == nodeId )
        {
            locks.releaseShared( INDEX_ENTRY, indexEntryId );
            locks.acquireExclusive( INDEX_ENTRY, indexEntryId );

            nodeId = entityReadOperations.nodeGetUniqueFromIndexLookup( state, index, value );
            if ( NO_SUCH_NODE != nodeId ) // we found it under the exclusive lock
            {
                // downgrade to a shared lock
                locks.acquireShared( INDEX_ENTRY, indexEntryId );
                locks.releaseExclusive( INDEX_ENTRY, indexEntryId );
            }
        }
        return nodeId;
    }

    @Override
    public long nodeCreate( KernelStatement statement )
    {
        return entityWriteOperations.nodeCreate( statement );
    }

    @Override
    public NodeCursor nodesGetAll( KernelStatement state )
    {
        return entityReadOperations.nodesGetAll( state );
    }

    @Override
    public RelationshipCursor relationshipsGetAll( KernelStatement state )
    {
        return entityReadOperations.relationshipsGetAll( state );
    }

    @Override
    public RelationshipCursor relationshipsGetById( KernelStatement state, long relationshipId )
    {
        return entityReadOperations.relationshipsGetById( state, relationshipId );
    }

    @Override
    public PropertyCursor graphProperties( KernelStatement state )
    {
        return entityReadOperations.graphProperties( state );
    }
}
