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
package io.github.mtrevisan.boxon.helpers;

import io.github.mtrevisan.boxon.io.ByteOrder;

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
	 * @param bitmapSize	The size of the resulting {@link BitSet}.
	 * @return	The created {@link BitSet}.
	 */
	public static BitSet createBitSet(final int bitmapSize){
		return new BitSet(bitmapSize);
	}

	/**
	 * Creates a {@link BitSet} from a long value.
	 *
	 * @param bitmapSize  The size in bits of the resulting {@link BitSet}.
	 * @param value The value to convert to {@link BitSet}.
	 * @return The created {@link BitSet}.
	 */
	public static BitSet createBitSet(final int bitmapSize, long value){
		final BitSet bitmap = createBitSet(bitmapSize);

		while(value != 0){
			final int nextSetBitIndex = Long.numberOfTrailingZeros(value);
			bitmap.set(nextSetBitIndex);

			//reset bit
			value &= ~(1l << nextSetBitIndex);
		}

		return bitmap;
	}

	/**
	 * Converts a {@link BigInteger} into a {@link BitSet} ignoring the sign of the {@link BigInteger}, according to SRP specification.
	 *
	 * @param bitmapSize      The size in bits of the value.
	 * @param value     The value, must not be {@code null}.
	 * @param byteOrder The type of endianness: either {@link ByteOrder#LITTLE_ENDIAN} or {@link ByteOrder#BIG_ENDIAN}.
	 * @return The {@link BitSet} representing the given value.
	 */
	public static BitSet createBitSet(final int bitmapSize, final BigInteger value, final ByteOrder byteOrder){
		return getConverter(byteOrder)
			.createBitSet(bitmapSize, value);
	}


	/**
	 * Convert this {@link BitSet} to a short.
	 * <p>This method assumes the higher bit set in the bit set is at most at index `bitmapSize - 1`.</p>
	 *
	 * @param bitmap	The {@link BitSet}.
	 * @param bitmapSize	The number of bits.
	 * @param byteOrder	The byte order.
	 * @return	The converted short.
	 */
	public static long toPrimitiveType(final BitSet bitmap, final int bitmapSize, final ByteOrder byteOrder){
		return getConverter(byteOrder)
			.toPrimitiveType(bitmap, bitmapSize);
	}

	/**
	 * Convert this {@link BitSet} to {@link BigInteger}.
	 *
	 * @param bitmap	The {@link BitSet}.
	 * @param bitmapSize	The number of bits.
	 * @param byteOrder	The byte order.
	 * @return	The converted {@link BigInteger}.
	 */
	public static BigInteger toObjectiveType(final BitSet bitmap, final int bitmapSize, final ByteOrder byteOrder){
		return getConverter(byteOrder)
			.toObjectiveType(bitmap, bitmapSize);
	}


	private static BitSetConverter getConverter(final ByteOrder byteOrder){
		return CONVERTER.get(byteOrder);
	}

}
