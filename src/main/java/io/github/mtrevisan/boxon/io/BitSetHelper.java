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

import java.math.BigInteger;
import java.util.Arrays;
import java.util.BitSet;


/**
 * A collection of convenience methods for working with {@link BitSet} objects.
 */
public final class BitSetHelper{

	private BitSetHelper(){}


	public static BitSet createBitSet(long value, final int size){
		final BitSet bits = new BitSet(size);
		while(value != 0){
			final int nextSetBitIndex = Long.numberOfTrailingZeros(value);
			if(nextSetBitIndex == Long.SIZE)
				break;

			bits.set(nextSetBitIndex);

			//reset bit
			value &= ~(1l << nextSetBitIndex);
		}
		return bits;
	}

	/**
	 * Converts a {@link BigInteger} into a bit set ignoring the sign of the {@link BigInteger}, according to SRP specification.
	 *
	 * @param value	The value, must not be {@code null}.
	 * @param size	The size in bits of the value.
	 * @param byteOrder	The type of endianness: either {@link ByteOrder#LITTLE_ENDIAN} or {@link ByteOrder#BIG_ENDIAN}.
	 * @return	The bit set representing the given value.
	 */
	public static BitSet createBitSet(final BigInteger value, final int size, final ByteOrder byteOrder){
		final byte[] array = value.toByteArray();

		if(byteOrder == ByteOrder.LITTLE_ENDIAN)
			//NOTE: need to reverse the bytes because {@link BigInteger} is big-endian and {@link BitSet} is little-endian
			BitSetHelper.changeByteOrder(array, byteOrder);
		else if((size + Byte.SIZE - 1) >>> 3 != array.length)
			return createBitSet(array, size);

		return BitSet.valueOf(array);
	}

	private static BitSet createBitSet(final byte[] array, final int size){
		final BitSet bits = new BitSet(size);
		int bitIndex = 0;
		int bitSetIndex = Byte.SIZE;
		//transfer bits one by one from the most significant byte to the {@link BitSet}
		for(int i = 0, length = array.length; i < length && bitIndex < size; i ++){
			final byte currentByte = array[i];

			//iterate over the bits from left to right in the byte (most significant to least significant)
			for(int j = 0; j < Byte.SIZE && bitIndex < size; j ++, bitIndex ++){
				final boolean bitValue = (((currentByte >> j) & 1) == 1);
				bits.set(bitSetIndex ++, bitValue);
			}
		}
		return bits;
	}

	/**
	 * Convert this bit set to {@link BigInteger}.
	 *
	 * @param bits	The bit set.
	 * @param size	The number of bits.
	 * @param byteOrder	The byte order.
	 * @return	The converted {@link BigInteger}.
	 */
	static BigInteger toBigInteger(final BitSet bits, final int size, final ByteOrder byteOrder){
		byte[] array = bits.toByteArray();
		final int expectedLength = size >>> 3;
		if(array.length < expectedLength)
			array = Arrays.copyOf(array, expectedLength);

		//NOTE: need to reverse the bytes because `BigInteger` is big-endian and `BitSet` is little-endian
		BitSetHelper.changeByteOrder(array, byteOrder);

		return new BigInteger(extendSign(array));
	}

	/**
	 * Convert the value to signed primitive.
	 *
	 * @param array	Field value.
	 * @return	The 2-complement expressed as int.
	 */
	private static byte[] extendSign(byte[] array){
		if((array[0] & 0x80) != 0x00){
			array = leftExtendArray(array);
			array[0] = -1;
		}
		return array;
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


	/**
	 * In-place reverse the order of the given array byte-by-byte.
	 *
	 * @param array	The array to be reversed.
	 * @param byteOrder	The byte order.
	 */
	public static void changeByteOrder(final byte[] array, final ByteOrder byteOrder){
		if(byteOrder == ByteOrder.LITTLE_ENDIAN)
			byteReverse(array);
	}

	/**
	 * Change the byte order appropriately.
	 *
	 * @param bits	The bit set.
	 * @param bitOrder	The bit order.
	 * @return	The bit set with the bits reversed if the bit order is little-endian.
	 */
	public static BitSet changeBitOrder(final BitSet bits, final ByteOrder bitOrder){
		return (bitOrder == ByteOrder.LITTLE_ENDIAN? bitReverse(bits): bits);
	}

	/**
	 * Reverse the endianness bit by bit.
	 *
	 * @param bits	The bit set.
	 * @return	The {@link BitSet} with the bits reversed.
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
		for(int i = 0, length = array.length; i < length; i ++)
			array[i] = reverseBits(array[i]);
		byteReverse(array);
	}

	/**
	 * Reverse the endianness bit by bit.
	 *
	 * @param number	The byte to be reversed.
	 * @return	The given number with the bits reversed.
	 */
	private static byte reverseBits(byte number){
		byte reverse = 0;
		for(int i = Byte.SIZE - 1; i >= 0; i --){
			reverse += (byte)((number & 1) << i);
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
			//swap `array[start]` with `array[end]`
			array[start] ^= (byte)(array[end] ^ (array[end] = array[start]));
	}

}
