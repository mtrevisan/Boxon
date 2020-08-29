/**
 * Copyright (c) 2020 Mauro Trevisan
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
package io.github.mtrevisan.boxon.external;

import java.math.BigInteger;


/**
 * @see <a href="https://git.irsamc.ups-tlse.fr/scemama/Bit-Twiddling-Hacks/">Bit Twiddling Hacks</a>
 */
public final class ByteHelper{

	private ByteHelper(){}

	/**
	 * Convert the value to signed primitive.
	 *
	 * @param value	Field value.
	 * @param size	Length in bits of the field.
	 * @return	The 2-complement expressed as int.
	 */
	@SuppressWarnings("ShiftOutOfRange")
	public static long extendSign(final long value, final int size){
		if(size <= 0)
			throw new IllegalArgumentException("Size must be a positive value, was " + size);

		final int shift = -size;
		return (value << shift) >> shift;
	}

	/**
	 * Convert the value to signed primitive.
	 *
	 * @param array   Field value.
	 * @return	The 2-complement expressed as int.
	 */
	static BigInteger extendSign(byte[] array){
		if((array[0] & 0x80) != 0x00){
			array = leftExtendArray(array);
			array[0] = -1;
		}
		return new BigInteger(array);
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
	 * Checks whether the given `mask` has the bit at `index` set.
	 *
	 * @param mask	The value to check the bit into.
	 * @param index	The index of the bit (rightmost is zero). The value can range between {@code 0} and {@link Byte#SIZE}.
	 * @return	The state of the bit at a given index in the given byte.
	 */
	public static boolean hasBit(final byte mask, final int index){
		assertLength(index, Byte.SIZE);

		final int bitMask = 1 << (index % Byte.SIZE);
		return ((mask & bitMask) != 0);
	}

	/**
	 * Checks whether the given `mask` has the bit at `index` set.
	 *
	 * @param mask	The value (as an array) to check the bit into.
	 * @param index	The index of the bit (rightmost is zero). The value can range between {@code 0} and
	 * 	<code>mask.length * {@link Byte#SIZE}</code>.
	 * @return	The state of the bit at a given index in the given byte.
	 */
	public static boolean hasBit(final byte[] mask, final int index){
		assertLength(index, mask.length << 3);

		final int bitGroup = mask.length - 1 - (index >>> 3);
		final int bitMask = 1 << (index % Byte.SIZE);
		return ((mask[bitGroup] & bitMask) != 0);
	}

	private static void assertLength(final int index, final int maxLength){
		if(index < 0 || index >= maxLength)
			throw new IllegalArgumentException("Index value must be between 0 and " + maxLength + " exclusive, was " + index);
	}

}
