/*
 * Copyright (c) 2002-2017 "Neo Technology,"
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
package org.neo4j.io.cursor;

import static java.lang.String.format;

public abstract class StoreCursor implements AutoCloseable
{
    private long id;
    private long pageBase = -1;
    private int offset, bound;
    private Progression progression;
    private Page page;
    private long lockToken;

    public final long id()
    {
        return id;
    }

    protected final int cursorBound()
    {
        return bound;
    }

    public final boolean gotoNext()
    {
        throw new UnsupportedOperationException( "not implemented" );
    }

    public final boolean shouldRetry()
    {
        throw new UnsupportedOperationException( "not implemented" );
    }

    @Override
    public void close()
    {
        throw new UnsupportedOperationException( "not implemented" );
    }

    protected final byte readByte( int offset )
    {
        return Memory.getByte( address( offset, 1 ) );
    }

    protected final int unsignedByte( int offset )
    {
        return 0xFF & readByte( offset );
    }

    protected final void writeByte( int offset, byte value )
    {
        Memory.putByte( address( offset, 1 ), value );
    }

    protected final void read( int offset, byte[] target, int pos, int length )
    {
        Memory.copyToArray( address( offset, length ), target, pos, length );
    }

    protected final void write( int offset, byte[] source, int pos, int length )
    {
        Memory.copyFromArray( address( offset, length ), source, pos, length );
    }

    protected final short readShort( int offset )
    {
        return Memory.getShort( address( offset, 2 ) );
    }

    protected final int unsignedShort( int offset )
    {
        return 0xFFFF & readShort( offset );
    }

    protected final void writeShort( int offset, short value )
    {
        Memory.putShort( address( offset, 2 ), value );
    }

    protected final void read( int offset, short[] target, int pos, int length )
    {
        Memory.copyToArray( address( offset, length * 2 ), target, pos, length );
    }

    protected final void write( int offset, short[] source, int pos, int length )
    {
        Memory.copyFromArray( address( offset, length * 2 ), source, pos, length );
    }

    protected final int readInt( int offset )
    {
        return Memory.getInt( address( offset, 4 ) );
    }

    protected final long unsignedInt( int offset )
    {
        return 0xFFFF_FFFFL & readInt( offset );
    }

    protected final void writeInt( int offset, int value )
    {
        Memory.putInt( address( offset, 4 ), value );
    }

    protected final void read( int offset, int[] target, int pos, int length )
    {
        Memory.copyToArray( address( offset, length * 4 ), target, pos, length );
    }

    protected final void write( int offset, int[] source, int pos, int length )
    {
        Memory.copyFromArray( address( offset, length * 4 ), source, pos, length );
    }

    protected final long readLong( int offset )
    {
        return Memory.getLong( address( offset, 8 ) );
    }

    protected final void writeLong( int offset, long value )
    {
        Memory.putLong( address( offset, 8 ), value );
    }

    protected final void read( int offset, long[] target, int pos, int length )
    {
        Memory.copyToArray( address( offset, length * 8 ), target, pos, length );
    }

    protected final void write( int offset, long[] source, int pos, int length )
    {
        Memory.copyFromArray( address( offset, length * 8 ), source, pos, length );
    }

    protected final char readChar( int offset )
    {
        return Memory.getChar( address( offset, 2 ) );
    }

    protected final void writeChar( int offset, char value )
    {
        Memory.putChar( address( offset, 2 ), value );
    }

    protected final void read( int offset, char[] target, int pos, int length )
    {
        Memory.copyToArray( address( offset, length * 2 ), target, pos, length );
    }

    protected final void write( int offset, char[] source, int pos, int length )
    {
        Memory.copyFromArray( address( offset, length * 2 ), source, pos, length );
    }

    protected void fill( int offset, int size, byte data )
    {
        Memory.fill( address( offset, size ), size, data );
    }

    /**
     * Copy the data content of this cursor to the given target cursor. Note that the caller is responsible for making
     * sure that the target cursor is properly initialized and positioned.
     *
     * @param target
     *         the cursor to copy the data to.
     */
    protected final void transferTo( StoreCursor target )
    {
        target.receive( 0, address( 0, bound ), bound );
    }

    protected final void transferTo( int offset, StoreCursor target, int targetOffset, int bytes )
    {
        target.receive( targetOffset, address( offset, bytes ), bytes );
    }

    private void receive( int offset, long sourceAddress, int size )
    {
        Memory.copy( sourceAddress, address( offset, size ), size );
    }

    private long address( int offset, int size )
    {
        assert withinBounds( offset, size );
        return this.pageBase + this.offset + offset;
    }

    private boolean withinBounds( int offset, int size )
    {
        if ( pageBase < 0 )
        {
            throw new IllegalStateException( "Cursor has not been initialized." );
        }
        if ( offset + size > bound )
        {
            throw new IndexOutOfBoundsException( format(
                    "This cursor is bounded to %d bytes, tried to access %d bytes at offset %d.",
                    bound, size, offset ) );
        }
        return true;
    }
}
