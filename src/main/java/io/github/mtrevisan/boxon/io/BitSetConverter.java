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
package io.github.mtrevisan.boxon.io;

import java.math.BigInteger;
import java.util.BitSet;


/**
 * Defines methods for converting between BitSet and primitive/objective types.
 */
interface BitSetConverter{

	/**
	 * Creates a {@link BitSet} with the given {@link BigInteger} value and `bitmapSize`.
	 *
	 * @param bitmapSize	The number of bits in the {@link BitSet}.
	 * @param value	The {@link BigInteger} value used to initialize the {@link BitSet}.
	 * @return	A new {@link BitSet} initialized with the given value and `bitmapSize`.
	 */
	BitSet createBitSet(int bitmapSize, BigInteger value);

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
