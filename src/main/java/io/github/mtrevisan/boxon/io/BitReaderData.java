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

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.util.BitSet;


/**
 * Provide bit-level tools for reading bits, skipping bits, and performing fallback after a reading on a {@link ByteBuffer}.
 */
abstract class BitReaderData{

	//FIXME reduce the creation of this class
	private static final class State{
		/** The position in the byte buffer of the cached value. */
		private int position;
		/** The <i>cache</i> used when reading bits. */
		private byte cache;
		/** The number of bits available (to read) within {@code cache}. */
		private int remaining;

		State(final int position, final byte cache, final int remaining){
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

	/** The <i>cache</i> used when reading bits. */
	private byte cache;
	/** The number of bits available (to read) within {@code cache}. */
	private int remaining;

	private State fallbackPoint;


	BitReaderData(final ByteBuffer buffer){
		this.buffer = buffer;
	}


	/**
	 * Create a fallback point that can later be restored (see {@link #restoreFallbackPoint()}).
	 */
	public final void createFallbackPoint(){
		if(fallbackPoint != null)
			//update current mark:
			fallbackPoint.set(buffer.position(), cache, remaining);
		else
			//create new mark
			fallbackPoint = createState();
	}

	/**
	 * Restore a fallback point created with {@link #createFallbackPoint()}.
	 */
	public final void restoreFallbackPoint(){
		if(fallbackPoint != null){
			//a fallback point has been marked before
			restoreState(fallbackPoint);

			clearFallbackPoint();
		}
	}

	/**
	 * Clear fallback point data.
	 * <p>After calling this method, no restoring is possible (see {@link #restoreFallbackPoint()}).</p>
	 */
	public final void clearFallbackPoint(){
		fallbackPoint = null;
	}

	private State createState(){
		return new State(buffer.position(), cache, remaining);
	}

	@SuppressWarnings("AccessingNonPublicFieldOfAnotherObject")
	private void restoreState(final State state){
		buffer.position(state.position);
		remaining = state.remaining;
		cache = state.cache;
	}


	/**
	 * Reads the next {@code length} bits and composes a {@link BitSet} in big-endian notation.
	 *
	 * @param length	The amount of bits to read.
	 * @return	A {@link BitSet} value at the {@link BitReader}'s current position.
	 */
	public final BitSet getBitSet(final int length){
		final BitSet bits = BitSetHelper.createBitSet(length);
		int offset = 0;
		while(offset < length){
			//transfer the cache values
			final int size = Math.min(length, remaining);
			if(size > 0){
				addCacheToBitSet(bits, offset, size);

				offset += size;
			}

			//if cache is empty and there are more bits to be read, fill it
			if(offset < length){
				cache = buffer.get();

				remaining = Byte.SIZE;
			}
		}
		return bits;
	}

	private Byte peekByte(){
		//make a copy of internal variables
		final State originalState = createState();

		Byte b = null;
		try{
			b = getByte();
		}
		catch(final BufferUnderflowException ignored){
			//trap end-of-buffer
		}
		finally{
			//restore original variables
			restoreState(originalState);
		}
		return b;
	}

	/**
	 * Add {@code size} bits from the cache starting from <a href="https://en.wikipedia.org/wiki/Bit_numbering#Bit_significance_and_indexing">LSB</a>
	 * with a given offset.
	 *
	 * @param bits	The bit set into which to transfer {@code size} bits from the cache.
	 * @param offset	The offset for the indexes.
	 * @param size	The amount of bits to read from the <a href="https://en.wikipedia.org/wiki/Bit_numbering#Bit_significance_and_indexing">LSB</a> of the cache.
	 */
	private void addCacheToBitSet(final BitSet bits, final int offset, final int size){
		int skip;
		while(cache != 0 && (skip = Integer.numberOfTrailingZeros(cache & 0xFF)) < size){
			bits.set(skip + offset);
			cache ^= (byte)(1 << skip);
		}
		//remove read bits from the cache
		cache >>= size;
		remaining -= size;
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
	 * @param os	The stream to write to.
	 * @param terminator	The terminator.
	 * @throws IOException	If an I/O error occurs.
	 */
	protected final void getTextUntilTerminator(final OutputStreamWriter os, final byte terminator) throws IOException{
		for(Byte byteRead = peekByte(); byteRead != null && byteRead != terminator; byteRead = peekByte())
			os.write(getByte());
		os.flush();
	}

	/**
	 * Retrieve text until a terminator is found. Not bytes are consumed.
	 *
	 * @param os	The stream to write to.
	 * @param terminator	The terminator.
	 * @throws IOException	If an I/O error occurs.
	 */
	protected final void getTextUntilTerminatorWithoutConsuming(final OutputStreamWriter os, final byte terminator) throws IOException{
		//make a copy of internal variables
		final State originalState = createState();

		try{
			getTextUntilTerminator(os, terminator);
		}
		catch(final BufferUnderflowException ignored){
			//trap end-of-buffer
		}
		finally{
			//restore original variables
			restoreState(originalState);
		}
	}


	/**
	 * Returns the byte array that backs this reader.
	 *
	 * @return	The array that backs this reader.
	 */
	public final byte[] array(){
		return buffer.array();
	}

	/**
	 * Gets the position of the backing {@link ByteBuffer} in integral number of {@code byte}s (lower bound).
	 *
	 * @return	The position of the backing buffer in {@code byte}s.
	 */
	public final int position(){
		return buffer.position() - ((remaining + Byte.SIZE - 1) >>> 3);
	}

	/**
	 * Sets the position of the backing {@link ByteBuffer} in {@code byte}s.
	 *
	 * @param newPosition	The position of the backing buffer in {@code byte}s.
	 */
	public final void position(final int newPosition){
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
	public final boolean hasRemaining(){
		return buffer.hasRemaining();
	}

	@Override
	public final String toString(){
		return StringHelper.toHexString(array());
	}

}
