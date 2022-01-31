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
package io.github.mtrevisan.boxon.internal;

import io.github.mtrevisan.boxon.external.io.ByteOrder;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.BitSet;


public final class BitSetHelper{

	private BitSetHelper(){}


	/**
	 * Converts a BigInteger into a byte array ignoring the sign of the BigInteger, according to SRP specification.
	 *
	 * @param value	the value, must not be {@code null}.
	 * @param size	The size in bits of the value.
	 * @param byteOrder	The type of endianness: either {@link ByteOrder#LITTLE_ENDIAN} or {@link ByteOrder#BIG_ENDIAN}.
	 * @return	The bit set representing the given value.
	 */
	public static BitSet valueOf(final BigInteger value, final int size, final ByteOrder byteOrder){
		byte[] array = value.toByteArray();
		final int newSize = (size + Byte.SIZE - 1) >>> 3;
		if(newSize != array.length){
			final int offset = Math.max(array.length - newSize, 0);
			final byte[] newArray = new byte[newSize];
			final int newArrayOffset = Math.max(newArray.length - array.length, 0);
			System.arraycopy(array, offset, newArray, newArrayOffset, array.length - offset);
			array = newArray;
		}
		if(byteOrder == ByteOrder.LITTLE_ENDIAN)
			//NOTE: need to reverse the bytes because BigInteger is big-endian and BitMap is little-endian
			byteReverse(array);
		return BitSet.valueOf(array);
	}

	/**
	 * Returns a long of given length and starting at a given offset.
	 *
	 * @param bits	The bit set.
	 * @param offset	The bit offset to start the extraction.
	 * @param size	The length in bits of the extraction (MUST BE less than {@link Long#SIZE}!).
	 * @return	A long starting at a given offset and of a given length.
	 */
	public static byte readNextByte(final BitSet bits, final int offset, final int size){
		byte value = 0;
		int index = bits.nextSetBit(offset);
		while(index >= 0 && index <= offset + size){
			value |= 1 << (index - offset);

			index = bits.nextSetBit(index + 1);
		}
		return value;
	}

	/**
	 * Sets the bits of a number to the complement of its current value.
	 *
	 * @param bits	The bit set.
	 * @param size	The size of the number in bits.
	 */
	public static void flipBits(final BitSet bits, final int size){
		bits.flip(0, size);
	}


	/**
	 * In-place reverse the order of the given array byte-by-byte.
	 *
	 * @param array	The array to be reversed.
	 * @param byteOrder	The byte order.
	 */
	public static void changeByteOrder(final byte[] array, final ByteOrder byteOrder){
		if(byteOrder == ByteOrder.LITTLE_ENDIAN)
			for(int start = 0, end = array.length - 1; start < end; start ++, end --)
				//swap array[start] with array[end]
				array[start] ^= array[end] ^ (array[end] = array[start]);
	}

	/**
	 * Change the byte order appropriately.
	 *
	 * @param bits	The bit set.
	 * @param bitOrder	The byte order.
	 */
	public static BitSet changeBitOrder(final BitSet bits, final ByteOrder bitOrder){
		return (bitOrder == ByteOrder.LITTLE_ENDIAN? bitReverse(bits): bits);
	}

	/**
	 * Reverse the endianness bit by bit.
	 *
	 * @param bits	The bit set.
	 */
	private static BitSet bitReverse(final BitSet bits){
		final byte[] array = bits.toByteArray();
		bitReverse(array);
		return BitSet.valueOf(array);
	}

	/**
	 * Reverse the endianness bit by bit.
	 *
	 * @param array	The array to be reversed.
	 */
	private static void bitReverse(final byte[] array){
		for(int i = 0; i < array.length; i ++)
			array[i] = reverseBits(array[i]);
		byteReverse(array);
	}

	/**
	 * Reverse the endianness bit by bit.
	 *
	 * @param number	The byte to be reversed.
	 */
	private static byte reverseBits(byte number){
		byte reverse = 0;
		for(int i = Byte.SIZE - 1; i >= 0; i --){
			reverse += ((number & 1) << i);
			number >>= 1;
		}
		return reverse;
	}

	/**
	 * In-place reverse the order of the given array.
	 *
	 * @param array	The array to be reversed.
	 */
	private static void byteReverse(final byte[] array){
		for(int start = 0, end = array.length - 1; start < end; start ++, end --)
			//swap array[start] with array[end]
			array[start] ^= array[end] ^ (array[end] = array[start]);
	}


	/**
	 * Convert this bit set to {@link BigInteger}.
	 *
	 * @param bits	The bit set.
	 * @param size	The number of bits.
	 * @param byteOrder	The byte order.
	 * @return	The converted {@link BigInteger}.
	 */
	public static BigInteger toInteger(final BitSet bits, final int size, final ByteOrder byteOrder){
		byte[] array = bits.toByteArray();
		final int expectedLength = size >>> 3;
		if(array.length < expectedLength)
			array = Arrays.copyOf(array, expectedLength);
		if(byteOrder == ByteOrder.LITTLE_ENDIAN)
			//NOTE: need to reverse the bytes because BigInteger is big-endian and BitMap is little-endian
			byteReverse(array);
		return extendSign(array);
	}

	/**
	 * Convert the value to signed primitive.
	 *
	 * @param array	Field value.
	 * @return	The 2-complement expressed as int.
	 */
	private static BigInteger extendSign(byte[] array){
		if((array[0] & 0x80) != 0x00){
			array = leftExtendArray(array);
			array[0] = -1;
		}
		return new BigInteger(array);
	}

	/**
	 * Extends an array leaving room for one more byte at the leftmost index.
	 *
	 * @param array	The array to extend.
	 * @return	The extended array.
	 */
	private static byte[] leftExtendArray(final byte[] array){
		final byte[] extendedArray = new byte[array.length + 1];
		System.arraycopy(array, 0, extendedArray, 1, array.length);
		return extendedArray;
	}

}
