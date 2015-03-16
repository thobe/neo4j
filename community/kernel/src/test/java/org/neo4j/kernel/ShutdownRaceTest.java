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

package org.neo4j.kernel;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Rule;
import org.junit.Test;

import org.neo4j.function.Function;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Transaction;
import org.neo4j.kernel.impl.util.function.Optional;
import org.neo4j.test.Barrier;
import org.neo4j.test.DatabaseRule;
import org.neo4j.test.EmbeddedDatabaseRule;
import org.neo4j.test.ThreadingRule;
import org.neo4j.tooling.GlobalGraphOperations;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import static org.neo4j.helpers.collection.Iterables.count;
import static org.neo4j.kernel.impl.util.function.Optionals.flatMapFutures;
import static org.neo4j.kernel.impl.util.function.Optionals.none;
import static org.neo4j.kernel.impl.util.function.Optionals.some;

public class ShutdownRaceTest
{
    private static final Function<GraphDatabaseService, Long> COUNT_NODES = new Function<GraphDatabaseService, Long>()
    {
        @Override
        public Long apply( GraphDatabaseService db )
        {
            return count( GlobalGraphOperations.at( db ).getAllNodes() );
        }
    };
    public final @Rule DatabaseRule db = new EmbeddedDatabaseRule();
    public final @Rule ThreadingRule threading = new ThreadingRule();
    private final Function<Barrier, Optional<Throwable>> createNode = new Function<Barrier, Optional<Throwable>>()
    {
        @Override
        public Optional<Throwable> apply( Barrier barrier )
        {
            GraphDatabaseService graphDb = db.getGraphDatabaseService();
            try ( Transaction tx = graphDb.beginTx() )
            {
                barrier.reached();
                graphDb.createNode();

                tx.success();
            }
            catch ( Throwable e )
            {
                return some( e );
            }
            return none();
        }

        @Override
        public String toString()
        {
            return "createNode";
        }
    }, restart = new Function<Barrier, Optional<Throwable>>()
    {
        @Override
        public Optional<Throwable> apply( Barrier barrier )
        {
            try
            {
                barrier.reached();
                db.restartDatabase();
            }
            catch ( Throwable e )
            {
                return some( e );
            }
            return none();
        }

        @Override
        public String toString()
        {
            return "restart";
        }
    };

    @Test
    public void shouldApplyAllCommittedTransactionsBeforeShuttingDown() throws Exception
    {
        // given
        int workers = 1;
        Barrier.Control barrier = Barrier.Control.multiple( workers );

        // when
        List<Future<Optional<Throwable>>> futures = new ArrayList<>( workers + 1 );
        for ( int i = 0; i < workers; i++ )
        {
            futures.add( threading.execute( createNode, barrier ) );
        }
        futures.add( threading.execute( restart, barrier.inverted() ) );

        // then
        List<Throwable> failures = flatMapFutures( futures );
        assertEquals( workers, db.tx( COUNT_NODES ).apply( db.getGraphDatabaseService() ).longValue() );
        assertThat( failures, isEmptyList() );
    }

    private static Matcher<? super List<Throwable>> isEmptyList()
    {
        return new TypeSafeMatcher<List<Throwable>>()
        {
            @Override
            protected boolean matchesSafely( List<Throwable> item )
            {
                return item.isEmpty();
            }

            @Override
            public void describeTo( Description description )
            {
                description.appendText( "empty list" );
            }
        };
    }
}
