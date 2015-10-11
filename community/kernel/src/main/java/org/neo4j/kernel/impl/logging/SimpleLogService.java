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
package org.neo4j.kernel.impl.logging;

import org.neo4j.logging.DuplicatingLogProvider;
import org.neo4j.logging.LogProvider;

/**
 * Simplest form of a {@link LogService} where already constructed {@link LogProvider} instances
 * are handed into the constructor.
 */
public class SimpleLogService extends AbstractLogService
{
    private final LogProvider userLogProvider;
    private final LogProvider internalLogProvider;

    public SimpleLogService( LogProvider userLogProvider, LogProvider internalLogProvider )
    {
        this.userLogProvider = new DuplicatingLogProvider( userLogProvider, internalLogProvider );
        this.internalLogProvider = internalLogProvider;
    }

    @Override
    public LogProvider getUserLogProvider()
    {
        return this.userLogProvider;
    }

    @Override
    public LogProvider getInternalLogProvider()
    {
        return this.internalLogProvider;
    }
}
