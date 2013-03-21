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

import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.neo4j.graphdb.Transaction;
import org.neo4j.kernel.GraphDatabaseAPI;
import org.neo4j.kernel.ThreadToStatementContextBridge;
import org.neo4j.kernel.api.StatementContext;
import org.neo4j.kernel.api.index.IndexNotFoundKernelException;
import org.neo4j.kernel.api.index.InternalIndexState;
import org.neo4j.kernel.impl.nioneo.store.IndexRule;
import org.neo4j.test.ImpermanentGraphDatabase;

import static org.junit.Assert.assertEquals;
import static org.neo4j.helpers.collection.IteratorUtil.asSet;

public class IndexIT
{

    @Test
    public void createANewIndex() throws Exception
    {
        // GIVEN
        Transaction tx = db.beginTx();
        StatementContext statement = ctxProvider.getStatementContext();
        long labelId = 5, propertyKey = 8;

        // WHEN
        IndexRule rule = statement.addIndexRule( labelId, propertyKey );
        tx.success();
        tx.finish();

        // AND WHEN the index is created
        awaitIndexOnline( rule );

        // THEN
    }

    @Test
    public void addIndexRuleInATransaction() throws Exception
    {
        // GIVEN
        Transaction tx = db.beginTx();
        StatementContext statement = ctxProvider.getStatementContext();
        long labelId = 5, propertyKey = 8;

        // WHEN
        IndexRule expectedRule = statement.addIndexRule( labelId, propertyKey );
        tx.success();
        tx.finish();

        // THEN
        tx = db.beginTx();
        statement = ctxProvider.getStatementContext();
        assertEquals( asSet( expectedRule ),
                asSet( statement.getIndexRules( labelId ) ) );
        assertEquals( expectedRule , statement.getIndexRule( labelId, propertyKey ) );
        tx.finish();
    }

    @Test
    public void committedAndTransactionalIndexRulesShouldBeMerged() throws Exception
    {
        // GIVEN
        long labelId = 5, propertyKey = 8;
        Transaction tx = db.beginTx();
        StatementContext statement = ctxProvider.getStatementContext();
        IndexRule existingRule = statement.addIndexRule( labelId, propertyKey );
        tx.success();
        tx.finish();

        // WHEN
        tx = db.beginTx();
        statement = ctxProvider.getStatementContext();
        long propertyKey2 = 10;
        IndexRule addedRule = statement.addIndexRule( labelId, propertyKey2 );
        Set<IndexRule> indexRulesInTx = asSet( statement.getIndexRules( labelId ) );
        tx.success();
        tx.finish();

        // THEN
        assertEquals( asSet( existingRule, addedRule ), indexRulesInTx );
    }

    @Test
    public void rollBackIndexRuleShouldNotBeCommitted() throws Exception
    {
        // GIVEN
        long labelId = 5, propertyKey = 11;
        Transaction tx = db.beginTx();
        StatementContext statement = ctxProvider.getStatementContext();

        // WHEN
        statement.addIndexRule( labelId, propertyKey );
        // don't mark as success
        tx.finish();

        // THEN
        tx = db.beginTx();
        assertEquals( asSet(), asSet( ctxProvider.getStatementContext().getIndexRules( labelId ) ) );
        tx.finish();
    }


    private void awaitIndexOnline( IndexRule indexRule ) throws IndexNotFoundKernelException
    {
        Transaction tx = db.beginTx();
        try
        {
            StatementContext ctx = ctxProvider.getStatementContext();
            long start = System.currentTimeMillis();
            while(true)
            {
                if(ctx.getIndexState(indexRule) == InternalIndexState.ONLINE)
                {
                    break;
                }

                if(start + 1000 * 10 < System.currentTimeMillis())
                {
                    throw new RuntimeException( "Index didn't come online within a reasonable time." );
                }
            }

            tx.success();
        }
        finally
        {
            tx.finish();
        }
    }

    private GraphDatabaseAPI db;
    private ThreadToStatementContextBridge ctxProvider;

    @Before
    public void before() throws Exception
    {
        db = new ImpermanentGraphDatabase();
        ctxProvider = db.getDependencyResolver().resolveDependency(
                ThreadToStatementContextBridge.class );
    }

    @After
    public void after() throws Exception
    {
        db.shutdown();
    }

}
