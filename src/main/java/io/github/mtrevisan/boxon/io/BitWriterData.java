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
package io.github.mtrevisan.boxon.io;

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
	private int remaining;


	/**
	 * Skip {@code length} bits.
	 *
	 * @param length	The amount of bits to skip.
	 */
	public final void skipBits(final int length){
		putBitSet(new BitSet(0), length, ByteOrder.BIG_ENDIAN);
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
			final byte nextCache = readNextByte(bits, offset, length);
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
	 * Returns a long of given length and starting at a given offset.
	 *
	 * @param bits	The bit set.
	 * @param offset	The bit offset to start the extraction.
	 * @param size	The length in bits of the extraction (MUST BE less than {@link Long#SIZE}!).
	 * @return	A long starting at a given offset and of a given length.
	 */
	private static byte readNextByte(final BitSet bits, final int offset, final int size){
		byte value = 0;
		int index = bits.nextSetBit(offset);
		while(index >= 0 && index <= offset + size){
			value |= (byte)(1 << (index - offset));

			index = bits.nextSetBit(index + 1);
		}
		return value;
	}

	/**
	 * Writes {@code value} to this {@link BitWriter} using {@code length} bits in big-endian format.
	 *
	 * @param value	The value to write.
	 * @param size	The amount of bits to use when writing {@code value} (MUST BE less than or equals to {@link Long#SIZE}).
	 */
	@SuppressWarnings("WeakerAccess")
	public final void putValue(final long value, final int size){
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
