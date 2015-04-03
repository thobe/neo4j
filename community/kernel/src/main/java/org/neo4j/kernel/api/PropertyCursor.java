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

import org.neo4j.kernel.api.properties.DefinedProperty;

public interface PropertyCursor extends AutoCloseable
{
    boolean propertyNext();

    boolean propertyFind( int propertyKeyId );

    int propertyKeyId();

    PropertyType propertyType();

    DefinedProperty property();

    Object propertyValue();

    boolean propertyValueEquals( Object value );

    boolean propertyValueEquals( CharSequence value );

    boolean propertyValueEquals( long value );

    boolean propertyValueEquals( double value );

    boolean propertyValueEquals( boolean value );

    void extractValue( PropertyWriter writer );

    boolean propertyValueLessThan( long value );

    boolean propertyValueLessThan( double value );

    boolean propertyValueLessThanOrEqualTo( long value );

    boolean propertyValueLessThanOrEqualTo( double value );

    boolean propertyValueGreaterThanOrEqualTo( long value );

    boolean propertyValueGreaterThanOrEqualTo( double value );

    boolean propertyValueGreaterThan( long value );

    boolean propertyValueGreaterThan( double value );

    @Override
    void close();
}
