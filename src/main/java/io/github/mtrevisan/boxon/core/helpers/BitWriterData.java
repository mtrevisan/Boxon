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

import io.github.mtrevisan.boxon.helpers.StringHelper;

import java.io.ByteArrayOutputStream;
import java.util.BitSet;


/**
 * Provide bit-level tools for writing bits and skipping bits on a {@link ByteArrayOutputStream}.
 */
class BitWriterData{

	/** The backing {@link ByteArrayOutputStream}. */
	private final ByteArrayOutputStream os = new ByteArrayOutputStream(0);

	/** The <i>cache</i> used when writing bits. */
	private byte cache;
	/** The number of bits available (to write) within {@code cache}. */
	private int remaining = Byte.SIZE;


	/**
	 * Writes {@code value} to this {@link BitWriter} in big-endian format.
	 *
	 * @param value	The value to write.
	 */
	final synchronized void putNumber(final byte value){
		putNumber(value, Byte.SIZE);
	}

	/**
	 * Writes {@code value} to this {@link BitWriter} in big-endian format.
	 *
	 * @param value	The value to write.
	 */
	final synchronized void putNumber(final short value){
		putNumber(value, Short.SIZE);
	}

	/**
	 * Writes {@code value} to this {@link BitWriter} in big-endian format.
	 *
	 * @param value	The value to write.
	 */
	final synchronized void putNumber(final int value){
		putNumber(value, Integer.SIZE);
	}

	/**
	 * Writes {@code value} to this {@link BitWriter} in big-endian format.
	 *
	 * @param value	The value to write.
	 */
	final synchronized void putNumber(final long value){
		putNumber(value, Long.SIZE);
	}

	/**
	 * Writes {@code value} to this {@link BitWriter} in big-endian format.
	 *
	 * @param value	The value to write.
	 * @param bitsToWrite	The amount of bits to use when writing the {@code value}.
	 */
	private void putNumber(final long value, int bitsToWrite){
		while(bitsToWrite > 0){
			//fill the cache one chunk of bits at a time
			final int length = Math.min(bitsToWrite, remaining);
			bitsToWrite -= length;

			final long mask = (1l << length) - 1;
			cache |= (byte)((value >> bitsToWrite) & mask);
			remaining -= length;

			//if cache is full, write it
			if(remaining == 0){
				os.write(cache);

				resetCache();
			}
		}
	}

	/**
	 * Writes {@code value} to this {@link BitWriter} using {@code length} bits in big-endian notation.
	 *
	 * @param bitmap	The value to write.
	 * @param bitsToWrite	The amount of bits to use when writing the {@code bitmap}.
	 */
	public final synchronized void putBitSet(final BitSet bitmap, int bitsToWrite){
		while(bitsToWrite > 0){
			//fill the cache one chunk of bits at a time
			final int length = Math.min(bitsToWrite, remaining);

			final byte nextCache = getNextByte(bitmap, bitsToWrite - 1, length);
			cache = (byte)((cache << (Byte.SIZE - length)) | nextCache);
			remaining -= length;
			bitsToWrite -= length;

			//if cache is full, write it
			if(remaining == 0){
				os.write(cache);

				resetCache();
			}
		}
	}

	/**
	 * Returns a long of given length and starting at a given offset.
	 *
	 * @param bitmap	The bit set.
	 * @param offset	The bit offset to start the extraction.
	 * @param size	The amount of bits to use when writing the {@code bitmap} (MUST BE less than or equals to {@link Integer#MAX_VALUE}).
	 * @return	A long starting at a given offset and of a given length.
	 */
	private byte getNextByte(final BitSet bitmap, final int offset, final int size){
		byte valueRead = 0;
		final int consumed = Byte.SIZE - remaining;
		int index = offset + 2;
		while((index = bitmap.previousSetBit(index - 1) - consumed) >= 0 && offset - index < size)
			valueRead |= (byte)(0x80 >> (offset - index));
		return valueRead;
	}

	/**
	 * Skip {@code length} bits.
	 *
	 * @param bitsToWrite	The amount of bits to skip.
	 */
	public final synchronized void skipBits(int bitsToWrite){
		while(bitsToWrite > 0){
			//fill the cache one chunk of bits at a time
			final int length = Math.min(bitsToWrite, remaining);
			bitsToWrite -= length;

			cache <<= length;
			remaining -= length;

			//if cache is full, write it
			if(remaining == 0){
				os.write(cache);

				resetCache();
			}
		}
	}


	/** Flush a minimum integral number of bytes to the output stream, padding any non-completed byte with zeros. */
	public final synchronized void flush(){
		//put the cache into the buffer
		remaining = Byte.SIZE - remaining;
		if(remaining > 0)
			//align the remaining bits in the cache
			os.write(cache << remaining);

		resetCache();
	}

	private void resetCache(){
		cache = 0;
		remaining = Byte.SIZE;
	}

	/**
	 * Returns a copy of the byte array that backs the buffer.
	 *
	 * @return	The copy of the array that backs this buffer.
	 */
	public final synchronized byte[] array(){
		return os.toByteArray();
	}

	@Override
	public final String toString(){
		return StringHelper.toHexString(array());
	}

}
