/*
 * Copyright (c) 2002-2017 "Neo Technology,"
 * Network Engine for Objects in Lund AB [http://neotechnology.com]
 *
 * This file is part of Neo4j.
 *
 * Neo4j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.neo4j.io.cursor.pages;

import java.io.File;
import java.io.IOException;

import org.neo4j.io.cursor.StoreCursor;

public class PageCursor extends StoreCursor
{
    private static final int BYTE = 1, SHORT = 2, INT = 4, LONG = 8;
    private int offset;

    public byte getByte()
    {
        byte v = readByte( offset );
        offset += BYTE;
        return v;
    }

    public byte getByte( int offset )
    {
        return readByte( offset );
    }

    public void putByte( byte value )
    {
        writeByte( offset, value );
        offset += BYTE;
    }

    public void putByte( int offset, byte value )
    {
        writeByte( offset, value );
    }

    public long getLong()
    {
        long v = readLong( offset );
        offset += LONG;
        return v;
    }

    public long getLong( int offset )
    {
        return readLong( offset );
    }

    public void putLong( long value )
    {
        writeLong( offset, value );
        offset += LONG;
    }

    public void putLong( int offset, long value )
    {
        writeLong( offset, value );
    }

    public int getInt()
    {
        int v = readInt( offset );
        offset += INT;
        return v;
    }

    public int getInt( int offset )
    {
        return readInt( offset );
    }

    public void putInt( int value )
    {
        writeInt( offset, value );
        offset += INT;
    }

    public void putInt( int offset, int value )
    {
        writeInt( offset, value );
    }

    public void getBytes( byte[] data )
    {
        getBytes( data, 0, data.length );
    }

    public void getBytes( byte[] data, int arrayOffset, int length )
    {
        read( offset, data, arrayOffset, length );
        offset += length;
    }

    public void putBytes( byte[] data )
    {
        putBytes( data, 0, data.length );
    }

    public void putBytes( byte[] data, int arrayOffset, int length )
    {
        write( offset, data, arrayOffset, length );
        offset += length;
    }

    public short getShort()
    {
        short v = readShort( offset );
        offset += SHORT;
        return v;
    }

    public short getShort( int offset )
    {
        return readShort( offset );
    }

    public void putShort( short value )
    {
        writeShort( offset, value );
        offset += SHORT;
    }

    public void putShort( int offset, short value )
    {
        writeShort( offset, value );
    }

    public void setOffset( int offset )
    {
        this.offset = offset;
    }

    public int getOffset()
    {
        return offset;
    }

    public long getCurrentPageId()
    {
        return id();
    }

    public int getCurrentPageSize()
    {
        return cursorBound();
    }

    public File getCurrentFile(){
        throw new UnsupportedOperationException( "not implemented" );
    }

    /**
     * Rewinds the cursor to its initial condition, as if freshly returned from
     * an equivalent io() call. In other words, the next call to next() will
     * move the cursor to the starting page that was specified in the io() that
     * produced the cursor.
     */
    public void rewind()
    {
        throw new UnsupportedOperationException( "not implemented" );
    }

    public boolean next() throws IOException
    {
        return gotoNext();
    }

    public boolean next( long pageId ) throws IOException
    {
        throw new UnsupportedOperationException( "not implemented" );
    }

    /**
     * Copy the specified number of bytes from the given offset of this page, to the given offset of the target page.
     * <p>
     * If the length reaches beyond the end of either cursor, then only as many bytes as are available in this cursor,
     * or can fit in the target cursor, are actually copied.
     * <p>
     * <strong>Note</strong> that {@code copyTo} is only guaranteed to work when both target and source cursor are from
     * the <em>same</em> page cache implementation. Using wrappers, delegates or mixing cursor implementations may
     * produce unspecified errors.
     *
     * @param sourceOffset The offset into this page to copy from.
     * @param targetCursor The cursor the data will be copied to.
     * @param targetOffset The offset into the target cursor to copy to.
     * @param lengthInBytes The number of bytes to copy.
     * @return The number of bytes actually copied.
     */
    public int copyTo( int sourceOffset, PageCursor targetCursor, int targetOffset, int lengthInBytes )
    {
        transferTo( sourceOffset, targetCursor, targetOffset, lengthInBytes );
        return lengthInBytes;
    }

//    /**
//     * Discern whether an out-of-bounds access has occurred since the last call to {@link #next()} or
//     * {@link #next(long)}, or since the last call to {@link #shouldRetry()} that returned {@code true}, or since the
//     * last call to this method.
//     *
//     * @return {@code true} if an access was out of bounds, or the {@link #raiseOutOfBounds()} method has been called.
//     */
//    public abstract boolean checkAndClearBoundsFlag();

//    /**
//     * Check if a cursor error has been set on this or any linked cursor, and if so, remove it from the cursor
//     * and throw it as a {@link CursorException}.
//     */
//    public abstract void checkAndClearCursorException() throws CursorException;

//    /**
//     * Explicitly raise the out-of-bounds flag.
//     *
//     * @see #checkAndClearBoundsFlag()
//     */
//    public abstract void raiseOutOfBounds();

//    /**
//     * Set an error condition on the cursor with the given message.
//     * <p>
//     * This will make calls to {@link #checkAndClearCursorException()} throw a {@link CursorException} with the given
//     * message, unless the error has gotten cleared by a {@link #shouldRetry()} call that returned {@code true},
//     * a call to {@link #next()} or {@link #next(long)}, or the cursor is closed.
//     *
//     * @param message The message of the {@link CursorException} that {@link #checkAndClearCursorException()} will
//     * throw.
//     */
//    public abstract void setCursorException( String message );

//    /**
//     * Unconditionally clear any error condition that has been set on this or any linked cursor, without throwing an
//     * exception.
//     */
//    public abstract void clearCursorException();

//    /**
//     * Open a new page cursor with the same pf_flags as this cursor, as if calling the {@link PagedFile#io(long, int)}
//     * on the relevant paged file. This cursor will then also delegate to the linked cursor when checking
//     * {@link #shouldRetry()} and {@link #checkAndClearBoundsFlag()}.
//     * <p>
//     * Opening a linked cursor on a cursor that already has a linked cursor, will close the older linked cursor.
//     * Closing a cursor also closes any linked cursor.
//     *
//     * @param pageId The page id that the linked cursor will be placed at after its first call to {@link #next()}.
//     * @return A cursor that is linked with this cursor.
//     */
//    public abstract PageCursor openLinkedCursor( long pageId );

    /**
     * Sets all bytes in this page to zero, as if this page was newly allocated at the end of the file.
     */
    public void zapPage()
    {
        fill( 0, cursorBound(), (byte) 0 );
    }

    /**
     * @return {@code true} if this page cursor was opened with {@link PagedFile#PF_SHARED_WRITE_LOCK},
     * {@code false} otherwise.
     */
    public boolean isWriteLocked()
    {
        throw new UnsupportedOperationException( "not implemented" );
    }
}
