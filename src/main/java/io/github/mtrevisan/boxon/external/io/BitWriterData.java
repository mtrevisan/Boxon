/*
 * Copyright (c) 2020-2022 Mauro Trevisan
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
package io.github.mtrevisan.boxon.external.io;

import io.github.mtrevisan.boxon.internal.BitSetHelper;
import io.github.mtrevisan.boxon.internal.StringHelper;

import java.io.ByteArrayOutputStream;
import java.util.BitSet;


class BitWriterData{

	/** The backing {@link ByteArrayOutputStream}. */
	private final ByteArrayOutputStream os = new ByteArrayOutputStream(0);

	/** The <i>cache</i> used when writing bits. */
	private byte cache;
	/** The number of bits available (to write) within {@code cache}. */
	private int remaining;


	/**
	 * Skip {@code length} bits.
	 *
	 * @param length	The amount of bits to skip.
	 */
	public final void skipBits(final int length){
		putBitSet(new BitSet(), length, ByteOrder.BIG_ENDIAN);
	}

	/**
	 * Writes {@code value} to this {@link BitWriter} using {@code length} bits in big-endian notation.
	 *
	 * @param bits	The value to write.
	 * @param size	The amount of bits to use when writing {@code value}.
	 * @param bitOrder	The type of endianness: either {@link ByteOrder#LITTLE_ENDIAN} or {@link ByteOrder#BIG_ENDIAN}.
	 */
	public final void putBitSet(BitSet bits, final int size, final ByteOrder bitOrder){
		bits = BitSetHelper.changeBitOrder(bits, bitOrder);

		//if the value that we're writing is too large to be placed entirely in the cache, then we need to place as
		//much as we can in the cache (the least significant bits), flush the cache to the backing ByteBuffer, and
		//place the rest in the cache
		int offset = 0;
		while(offset < size){
			//fill the cache one chunk of bits at a time
			final int length = Math.min(size - offset, Byte.SIZE - remaining);
			final byte nextCache = BitSetHelper.readNextByte(bits, offset, length);
			cache = (byte)((cache << length) | nextCache);
			remaining += length;
			offset += length;

			//if cache is full, write it
			if(remaining == Byte.SIZE){
				os.write(cache);

				resetInnerVariables();
			}
		}
	}

	/**
	 * Writes {@code value} to this {@link BitWriter} using {@code length} bits in big-endian format.
	 *
	 * @param value	The value to write.
	 * @param size	The amount of bits to use when writing {@code value} (MUST BE less than or equals to {@link Long#SIZE}).
	 */
	final void putValue(final long value, final int size){
		final BitSet bits = BitSet.valueOf(new long[]{value});
		putBitSet(bits, size, ByteOrder.BIG_ENDIAN);
	}


	/** Flush a minimum integral number of bytes to the output stream, padding any non-completed byte with zeros. */
	public final void flush(){
		//put the cache into the buffer
		if(remaining > 0)
			os.write(cache);

		resetInnerVariables();
	}

	private void resetInnerVariables(){
		remaining = 0;
		cache = 0;
	}

	/**
	 * Returns a copy of the byte array that backs the buffer.
	 *
	 * @return	The copy of the array that backs this buffer.
	 */
	public final byte[] array(){
		return os.toByteArray();
	}

	@Override
	public final String toString(){
		return StringHelper.toHexString(array());
	}

}
