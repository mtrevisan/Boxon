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
import java.util.EnumMap;
import java.util.Map;


/**
 * A collection of convenience methods for working with {@link BitSet} objects.
 */
public final class BitSetHelper{

	private static final Map<ByteOrder, BitSetConverter> CONVERTER = new EnumMap<>(ByteOrder.class);
	static{
		CONVERTER.put(ByteOrder.BIG_ENDIAN, new BigEndianConverter());
		CONVERTER.put(ByteOrder.LITTLE_ENDIAN, new LittleEndianConverter());
	}


	private BitSetHelper(){}


	/**
	 * Creates a new {@link BitSet} with the specified size.
	 *
	 * @param size	The size of the resulting {@link BitSet}.
	 * @return	The created {@link BitSet}.
	 */
	public static BitSet createBitSet(final int size){
		return new BitSet(size);
	}

	/**
	 * Creates a {@link BitSet} from a long value.
	 *
	 * @param value	The value to convert to {@link BitSet}.
	 * @param size	The size in bits of the resulting {@link BitSet}.
	 * @return	The created {@link BitSet}.
	 */
	public static BitSet createBitSet(long value, final int size){
		final BitSet bits = createBitSet(size);

		while(value != 0){
			final int nextSetBitIndex = Long.numberOfTrailingZeros(value);
			bits.set(nextSetBitIndex);

			//reset bit
			value &= ~(1l << nextSetBitIndex);
		}

		return bits;
	}

	/**
	 * Converts a {@link BigInteger} into a {@link BitSet} ignoring the sign of the {@link BigInteger}, according to SRP specification.
	 *
	 * @param value	The value, must not be {@code null}.
	 * @param size	The size in bits of the value.
	 * @param byteOrder	The type of endianness: either {@link ByteOrder#LITTLE_ENDIAN} or {@link ByteOrder#BIG_ENDIAN}.
	 * @return	The {@link BitSet} representing the given value.
	 */
	public static BitSet createBitSet(final BigInteger value, final int size, final ByteOrder byteOrder){
		final BitSet bits = createBitSet(size);

		final boolean littleEndian = (byteOrder == ByteOrder.LITTLE_ENDIAN);
		//transfer bits one by one from the most significant byte to the {@link BitSet}
		for(int i = 0, length = (size + Byte.SIZE - 1) / Byte.SIZE; i < length; i ++){
			final int byteIndex = (littleEndian? i: length - 1 - i);
			final byte currentByte = value.shiftRight(byteIndex << 3)
				.byteValue();

			fillBits(bits, currentByte, i, size);
		}

		return bits;
	}

	private static void fillBits(final BitSet bits, final byte currentByte, final int index, final int size){
		//iterate over the bits from left to right in the byte (most significant to least significant)
		for(int j = 0, k = index << 3; j < Byte.SIZE && k < size; j ++, k ++)
			if(((currentByte >> j) & 1) != 0)
				bits.set(k);
	}


	/**
	 * Convert this {@link BitSet} to a short.
	 * <p>This method assumes the higher bit set in the bit set is at most at index `bitSize - 1`.</p>
	 *
	 * @param bits	The {@link BitSet}.
	 * @param bitSize	The number of bits.
	 * @param byteOrder	The byte order.
	 * @return	The converted short.
	 */
	static long toPrimitiveType(final BitSet bits, final int bitSize, final ByteOrder byteOrder){
		return getConverter(byteOrder)
			.toPrimitiveType(bits, bitSize);
	}

	/**
	 * Convert this {@link BitSet} to {@link BigInteger}.
	 *
	 * @param bits	The {@link BitSet}.
	 * @param bitSize	The number of bits.
	 * @param byteOrder	The byte order.
	 * @return	The converted {@link BigInteger}.
	 */
	public static BigInteger toObjectiveType(final BitSet bits, final int bitSize, final ByteOrder byteOrder){
		return getConverter(byteOrder)
			.toObjectiveType(bits, bitSize);
	}

	private static BitSetConverter getConverter(final ByteOrder byteOrder){
		return CONVERTER.get(byteOrder);
	}

}
