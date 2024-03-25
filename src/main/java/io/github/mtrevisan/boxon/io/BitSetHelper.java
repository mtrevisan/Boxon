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
		//FIXME avoid array creation
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
		boolean negative;
		BigInteger result = BigInteger.ZERO;
		if(byteOrder == ByteOrder.BIG_ENDIAN){
			negative = bits.get(7);
			for(int i = bits.nextSetBit(0); i >= 0; i = bits.nextSetBit(i + 1)){
				final int index = size - 1 - i;
				final int byteIndex = index / Byte.SIZE + 1;
				final int bitIndex = index % Byte.SIZE + 1;
				result = result.setBit(byteIndex * Byte.SIZE - bitIndex);
			}
		}
		else{
			negative = bits.get(size - 1);
			for(int i = bits.nextSetBit(0); i >= 0; i = bits.nextSetBit(i + 1))
				result = result.setBit(i);
		}

		if(negative){
			final BigInteger mask = BigInteger.ONE
				.shiftLeft(size)
				.subtract(BigInteger.ONE);
			result = result.not()
				.add(BigInteger.ONE)
				.and(mask)
				.negate();
		}

		return result;
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
	 * @param size	Length in bytes of the entire bit set.
	 * @param byteOrder	The byte order.
	 * @return	The bit set with the bytes reversed if the byte order is little-endian.
	 */
	public static BitSet changeBitOrder(final BitSet bits, final int size, final ByteOrder byteOrder){
		return (byteOrder == ByteOrder.LITTLE_ENDIAN? bitReverseEndianness(bits, size): bits);
	}

	/**
	 * Reverse the endianness bit by bit.
	 *
	 * @param bits	The bit set.
	 * @param size	Length in bytes of the entire bit set.
	 * @return	The {@link BitSet} with the bits reversed.
	 */
	private static BitSet bitReverseEndianness(final BitSet bits, final int size){
		//FIXME avoid array creation
		final byte[] array = bits.toByteArray();
		reverseEndianness(array, size);
		return BitSet.valueOf(array);
	}

	private static BitSet bitReverseEndianness2(final BitSet bits, final int size){
		final BitSet reversedBits = new BitSet(size);
		for(int i = bits.nextSetBit(0); i >= 0; i = bits.nextSetBit(i + 1)){
			if(bits.get(i)){
				final int index = size - 1 - i;
				final int byteIndex = index / Byte.SIZE + 1;
				final int bitIndex = index % Byte.SIZE + 1;
				reversedBits.set(byteIndex * Byte.SIZE - bitIndex);
			}
//			reversedBits.set(length - 1 - i);
		}
		return reversedBits;
	}

	/**
	 * Reverse the endianness byte by byte.
	 *
	 * @param array	The array to be reversed.
	 */
	private static void reverseEndianness(final byte[] array, final int size){
		byte temp;
		for(int i = 0, length = Math.min(array.length, size) >> 1; i < length; i ++){
			temp = array[i];
			array[i] = array[length - 1 - i];
			array[length - 1 - i] = temp;
		}
	}

	/**
	 * Reverse bit by bit.
	 *
	 * @param array	The array to be reversed.
	 */
	private static void bitReverse(final byte[] array){
		for(int i = 0, length = array.length; i < length; i ++)
			array[i] = reverseBits(array[i]);
		byteReverse(array);
	}

	/**
	 * Reverse the number bit by bit.
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
