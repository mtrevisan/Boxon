/*
 * Copyright (c) 2020-2024 Mauro Trevisan
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */
package io.github.mtrevisan.boxon.io;

import io.github.mtrevisan.boxon.helpers.StringHelper;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.BitSet;


/**
 * Provide bit-level tools for reading bits, skipping bits, and performing fallback after a reading on a {@link ByteBuffer}.
 */
abstract class BitReaderData{

	//FIXME try to reduce the number of instances of this class
	private static final class Snapshot{
		/** The position in the byte buffer of the cached value. */
		private int position;
		/** The cache used when reading bits. */
		private byte cache;
		/** The number of bits available (to read) within the cache. */
		private int remaining;

		Snapshot(final int position, final byte cache, final int remaining){
			set(position, cache, remaining);
		}

		void set(final int position, final byte cache, final int remaining){
			this.position = position;
			this.cache = cache;
			this.remaining = remaining;
		}
	}


	/** The backing {@link ByteBuffer}. */
	private final ByteBuffer buffer;

	/** The cache used when reading bits. */
	private byte cache;
	/** The number of bits available (to read) within the cache. */
	private int remaining;

	private Snapshot fallbackPoint;


	BitReaderData(final ByteBuffer buffer){
		this.buffer = buffer;
	}


	/**
	 * Create a fallback point that can later be restored (see {@link BitReaderData#restoreFallbackPoint()}).
	 */
	public final synchronized void createFallbackPoint(){
		if(fallbackPoint != null)
			//update current mark:
			fallbackPoint.set(buffer.position(), cache, remaining);
		else
			//create new mark
			fallbackPoint = createSnapshot();
	}

	/**
	 * Restore a fallback point created with {@link BitReaderData#createFallbackPoint()}.
	 */
	public final synchronized void restoreFallbackPoint(){
		if(fallbackPoint != null){
			//a fallback point has been marked before
			restoreSnapshot(fallbackPoint);

			clearFallbackPoint();
		}
	}

	/**
	 * Clear fallback point data.
	 * <p>After calling this method, no restoring is possible (see {@link BitReaderData#restoreFallbackPoint()}).</p>
	 */
	private synchronized void clearFallbackPoint(){
		fallbackPoint = null;
	}

	private Snapshot createSnapshot(){
		return new Snapshot(buffer.position(), cache, remaining);
	}

	@SuppressWarnings("AccessingNonPublicFieldOfAnotherObject")
	private void restoreSnapshot(final Snapshot snapshot){
		buffer.position(snapshot.position);
		remaining = snapshot.remaining;
		cache = snapshot.cache;
	}


	/**
	 * Reads the next {@code length} bits and composes a long in little-endian notation.
	 *
	 * @param bitsToRead	The amount of bits to read.
	 * @return	A long value at the {@link BitReader}'s current position.
	 */
	final synchronized long getNumber(final int bitsToRead){
		long bitmap = 0l;

		int bitsRead = 0;
		while(bitsRead < bitsToRead){
			//if cache is empty and there are more bits to be read, fill it
			if(remaining == 0)
				fillCache();

			final int length = Math.min(bitsToRead - bitsRead, remaining);
			bitmap = readFromCache(bitmap, bitsRead, length);
			bitsRead += length;

			consumeCache(length);
		}
		return bitmap;
	}

	/**
	 * Add {@code size} bits from the cache starting from <a href="https://en.wikipedia.org/wiki/Bit_numbering#Bit_significance_and_indexing">LSB</a>
	 * with a given offset.
	 *
	 * @param bitmap	The bit set into which to transfer {@code size} bits from the cache.
	 * @param offset	The offset for the indexes.
	 * @param size	The amount of bits to read from the <a href="https://en.wikipedia.org/wiki/Bit_numbering#Bit_significance_and_indexing">LSB</a> of the cache.
	 */
	private long readFromCache(long bitmap, final int offset, final int size){
		int skip;
		while(cache != 0 && (skip = Integer.numberOfTrailingZeros(cache & 0xFF)) < size){
			bitmap |= 1l << (skip + offset);
			cache ^= (byte)(1 << skip);
		}
		return bitmap;
	}

	/**
	 * Reads the next {@code length} bits and composes a {@link BitSet} in little-endian notation.
	 *
	 * @param bitsToRead	The amount of bits to read.
	 * @return	A {@link BitSet} value at the {@link BitReader}'s current position.
	 */
	public final synchronized BitSet getBitSet(final int bitsToRead){
		final BitSet bitmap = new BitSet(bitsToRead);

		int bitsRead = 0;
		while(bitsRead < bitsToRead){
			//if cache is empty and there are more bits to be read, fill it
			if(remaining == 0)
				fillCache();

			final int length = Math.min(bitsToRead - bitsRead, remaining);
			readFromCache(bitmap, bitsRead, length);
			bitsRead += length;

			consumeCache(length);
		}
		return bitmap;
	}

	/**
	 * Add {@code size} bits from the cache starting from <a href="https://en.wikipedia.org/wiki/Bit_numbering#Bit_significance_and_indexing">LSB</a>
	 * with a given offset.
	 *
	 * @param bitmap	The bit set into which to transfer {@code size} bits from the cache.
	 * @param offset	The offset for the indexes.
	 * @param size	The amount of bits to read from the <a href="https://en.wikipedia.org/wiki/Bit_numbering#Bit_significance_and_indexing">LSB</a> of the cache.
	 */
	private void readFromCache(final BitSet bitmap, final int offset, final int size){
		int skip;
		while(cache != 0 && (skip = Integer.numberOfTrailingZeros(cache & 0xFF)) < size){
			bitmap.set(skip + offset);
			cache ^= (byte)(1 << skip);
		}
	}

