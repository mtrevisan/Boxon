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
package io.github.mtrevisan.boxon.core.helpers;

import io.github.mtrevisan.boxon.helpers.JavaHelper;
import io.github.mtrevisan.boxon.helpers.StringHelper;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.BitSet;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiConsumer;


/**
 * Provide bit-level tools for reading bits, skipping bits, and performing fallback after a reading on a {@link ByteBuffer}.
 */
abstract class BitReaderData{

	private static final class Snapshot{
		/** The position in the byte buffer of the cached value. */
		private int position;
		/** The cache used when reading bits. */
		private byte cache;
		/** The number of bits available (to read) within the cache. */
		private int remainingBitsInCache;

		BufferState(final ByteBuffer buffer, final byte cache, final int remainingBitsInCache){
			update(buffer, cache, remainingBitsInCache);
		}

		void update(final ByteBuffer buffer, final byte cache, final int remainingBitsInCache){
			position = buffer.position();
			this.cache = cache;
			this.remainingBitsInCache = remainingBitsInCache;
		}
	}


	/** The backing {@link ByteBuffer}. */
	private final ByteBuffer buffer;

	/** The cache used when reading bits. */
	private byte cache;
	/** The number of bits available (to read) within the cache. */
	private int remainingBitsInCache;

	private BufferState savepoint;


	BitReaderData(final ByteBuffer buffer){
		this.buffer = buffer;
	}


	/**
	 * Create a fallback point that can later be restored (see {@link BitReaderData#restoreSavepoint()}).
	 */
	public final synchronized void createSavepoint(){
		if(savepoint != null)
			//update current mark:
			savepoint.update(buffer, cache, remainingBitsInCache);
		else
			//create new mark
			savepoint = createSnapshot();
	}

	/**
	 * Restore a fallback point created with {@link BitReaderData#createSavepoint()}.
	 */
	public final synchronized void restoreSavepoint(){
		if(savepoint != null){
			//a fallback point has been marked before
			restoreSnapshot(savepoint);

			clearSavepoint();
		}
	}

	/**
	 * Clear fallback point data.
	 * <p>After calling this method, no restoring is possible (see {@link BitReaderData#restoreSavepoint()}).</p>
	 */
	private synchronized void clearSavepoint(){
		savepoint = null;
	}

	private BufferState createSnapshot(){
		return new BufferState(buffer, cache, remainingBitsInCache);
	}

	private void restoreSnapshot(final BufferState snapshot){
		buffer.position(snapshot.position);
		remainingBitsInCache = snapshot.remainingBitsInCache;
		cache = snapshot.cache;
	}


	/**
	 * Reads the next {@code length} bits and composes a long in big-endian notation.
	 *
	 * @param bitsToRead	The amount of bits to read.
	 * @return	A long value at the {@link BitReader}'s current position.
	 */
	final synchronized long readNumber(final int bitsToRead){
		final AtomicLong bitmap = new AtomicLong(0l);
		final BiConsumer<Integer, Integer> numberBufferConsumer = (bitsToProcess, length) -> readFromCache(bitmap, bitsToProcess, length);
		readBits(bitsToRead, numberBufferConsumer);
		return bitmap.get();
	}

	private void readFromCache(final AtomicLong atomicBitmap, final int bitsToRead, final int length){
		long bitmap = atomicBitmap.get();

		final long mask = (0xFFl << byteComplement(length)) & 0xFFl;
		final int shift = remainingBitsInCache - length;
		if(shift == 0)
			bitmap |= (cache & mask) << bitsToRead;
		else
			bitmap |= (cache & mask) >>> shift;
		cache &= (byte)~mask;

		atomicBitmap.set(bitmap);
	}

	/**
	 * Reads the next {@code length} bits and composes a {@link BitSet} in little-endian notation.
	 *
	 * @param bitsToRead	The amount of bits to read.
	 * @return	A {@link BitSet} value at the {@link BitReader}'s current position.
	 */
	public final synchronized BitSet readBitSet(final int bitsToRead){
		final BitSet bitmap = new BitSet(bitsToRead);
		final BiConsumer<Integer, Integer> bitmapBufferConsumer = (bitsToProcess, length) -> readFromCache(bitmap, bitsToProcess, length);
		readBits(bitsToRead, bitmapBufferConsumer);
		return bitmap;
	}

