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
package io.github.mtrevisan.boxon.helpers;

import java.math.BigInteger;
import java.util.BitSet;


/**
 * Defines methods for converting between BitSet and primitive/objective types.
 */
interface BitSetConverter{

	/**
	 * Creates a {@link BitSet} with the given {@link BigInteger} value and `bitSize`.
	 *
	 * @param value	The {@link BigInteger} value used to initialize the {@link BitSet}.
	 * @param bitSize	The number of bits in the {@link BitSet}.
	 * @return	A new {@link BitSet} initialized with the given value and `bitSize`.
	 */
	BitSet createBitSet(BigInteger value, int bitSize);

	/**
	 * Converts a {@link BitSet} to a primitive value of type long.
	 *
	 * @param bits	The {@link BitSet} to convert.
	 * @param bitSize	The number of bits in the {@link BitSet}.
	 * @return	The converted primitive value.
	 */
	long toPrimitiveType(BitSet bits, int bitSize);

	/**
	 * Converts a {@link BitSet} to a {@link BigInteger}.
	 *
	 * @param bits	The {@link BitSet} to convert.
	 * @param bitSize	The number of bits in the {@link BitSet}.
	 * @return	The converted {@link BigInteger}.
	 */
	BigInteger toObjectiveType(BitSet bits, int bitSize);


	static void fillBits(final BitSet bits, final byte currentByte, final int index, final int size){
		//iterate over the bits from left to right in the byte (most significant to least significant)
		for(int j = 0, k = index << 3; j < Byte.SIZE && k < size; j ++, k ++)
			if(((currentByte >> j) & 1) != 0)
				bits.set(k);
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