	/**
	 * Skips the next {@code length} bits.
	 *
	 * @param bitsToSkip	The amount of bits to skip.
	 */
	final synchronized void skipBits(final int bitsToSkip){
		int bitsSkipped = 0;
		while(bitsSkipped < bitsToSkip){
			//if cache is empty and there are more bits to be read, fill it
			if(remaining == 0)
				fillCache();

			final int length = Math.min(bitsToSkip - bitsSkipped, remaining);
			bitsSkipped += length;

			consumeCache(length);
		}
	}

	private void consumeCache(final int size){
		//remove read bits from the cache
		cache >>= size;
		remaining -= size;
	}

	private void fillCache(){
		cache = buffer.get();
		remaining = Byte.SIZE;
	}

	/**
	 * Reads {@link Byte#SIZE} bits and composes a {@code byte}.
	 *
	 * @return	A {@code byte}.
	 */
	public abstract byte getByte();

	/**
	 * Retrieve text until a terminator (NOT consumed!) is found.
	 *
	 * @param baos	The stream to write to.
	 * @param terminator	The terminator.
	 * @throws IOException	If an I/O error occurs.
	 */
	final synchronized void getTextUntilTerminator(final ByteArrayOutputStream baos, final byte terminator) throws IOException{
		Byte byteRead;
		while((byteRead = peekByte()) != null && byteRead != terminator)
			baos.write(getByte());
		baos.flush();
	}

	/**
	 * Retrieve text until a terminator (NOT consumed!) is found.
	 *
	 * @param baos	The stream to write to.
	 * @param terminator	The terminator.
	 * @param charset	The charset.
	 * @throws IOException	If an I/O error occurs.
	 */
	final synchronized void getTextUntilTerminator(final ByteArrayOutputStream baos, final String terminator, final Charset charset)
			throws IOException{
		final byte[] terminatorArray = terminator.getBytes(charset);
		final byte[] peekBuffer = new byte[terminatorArray.length];

		while(peekString(peekBuffer) != null && !Arrays.equals(terminatorArray, peekBuffer))
			baos.write(getByte());
		baos.flush();
	}

	private Byte peekByte(){
		//make a copy of internal variables
		final Snapshot originalSnapshot = createSnapshot();

		Byte b = null;
		try{
			b = getByte();
		}
		catch(final BufferUnderflowException ignored){
			//trap end-of-buffer
		}
		finally{
			//restore original variables
			restoreSnapshot(originalSnapshot);
		}
		return b;
	}

	private byte[] peekString(final byte[] peekBuffer){
		//make a copy of internal variables
		final Snapshot originalSnapshot = createSnapshot();

		try{
			for(int i = 0, length = peekBuffer.length; i < length; i ++)
				peekBuffer[i] = getByte();
		}
		catch(final BufferUnderflowException ignored){
			//trap end-of-buffer
			return null;
		}
		finally{
			//restore original variables
			restoreSnapshot(originalSnapshot);
		}

		return peekBuffer;
	}

	/**
	 * Retrieve text until a terminator is found. Not bytes are consumed.
	 *
	 * @param baos	The stream to write to.
	 * @param terminator	The terminator.
	 * @throws IOException	If an I/O error occurs.
	 */
	final synchronized void getTextUntilTerminatorWithoutConsuming(final ByteArrayOutputStream baos, final byte terminator)
			throws IOException{
		//make a copy of internal variables
		final Snapshot originalSnapshot = createSnapshot();

		try{
			getTextUntilTerminator(baos, terminator);
		}
		catch(final BufferUnderflowException ignored){
			//trap end-of-buffer
		}
		finally{
			//restore original variables
			restoreSnapshot(originalSnapshot);
		}
	}


	/**
	 * Returns the byte array that backs this reader.
	 *
	 * @return	The array that backs this reader.
	 */
	public final synchronized byte[] array(){
		return buffer.array();
	}

	/**
	 * Gets the position of the backing {@link ByteBuffer} in integral number of {@code byte}s (lower bound).
	 *
	 * @return	The position of the backing buffer in {@code byte}s.
	 */
	public final synchronized int position(){
		return buffer.position() - ((remaining + Byte.SIZE - 1) >>> 3);
	}

	/**
	 * Sets the position of the backing {@link ByteBuffer} in {@code byte}s.
	 *
	 * @param newPosition	The position of the backing buffer in {@code byte}s.
	 */
	public final synchronized void position(final int newPosition){
		buffer.position(newPosition);

		resetInnerVariables();
	}

	private void resetInnerVariables(){
		remaining = 0;
		cache = 0;
	}

	/**
	 * Tells whether there are any elements between the current position and the limit of the underlying {@link ByteBuffer}.
	 *
	 * @return	Whether there is at least one element remaining in the underlying {@link ByteBuffer}.
	 */
	public final synchronized boolean hasRemaining(){
		return buffer.hasRemaining();
	}

	@Override
	public final String toString(){
		return StringHelper.toHexString(array());
	}

}