	/**
	 * Add {@code size} bits from the cache starting from <a href="https://en.wikipedia.org/wiki/Bit_numbering#Bit_significance_and_indexing">MSB</a>
	 * with a given offset.
	 *
	 * @param bitmap	The bit set into which to transfer {@code size} bits from the cache.
	 * @param offset	The offset for the indexes.
	 * @param size	The amount of bits to read from the <a href="https://en.wikipedia.org/wiki/Bit_numbering#Bit_significance_and_indexing">MSB</a> of the cache.
	 */
	private void readFromCache(final BitSet bitmap, int offset, final int size){
		offset += size - 1;
		final int consumed = byteComplement(remainingBitsInCache);
		int skip;
		while(cache != 0 && (skip = countLeadingZerosInCache() - consumed) < size){
			bitmap.set(offset - skip);

			cache ^= (byte)(0x80 >> (skip + consumed));
		}
	}

	private int countLeadingZerosInCache(){
		return Integer.numberOfLeadingZeros(cache & 0xFF) - (Integer.SIZE - Byte.SIZE);
	}

	/**
	 * Skips the next {@code length} bits.
	 *
	 * @param bitsToSkip	The amount of bits to skip.
	 */
	final synchronized void skipBits(final int bitsToSkip){
		readBits(bitsToSkip, SKIP_BUFFER_CONSUMER);
	}

	private void readBits(int bitsToProcess, final BiConsumer<Integer, Integer> bitConsumer){
		while(bitsToProcess > 0){
			//if cache is empty and there are more bits to be read, fill it
			if(remainingBitsInCache == 0){
				cache = buffer.get();
				remainingBitsInCache = Byte.SIZE;
			}

			final int length = Math.min(bitsToProcess, remainingBitsInCache);
			bitsToProcess -= length;

			bitConsumer.accept(bitsToProcess, length);

			remainingBitsInCache -= length;
		}
	}

	private static int byteComplement(final int bits){
		return Byte.SIZE - bits;
	}

	/**
	 * Reads {@link Byte#SIZE} bits and composes a {@code byte}.
	 *
	 * @return	A {@code byte}.
	 */
	protected abstract byte readByte();

	/**
	 * Retrieve text until a terminator (NOT consumed!) is found.
	 *
	 * @param baos	The stream to write to.
	 * @param terminator	The terminator.
	 * @throws IOException	If an I/O error occurs.
	 */
	final synchronized void getTextUntilTerminator(final ByteArrayOutputStream baos, final byte terminator) throws IOException{
		while(hasNextByte(terminator))
			baos.write(readByte());
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
			baos.write(readByte());
		baos.flush();
	}

	//FIXME refactor
	private boolean hasNextByte(final byte terminator){
		long bitmap = 0l;
		int bitsToRead = Byte.SIZE;
		final int currentPosition = buffer.position();
		final int bytesRemaining = buffer.limit() - currentPosition;
		long forecastCache = cache;
		int forecastRemaining = remainingBitsInCache;
		int offset = 0;
		//cycle 2 times at most, since a byte can be split at most in two
		while(bitsToRead > 0){
			//if cache is empty and there are more bits to be read, fill it
			if(forecastRemaining == 0){
				if(offset >= bytesRemaining)
					return false;

				//fill cache
				forecastCache = buffer.get(currentPosition + offset);
				forecastRemaining = Byte.SIZE;
			}

			final int length = Math.min(bitsToRead, forecastRemaining);
			bitsToRead -= length;

			final long mask = (0xFFl << byteComplement(length)) & 0xFFl;
			final int shift = forecastRemaining - length;
			if(shift == 0)
				bitmap |= (forecastCache & mask) << bitsToRead;
			else
				bitmap |= (forecastCache & mask) >> shift;

			forecastCache &= ~mask;
			forecastRemaining -= length;
			offset ++;
		}
		return (bitmap != terminator);
	}

	private byte[] peekString(final byte[] peekBuffer){
		//make a copy of internal variables
		final BufferState originalSnapshot = createSnapshot();

		try{
			for(int i = 0, length = peekBuffer.length; i < length; i ++)
				peekBuffer[i] = readByte();
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
		final BufferState originalSnapshot = createSnapshot();

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
		return buffer.position() - JavaHelper.getSizeInBytes(remainingBitsInCache);
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
		remainingBitsInCache = 0;
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
