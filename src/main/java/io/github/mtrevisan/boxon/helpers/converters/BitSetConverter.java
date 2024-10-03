/*
 * Copyright (c) 2024 Mauro Trevisan
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
package io.github.mtrevisan.boxon.helpers.converters;

import java.math.BigInteger;
import java.util.BitSet;


/**
 * Defines methods for converting between BitSet and primitive/objective types.
 */
public interface BitSetConverter{

	/**
	 * Creates a {@link BitSet} with the given {@link BigInteger} value and `bitmapSize`.
	 *
	 * @param value	The {@link BigInteger} value used to initialize the {@link BitSet}.
	 * @param bitmapSize	The number of bits in the {@link BitSet}.
	 * @return	A new {@link BitSet} initialized with the given value and `bitmapSize`.
	 */
	BitSet createBitSet(BigInteger value, int bitmapSize);

	/**
	 * Converts a {@link BitSet} to a {@link BigInteger}.
	 *
	 * @param bitmap	The {@link BitSet} to convert.
	 * @param bitmapSize	The number of bits in the {@link BitSet}.
	 * @return	The converted {@link BigInteger}.
	 */
	BigInteger toObjectiveType(BitSet bitmap, int bitmapSize);


	static void fillBits(final BitSet bitmap, final byte currentByte, final int index, final int size){
		//iterate over the bits from left to right in the byte (most significant to least significant)
		for(int j = 0, k = index << 3; j < Byte.SIZE && k < size; j ++, k ++)
			if(((currentByte >> j) & 1) != 0)
				bitmap.set(k);
	}

//	static BigInteger toBigIntegerBigEndian2(final BitSet bitmap, final int bitmapSize){
//		BigInteger result = BigInteger.ZERO;
//		int i = -1;
//		while((i = bitmap.nextSetBit(i + 1)) >= 0)
//			result = result.setBit(calculateTrueIndex(i, bitmapSize));
//		return result;
//	}

//	private static int calculateTrueIndex(final int i, final int bitSize){
//		final int index = bitSize - 1 - i;
//		final int offsetByteIndex = index / Byte.SIZE + 1;
//		final int offsetBitIndex = index % Byte.SIZE + 1;
//		return offsetByteIndex * Byte.SIZE - offsetBitIndex;
//	}

//	static BigInteger toBigIntegerLittleEndian(final BitSet bitmap){
//		BigInteger result = BigInteger.ZERO;
//		int i = -1;
//		while((i = bitmap.nextSetBit(i + 1)) >= 0)
//			result = result.setBit(i);
//		return result;
//	}

	static BigInteger toBigIntegerBigEndian(final BitSet bitmap, final int bitmapSize){
		final int length = (bitmapSize + 7) / Byte.SIZE;
		final byte[] bytes = new byte[length];
		int i = -1;
		while((i = bitmap.nextSetBit(i + 1)) >= 0){
			final int index = bitmapSize - 1 - i;
			final int byteIndex = index / Byte.SIZE;
			final int bitIndex = index % Byte.SIZE;
			//in-place reverse byte array
			bytes[length - 1 - byteIndex] |= (byte)(1 << (Byte.SIZE - 1 - bitIndex));
		}
		return new BigInteger(1, bytes);
	}

	static BigInteger toBigIntegerLittleEndian(final BitSet bitmap){
		final long[] words = bitmap.toLongArray();
		final int length = words.length;
		final byte[] bytes = new byte[length * Long.BYTES];
		for(int i = 0; i < length; i ++){
			long word = words[i];

			final int offset = bytes.length - 1 - i * Long.BYTES;
			for(int j = 0; j < Long.BYTES; j ++){
				//in-place reverse byte array
				bytes[offset - j] = (byte)(word & 0xFF);
				word >>= 8;
			}
		}
		return new BigInteger(1, bytes);
	}

	static BigInteger negateValue(final BigInteger result, final int bitSize){
		final BigInteger mask = BigInteger.ONE
			.shiftLeft(bitSize)
			.subtract(BigInteger.ONE);
		return result.not()
			.add(BigInteger.ONE)
			.and(mask)
			.negate();
	}

}
