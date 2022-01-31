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

import java.math.BigInteger;
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

		//NOTE: need to reverse the bytes because BigInteger is big-endian and BitSet is little-endian
		changeByteOrder(array, byteOrder);

		return BitSet.valueOf(array);
	}


	/**
	 * In-place reverse the order of the given array byte-by-byte.
	 *
	 * @param array	The array to be reversed.
	 * @param byteOrder	The byte order.
	 */
	static void changeByteOrder(final byte[] array, final ByteOrder byteOrder){
		if(byteOrder == ByteOrder.LITTLE_ENDIAN)
			byteReverse(array);
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

}
