/**
 * Copyright (c) 2002-2013 "Neo Technology,"
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
package org.neo4j.kernel.impl.api.index;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.schema.IndexDefinition;
import org.neo4j.kernel.GraphDatabaseAPI;
import org.neo4j.kernel.ThreadToStatementContextBridge;
import org.neo4j.kernel.api.LabelNotFoundKernelException;
import org.neo4j.kernel.api.PropertyKeyNotFoundException;
import org.neo4j.kernel.api.index.IndexAccessor;
import org.neo4j.kernel.api.index.IndexPopulator;
import org.neo4j.kernel.api.index.NodePropertyUpdate;
import org.neo4j.kernel.api.index.SchemaIndexProvider;
import org.neo4j.kernel.extension.KernelExtensionFactory;
import org.neo4j.test.EphemeralFileSystemRule;
import org.neo4j.test.TestGraphDatabaseFactory;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.neo4j.graphdb.DynamicLabel.label;
import static org.neo4j.helpers.collection.IteratorUtil.asCollection;
import static org.neo4j.helpers.collection.IteratorUtil.asSet;
import static org.neo4j.helpers.collection.MapUtil.map;
import static org.neo4j.kernel.impl.api.index.SchemaIndexTestHelper.singleInstanceSchemaIndexProviderFactory;

public class IndexCRUDIT
{
    @Test
    public void addingANodeWithPropertyShouldGetIndexed() throws Exception
    {
        // Given
        String indexProperty = "indexProperty";
        GatheringIndexWriter writer = newWriter( indexProperty );
        createIndex( myLabel, indexProperty );

        // When
        int value1 = 12;
        String otherProperty = "otherProperty";
        int otherValue = 17;
        Node node = createNode( map( indexProperty, value1, otherProperty, otherValue ), myLabel );

        // Then, for now, this should trigger two NodePropertyUpdates
        Transaction tx = db.beginTx();
        try
        {
            long propertyKey1 = ctxProvider.getStatementContext().getPropertyKeyId( indexProperty );
            long[] labels = new long[] {ctxProvider.getStatementContext().getLabelId( myLabel.name() )};
            assertThat( writer.updates, equalTo( asSet(
                    NodePropertyUpdate.add( node.getId(), propertyKey1, value1, labels ) ) ) );

            // We get two updates because we both add a label and a property to be indexed
            // in the same transaction, in the future, we should optimize this down to
            // one NodePropertyUpdate.

            tx.success();
        }
        finally
        {
            tx.finish();
        }
    }

    @Test
    public void addingALabelToPreExistingNodeShouldGetIndexed() throws Exception
    {
        // GIVEN
        String indexProperty = "indexProperty";
        GatheringIndexWriter writer = newWriter( indexProperty );
        createIndex( myLabel, indexProperty );

        // WHEN
        String otherProperty = "otherProperty";
        int value = 12;
        int otherValue = 17;
        Node node = createNode( map( indexProperty, value, otherProperty, otherValue ) );

        // THEN
        assertThat( writer.updates.size(), equalTo( 0 ) );

        // AND WHEN
        Transaction tx = db.beginTx();
        node.addLabel( myLabel );
        tx.success();
        tx.finish();

        // THEN
        tx = db.beginTx();
        long propertyKey1 = ctxProvider.getStatementContext().getPropertyKeyId( indexProperty );
        long[] labels = new long[] {ctxProvider.getStatementContext().getLabelId( myLabel.name() )};
        assertThat( writer.updates, equalTo( asSet(
                NodePropertyUpdate.add( node.getId(), propertyKey1, value, labels ) ) ) );
        tx.finish();
    }

    private GraphDatabaseAPI db;
    private TestGraphDatabaseFactory factory;
    @Rule public EphemeralFileSystemRule fs = new EphemeralFileSystemRule();
    private final SchemaIndexProvider mockedIndexProvider = mock( SchemaIndexProvider.class );
    private final KernelExtensionFactory<?> mockedIndexProviderFactory =
            singleInstanceSchemaIndexProviderFactory( "none", mockedIndexProvider );
    private ThreadToStatementContextBridge ctxProvider;
    private final Label myLabel = label( "MYLABEL" );
    
    private Node createNode( Map<String, Object> properties, Label ... labels )
    {
        Transaction tx = db.beginTx();
        Node node = db.createNode( labels );
        for ( Map.Entry<String, Object> prop : properties.entrySet() )
        {
            node.setProperty( prop.getKey(), prop.getValue() );
        }
        tx.success();
        tx.finish();
        return node;
    }

    private void createIndex( Label myLabel, String indexProperty )
    {
        Transaction tx = db.beginTx();
        IndexDefinition index = db.schema().indexCreator( myLabel ).on( indexProperty ).create();
        tx.success();
        tx.finish();
        tx = db.beginTx();
        db.schema().awaitIndexOnline( index, 10, TimeUnit.SECONDS );
        tx.finish();
    }

    @Before
    public void before() throws Exception
    {
        factory = new TestGraphDatabaseFactory();
        factory.setFileSystem( fs.get() );
        factory.setKernelExtensions( Arrays.<KernelExtensionFactory<?>>asList( mockedIndexProviderFactory ) );
        db = (GraphDatabaseAPI) factory.newImpermanentDatabase();
        ctxProvider = db.getDependencyResolver().resolveDependency( ThreadToStatementContextBridge.class );
    }
    
    private GatheringIndexWriter newWriter( String propertyKey )
    {
        GatheringIndexWriter writer = new GatheringIndexWriter( propertyKey );
        when( mockedIndexProvider.getPopulator( anyLong() ) ).thenReturn( writer );
        when( mockedIndexProvider.getOnlineAccessor( anyLong() ) ).thenReturn( writer );
        return writer;
    }

    @After
    public void after() throws Exception
    {
        db.shutdown();
    }
    
    private class GatheringIndexWriter extends IndexAccessor.Adapter implements IndexPopulator
    {
        private final Set<NodePropertyUpdate> updates = new HashSet<NodePropertyUpdate>();
        private final String propertyKey;
        
        public GatheringIndexWriter( String propertyKey )
        {
            this.propertyKey = propertyKey;
        }

        @Override
        public void create()
        {
        }

        @Override
        public void add( long nodeId, Object propertyValue )
        {
            try
            {
                updates.add( NodePropertyUpdate.add( nodeId, ctxProvider.getStatementContext()
                                                                        .getPropertyKeyId( propertyKey ),
                        propertyValue, new long[] {ctxProvider.getStatementContext().getLabelId( myLabel.name() )} ) );
            }
            catch ( PropertyKeyNotFoundException e )
            {
                throw new RuntimeException( e );
            }
            catch ( LabelNotFoundKernelException e )
            {
                throw new RuntimeException( e );
            }
        }

        @Override
        public void update( Iterable<NodePropertyUpdate> updates )
        {
            this.updates.addAll( asCollection( updates ) );
        }
        
        @Override
        public void updateAndCommit( Iterable<NodePropertyUpdate> updates )
        {
            this.updates.addAll( asCollection( updates ) );
        }

        @Override
        public void close( boolean populationCompletedSuccessfully )
        {
        }
    }
}
