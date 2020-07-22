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
package io.github.mtrevisan.boxon.helpers;

import io.github.mtrevisan.boxon.annotations.ByteOrder;

import java.math.BigInteger;
import java.util.Arrays;


/**
 * @see <a href="https://git.irsamc.ups-tlse.fr/scemama/Bit-Twiddling-Hacks/">Bit Twiddling Hacks</a>
 * @see <a href="https://graphics.stanford.edu/~seander/bithacks.html#ConditionalSetOrClearBitsWithoutBranching">Bit Twiddling Hacks</a>
 */
public final class ByteHelper{

	private ByteHelper(){}


	/**
	 * Converts an array of bytes into a string representing the hexadecimal values of each byte in order
	 *
	 * @param array	Array to be converted to hexadecimal characters
	 * @return	The hexadecimal characters
	 */
	public static String toHexString(final byte[] array){
		final StringBuffer sb = new StringBuffer((array != null? array.length << 1: 0));
		if(array != null)
			for(final byte b : array){
				sb.append(Character.forDigit((b >>> 4) & 0x0F, 16));
				sb.append(Character.forDigit((b & 0x0F), 16));
			}
		return sb.toString().toUpperCase();
	}

	/**
	 * Converts a string representing the hexadecimal values of each byte to an array of bytes in order
	 *
	 * @param hexString	The hexadecimal string
	 * @return	Array of converted hexadecimal characters
	 */
	public static byte[] toByteArray(final String hexString){
		final int len = (hexString != null? hexString.length(): 0);
		if(len % 2 != 0)
			throw new IllegalArgumentException("Malformed input");

		final byte[] data = new byte[len / 2];
		for(int i = 0; i < len; i += 2)
			data[i / 2] = (byte)((Character.digit(hexString.charAt(i), 16) << 4) + Character.digit(hexString.charAt(i + 1), 16));
		return data;
	}

	public static long reverseBytes(final long value, final int size, final ByteOrder byteOrder){
		return (byteOrder == ByteOrder.BIG_ENDIAN? (Long.reverseBytes(value) >> (Long.SIZE - size)): value);
	}

	public static long bitsToLong(final BitSet bits, final int size, final ByteOrder byteOrder){
		final long value = bits.toLong(0, size);
		return (byteOrder == ByteOrder.BIG_ENDIAN? Long.reverseBytes(value) >>> (Long.SIZE - size): value);
	}

	public static BigInteger toInteger(final BitSet bits, final int size, final ByteOrder byteOrder, final boolean unsigned){
		byte[] array = bits.toByteArray();
		final int expectedLength = size / Byte.SIZE;
		if(array.length < expectedLength)
			array = Arrays.copyOf(array, expectedLength);
		if(byteOrder == ByteOrder.LITTLE_ENDIAN)
			//NOTE: need to reverse the bytes because BigInteger is big-endian and BitMap is little-endian
			reverse(array);
		return extendSign(array, size, unsigned);
	}

	/**
	 * Converts a BigInteger into a byte array ignoring the sign of the BigInteger, according to SRP specification
	 *
	 * @param value	the value, must not be <code>null</code>.
	 * @param size	The size in bits of the value.
	 * @param byteOrder	The type of endianness: either {@link ByteOrder#LITTLE_ENDIAN} or {@link ByteOrder#BIG_ENDIAN}.
	 * @return	The {@link BitSet} representing the given value.
	 */
	public static BitSet toBits(final BigInteger value, final int size, final ByteOrder byteOrder){
		byte[] array = value.toByteArray();
		final int newSize = (size + Byte.SIZE - 1) / Byte.SIZE;
		if(newSize != array.length){
			final int offset = Math.max(array.length - newSize, 0);
			final byte[] newArray = new byte[newSize];
			final int newArrayOffset = Math.max(newArray.length - array.length, 0);
			System.arraycopy(array, offset, newArray, newArrayOffset, array.length - offset);
			array = newArray;
		}
		if(byteOrder == ByteOrder.LITTLE_ENDIAN)
			//NOTE: need to reverse the bytes because BigInteger is big-endian and BitMap is little-endian
			reverse(array);
		return BitSet.valueOf(array);
	}

	/**
	 * Reverses the order of the given array.
	 *
	 * @param array	The array to reverse
	 */
	private static void reverse(final byte[] array){
		for(int start = 0, end = array.length - 1; start < end; start ++, end --){
			//swap array[start] with array[end]
			array[start] ^= array[end];
			array[end] ^= array[start];
			array[start] ^= array[end];
		}
	}

	/**
	 * Reverses the order of the given array.
	 *
	 * @param array	The array to reverse
	 */
	public static void reverse(final int[] array){
		for(int start = 0, end = array.length - 1; start < end; start ++, end --){
			//swap array[start] with array[end]
			array[start] ^= array[end];
			array[end] ^= array[start];
			array[start] ^= array[end];
		}
	}


	/**
	 * Convert the value to signed primitive
	 *
	 * @param value	Field value
	 * @param size	Length in bits of the field
	 * @return	The 2-complement expressed as int
	 */
	public static long extendSign(final long value, final int size){
		if(size <= 0)
			throw new IllegalArgumentException("Size must be a positive value, was " + size);

		final int shift = -size;
		return (value << shift) >> shift;
	}

	/**
	 * Convert the value to signed primitive
	 *
	 * @param array	Field value
	 * @param unsigned	Whether to consider this number an unsigned one
	 * @return	The 2-complement expressed as int
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

	private static byte[] extendArray(final byte[] array){
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
