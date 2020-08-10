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
	 * @param array	Field value.
	 * @param unsigned	Whether to consider this number an unsigned one.
	 * @return	The 2-complement expressed as int.
	 */
	private static BigInteger extendSign(byte[] array, final int size, final boolean unsigned){
		if(!unsigned && (array[0] & 0x80) != 0x00){
			array = extendArray(array);
			array[0] = (byte)-1;
		}
		else if(unsigned && size >= array.length * Byte.SIZE)
			array = extendArray(array);
		return new BigInteger(array);
	}

	static byte[] extendArray(final byte[] array){
		final byte[] extendedArray = new byte[array.length + 1];
		System.arraycopy(array, 0, extendedArray, 1, array.length);
		return extendedArray;
	}

	public static boolean hasBit(final byte mask, final int index){
		if(index < 0 || index >= Byte.SIZE)
			throw new IllegalArgumentException("Index value must be between 0 and " + Byte.SIZE + " exclusive, was " + index);

		final int bitMask = 1 << (index % Byte.SIZE);
		return ((mask & bitMask) != 0);
	}

	public static boolean hasBit(final byte[] mask, final int index){
		if(index < 0 || index >= mask.length * Byte.SIZE)
			throw new IllegalArgumentException("Index value must be between 0 and " + (mask.length * Byte.SIZE) + " exclusive, was " + index);

		final int bitGroup = mask.length - 1 - index / Byte.SIZE;
		final int bitMask = 1 << (index % Byte.SIZE);
		return ((mask[bitGroup] & bitMask) != 0);
	}

}
