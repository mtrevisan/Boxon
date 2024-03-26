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
		final boolean littleEndian = (byteOrder == ByteOrder.LITTLE_ENDIAN);
		final BitSet bits = new BitSet(size);
		//transfer bits one by one from the most significant byte to the {@link BitSet}
		for(int i = 0, length = (size + Byte.SIZE - 1) / Byte.SIZE; i < length; i ++){
			final int byteIndex = (littleEndian? i: length - 1 - i);
			final byte currentByte = value.shiftRight(byteIndex << 3)
				.byteValue();

			//iterate over the bits from left to right in the byte (most significant to least significant)
			for(int j = 0, k = i << 3; j < Byte.SIZE && k < size; j ++, k ++)
				if(((currentByte >> j) & 1) == 1)
					bits.set(k);
		}
		return bits;
	}

	/**
	 * Convert this bit set to {@link BigInteger}.
	 *
	 * @param bits	The bit set.
	 * @param bitSize	The number of bits.
	 * @param byteOrder	The byte order.
	 * @return	The converted {@link BigInteger}.
	 */
	static BigInteger toBigInteger(final BitSet bits, final int bitSize, final ByteOrder byteOrder){
		final boolean negative;
		BigInteger result;
		if(byteOrder == ByteOrder.BIG_ENDIAN){
			negative = bits.get(7);
			result = toBigIntegerBigEndian(bits, bitSize);
		}
		else{
			negative = bits.get(bitSize - 1);
			result = toBigIntegerLittleEndian(bits);
		}

		if(negative){
			final BigInteger mask = BigInteger.ONE
				.shiftLeft(bitSize)
				.subtract(BigInteger.ONE);
			result = result.not()
				.add(BigInteger.ONE)
				.and(mask)
				.negate();
		}

		return result;
	}

	private static BigInteger toBigIntegerBigEndian(final BitSet bits, final int bitSize){
		BigInteger result = BigInteger.ZERO;
		for(int i = bits.nextSetBit(0); i >= 0; i = bits.nextSetBit(i + 1)){
			final int index = bitSize - 1 - i;
			final int byteIndex = index / Byte.SIZE + 1;
			final int bitIndex = index % Byte.SIZE + 1;
			result = result.setBit(byteIndex * Byte.SIZE - bitIndex);
		}
		return result;
	}

	private static BigInteger toBigIntegerLittleEndian(final BitSet bits){
		BigInteger result = BigInteger.ZERO;
		for(int i = bits.nextSetBit(0); i >= 0; i = bits.nextSetBit(i + 1))
			result = result.setBit(i);
		return result;
	}

}
