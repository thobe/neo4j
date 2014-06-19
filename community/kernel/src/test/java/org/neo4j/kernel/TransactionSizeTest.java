package org.neo4j.kernel;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;
import org.neo4j.test.TestGraphDatabaseFactory;

import static org.junit.Assert.assertEquals;

import static org.neo4j.graphdb.DynamicLabel.label;
import static org.neo4j.graphdb.DynamicRelationshipType.withName;

public class TransactionSizeTest
{
    @Test
    public void shouldGetSizeOfEmptyTransaction() throws Exception
    {
        // given
        try ( Transaction tx = graphdb.beginTx() )
        {
            // then
            assertEquals( 0, tx.changedNodeCount() );
            assertEquals( 0, tx.changedRelationshipCount() );
        }
    }

    @Test
    public void shouldCountAddedNodes() throws Exception
    {
        try ( Transaction tx = graphdb.beginTx() )
        {
            // given
            int nodes = 5;

            // when
            for ( int n = 0; n < nodes; n++ )
            {
                graphdb.createNode();
            }

            // then
            assertEquals( nodes, tx.changedNodeCount() );
        }
    }

    @Test
    public void shouldCountRemovedNodes() throws Exception
    {
        // given
        Node[] nodes = new Node[5];
        try ( Transaction tx = graphdb.beginTx() )
        {
            for ( int n = 0; n < nodes.length; n++ )
            {
                nodes[n] = graphdb.createNode();
            }
            tx.success();
        }

        // when
        try ( Transaction tx = graphdb.beginTx() )
        {
            for ( Node node : nodes )
            {
                node.delete();
            }
            // then
            assertEquals( nodes.length, tx.changedNodeCount() );
        }
    }

    @Test
    public void shouldNotCountNodesThatHaveBeenAddedThenRemoved() throws Exception
    {
        // given
        try ( Transaction tx = graphdb.beginTx() )
        {
            // when
            graphdb.createNode().delete();

            // then
            assertEquals( 0, tx.changedNodeCount() );
        }
    }

    @Test
    public void shouldCountNodesWithLabelChanges() throws Exception
    {
        // given
        Node node;
        try ( Transaction tx = graphdb.beginTx() )
        {
            node = graphdb.createNode();
            tx.success();
        }

        // when
        try ( Transaction tx = graphdb.beginTx() )
        {
            node.addLabel( label( "Foo" ) );

            // then
            assertEquals( 1, tx.changedNodeCount() );
            tx.success();
        }

        // when
        try ( Transaction tx = graphdb.beginTx() )
        {
            node.removeLabel( label( "Foo" ) );

            // then
            assertEquals( 1, tx.changedNodeCount() );
            tx.success();
        }

        // when
        try ( Transaction tx = graphdb.beginTx() )
        {
            node.removeLabel( label( "Foo" ) );

            // then
            assertEquals( 0, tx.changedNodeCount() );
            tx.success();
        }
    }

    @Test
    public void shouldCountNodesWithPropertyChanges() throws Exception
    {
        // given
        Node node;
        try ( Transaction tx = graphdb.beginTx() )
        {
            node = graphdb.createNode();
            tx.success();
        }

        // when
        try ( Transaction tx = graphdb.beginTx() )
        {
            node.setProperty( "foo", "bar" );

            // then
            assertEquals( 1, tx.changedNodeCount() );
            tx.success();
        }

        // when
        try ( Transaction tx = graphdb.beginTx() )
        {
            node.removeProperty( "foo" );

            // then
            assertEquals( 1, tx.changedNodeCount() );
            tx.success();
        }

        // when
        try ( Transaction tx = graphdb.beginTx() )
        {
            node.removeProperty( "foo" );

            // then
            assertEquals( 0, tx.changedNodeCount() );
            tx.success();
        }
    }

    @Test
    public void shouldCountNodesWithPropertyAndLabelChanges() throws Exception
    {
        // given
        Node node;
        try ( Transaction tx = graphdb.beginTx() )
        {
            node = graphdb.createNode();
            tx.success();
        }

        // when
        try ( Transaction tx = graphdb.beginTx() )
        {
            node.addLabel( label( "Foo" ) );
            node.setProperty( "bar", "baz" );

            // then
            assertEquals( 1, tx.changedNodeCount() );
        }
    }

