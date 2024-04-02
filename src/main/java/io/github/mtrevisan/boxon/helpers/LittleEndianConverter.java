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


class LittleEndianConverter implements BitSetConverter{

	@Override
	public BitSet createBitSet(final BigInteger value, final int bitSize){
		final BitSet bits = BitSetHelper.createBitSet(bitSize);
		//transfer bits one by one from the most significant byte to the {@link BitSet}
		for(int i = 0, length = (bitSize + Byte.SIZE - 1) / Byte.SIZE; i < length; i ++){
			final byte currentByte = value.shiftRight(i << 3)
				.byteValue();

			BitSetConverter.fillBits(bits, currentByte, i, bitSize);
		}
		return bits;
	}

	@Override
	public long toPrimitiveType(final BitSet bits, final int bitSize){
		long result = 0l;
		int i = -1;
		while((i = bits.nextSetBit(i + 1)) >= 0)
			result |= (1l << i);
		return result;
	}

	@Override
	public BigInteger toObjectiveType(final BitSet bits, final int bitSize){
		final boolean negative = bits.get(bitSize - 1);
		final BigInteger result = toBigInteger(bits);
		return (negative? BitSetConverter.negateValue(result, bitSize): result);
	}

	private static BigInteger toBigInteger(final BitSet bits){
		BigInteger result = BigInteger.ZERO;
		int i = -1;
		while((i = bits.nextSetBit(i + 1)) >= 0)
			result = result.setBit(i);
		return result;
	}

}