    @Test
    public void shouldOnlyCountAddedNodesWithPropertyOrLabelChangesOnce() throws Exception
    {
        // when
        try ( Transaction tx = graphdb.beginTx() )
        {
            Node node = graphdb.createNode();
            node.addLabel( label( "Foo" ) );
            node.setProperty( "bar", "baz" );

            // then
            assertEquals( 1, tx.changedNodeCount() );
        }
    }

    @Test
    public void shouldOnlyCountDeletedNodesWithPropertyOrLabelChangesOnce() throws Exception
    {
        // given
        Node node;
        try ( Transaction tx = graphdb.beginTx() )
        {
            node = graphdb.createNode();
            tx.success();
        }

        // when
        try ( Transaction tx = graphdb.beginTx() )
        {
            node.addLabel( label( "Foo" ) );
            node.setProperty( "bar", "baz" );
            node.delete();

            // then
            assertEquals( 1, tx.changedNodeCount() );
        }
    }

    @Test
    public void shouldCountCreatedRelationships() throws Exception
    {
        // when
        try ( Transaction tx = graphdb.beginTx() )
        {
            graphdb.createNode().createRelationshipTo( graphdb.createNode(), withName( "FOO" ) );

            // then
            assertEquals( 1, tx.changedRelationshipCount() );
        }
    }

    @Test
    public void shouldCountDeletedRelationships() throws Exception
    {
        // given
        Relationship rel;
        try ( Transaction tx = graphdb.beginTx() )
        {
            rel = graphdb.createNode().createRelationshipTo( graphdb.createNode(), withName( "FOO" ) );
            tx.success();
        }
        // when
        try ( Transaction tx = graphdb.beginTx() )
        {
            rel.delete();

            // then
            assertEquals( 1, tx.changedRelationshipCount() );
        }
    }

    @Test
    public void shouldCountRelationshipWithPropertyChanges() throws Exception
    {
        // given
        Relationship rel;
        try ( Transaction tx = graphdb.beginTx() )
        {
            rel = graphdb.createNode().createRelationshipTo( graphdb.createNode(), withName( "FOO" ) );
            tx.success();
        }

        // when
        try ( Transaction tx = graphdb.beginTx() )
        {
            rel.setProperty( "foo", "bar" );

            // then
            assertEquals( 1, tx.changedRelationshipCount() );
            tx.success();
        }

        // when
        try ( Transaction tx = graphdb.beginTx() )
        {
            rel.removeProperty( "foo" );

            // then
            assertEquals( 1, tx.changedRelationshipCount() );
            tx.success();
        }

        // when
        try ( Transaction tx = graphdb.beginTx() )
        {
            rel.removeProperty( "foo" );

            // then
            assertEquals( 0, tx.changedRelationshipCount() );
            tx.success();
        }
    }

    @Test
    public void shouldOnlyCountAddedRelationshipWithPropertyChangesOnce() throws Exception
    {
        // when
        try ( Transaction tx = graphdb.beginTx() )
        {
            Relationship rel = graphdb.createNode().createRelationshipTo( graphdb.createNode(), withName( "FOO" ) );
            rel.setProperty( "bar", "baz" );

            // then
            assertEquals( 1, tx.changedRelationshipCount() );
        }
    }

    @Test
    public void shouldOnlyCountDeletedRelationshipWithPropertyChangesOnce() throws Exception
    {
        // given
        Relationship rel;
        try ( Transaction tx = graphdb.beginTx() )
        {
            rel = graphdb.createNode().createRelationshipTo( graphdb.createNode(), withName( "FOO" ) );
            tx.success();
        }

        // when
        try ( Transaction tx = graphdb.beginTx() )
        {
            rel.setProperty( "bar", "baz" );
            rel.delete();

            // then
            assertEquals( 1, tx.changedRelationshipCount() );
        }
    }

    private GraphDatabaseService graphdb;

    @Before
    public void start()
    {
        graphdb = new TestGraphDatabaseFactory().newImpermanentDatabase();
    }

    @After
    public void shutdown()
    {
        graphdb.shutdown();
    }
}