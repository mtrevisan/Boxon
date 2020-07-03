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
package unit731.boxon.helpers;

import unit731.boxon.annotations.ByteOrder;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.BitSet;


/**
 * @see <a href="https://graphics.stanford.edu/~seander/bithacks.html#ConditionalSetOrClearBitsWithoutBranching">Bit Twiddling Hacks</a>
 * @see <a href="https://git.irsamc.ups-tlse.fr/scemama/Bit-Twiddling-Hacks/">Bit Twiddling Hacks</a>
 */
public class ByteHelper{

	private ByteHelper(){}

	public static boolean hasBit(final byte mask, final int index){
		if(index < 0 || index >= Byte.SIZE)
			throw new IllegalArgumentException("Index value must be between 0 and " + (Byte.SIZE - 1) + " inclusive, was " + index);

		return ((mask & (1 << (index % Byte.SIZE))) != 0);
	}


	/**
	 * Returns the starting position of the first occurrence of the specified pattern array within the specified source array,
	 * or {@code -1} if there is no such occurrence.
	 * More formally, returns the lowest index `i` such that {@code source.subArray(i, i + pattern.size()).equals(pattern)},
	 * or {@code -1} if there is no such index.<br>
	 * (Returns {@code -1} if {@code pattern.size() > source.size()})
	 * <p>
	 * This implementation uses the "brute force" technique of scanning over the source list, looking for a match with the pattern
	 * at each location in turn
	 *
	 * @param source	The list in which to search for the first occurrence of {@code pattern}.
	 * @param pattern	The list to search for as a subList of {@code source}.
	 * @param offset	Offset to start the search from.
	 * @param lps	LPS array precomputed by {@link #indexOfComputeLPS(byte[])}
	 * @return	The starting position of the first occurrence of the specified pattern list within the specified source list,
	 * 	or {@code -1} if there is no such occurrence.
	 */
	public static int indexOf(final byte[] source, final byte[] pattern, final int offset, final int[] lps){
		//no candidate matched the pattern
		int index = -1;

		//current char in target string
		int targetPointer = 0;
		//current char in search string
		int searchPointer = offset;
		//while there is more to search with, keep searching
		while(searchPointer < source.length){
			if(source[searchPointer] == pattern[targetPointer]){
				//found current char in targetPointer in search string
				targetPointer ++;
				if(targetPointer == pattern.length){
					//return starting index of found target inside searched string
					index = searchPointer - targetPointer + 1;
					break;
				}

				//move forward if not found target string
				searchPointer ++;
			}
			else if(targetPointer > 0)
				//use failureTable to use pointer pointed at nearest location of usable string prefix
				targetPointer = lps[targetPointer - 1];
			else
				//targetPointer is pointing at state 0, so restart search with current searchPointer index
				searchPointer ++;
		}
		return index;
	}

	/**
	 * Returns an array that points to last valid string prefix
	 *
	 * @param pattern	The list to search for as a subList of {@code source}.
	 * @return	The array of LPS
	 */
	public static int[] indexOfComputeLPS(final byte[] pattern){
		final int[] lps = new int[pattern.length];
		lps[0] = 0;

		int i = 1;
		//length of the previous longest prefix suffix
		int lengthPreviousLPS = 0;
		//the loop calculates lps[i] for i = 1 to m-1
		while(i < pattern.length){
			if(pattern[i] == pattern[lengthPreviousLPS])
				lps[i ++] = ++ lengthPreviousLPS;
			//if lengthPreviousLPS isn't at the very beginning, then send lengthPreviousLPS backward by following the already set pointer to where it is pointing to
			else if(lengthPreviousLPS > 0)
				lengthPreviousLPS = lps[lengthPreviousLPS - 1];
			//lengthPreviousLPS has fallen all the way back to the beginning
			else
				lps[i ++] = lengthPreviousLPS;
		}
		return lps;
	}


	/**
	 * Converts an array of bytes into a string representing the hexadecimal values of each byte in order
	 *
	 * @param array	Array to be converted to hexadecimal characters
	 * @return	The hexadecimal characters
	 */
	public static String byteArrayToHexString(final byte[] array){
		final StringBuffer sb = new StringBuffer(array.length << 1);
		for(final byte b : array){
			sb.append(Character.forDigit((b >> 4) & 0x0F, 16));
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
	public static byte[] hexStringToByteArray(final String hexString){
		final int len = (hexString != null? hexString.length(): 0);
		if(len % 2 != 0)
			throw new IllegalArgumentException("Malformed input");

		final byte[] data = new byte[len / 2];
		for(int i = 0; i < len; i += 2)
			data[i / 2] = (byte)((Character.digit(hexString.charAt(i), 16) << 4) + Character.digit(hexString.charAt(i + 1), 16));
		return data;
	}


	/**
	 * Apply mask and shift right (<code>maskByte(27, 0x18) = 3</code>)
	 *
	 * @param value	The value to which to apply the mask and the right shift
	 * @param size	Size in bits of the given value
	 * @param mask	The mask
	 * @return	The masked and shifter value
	 */
	public static long applyMaskAndShift(long value, final int size, long mask){
		final int ctz = Long.numberOfTrailingZeros(mask);
		value = ((value & mask) >> ctz);
		mask = (((1 << size) - 1) >>> ctz);
		return (value & mask);
	}

	/**
	 * Convert the value to signed primitive
	 *
	 * @param value	Field value
	 * @param size	Length in bits of the field
	 * @return	The 2-complement expressed as int
	 */
	public static int extendSign(final int value, final int size){
		final int shift = Integer.SIZE - size;
		return (value << shift) >> shift;
	}

	public static void reverseBits(final BitSet input, final int size){
		for(int i = 0; i < size / 2; i ++){
			final boolean t = input.get(i);
			input.set(i, input.get(size - i - 1));
			input.set(size - i - 1, t);
		}
	}

	public static long reverseBytes(final long value, final int size, final ByteOrder byteOrder){
		return (byteOrder == ByteOrder.BIG_ENDIAN? (Long.reverseBytes(value) >> (Long.SIZE - size)): value);
	}


	public static long bitsToLong(final BitSet bits, final int size, final ByteOrder byteOrder){
		long value = bits.toLongArray()[0];
		return (byteOrder == ByteOrder.BIG_ENDIAN? Long.reverseBytes(value) >>> (Long.SIZE - size): value);
	}

	public static BigInteger bitsToInteger(final BitSet bits, final int size, final ByteOrder byteOrder){
		byte[] array = bits.toByteArray();
		final int expectedLength = size / Byte.SIZE;
		if(array.length < expectedLength)
			array = Arrays.copyOf(array, expectedLength);
		if(byteOrder == ByteOrder.LITTLE_ENDIAN)
			//NOTE: need to reverse the bytes because BigInteger is big-endian and BitSet is little-endian
			reverse(array);

		if(size >= array.length * Byte.SIZE){
			final byte[] extendedArray = new byte[array.length + 1];
			System.arraycopy(array, 0, extendedArray, 1, array.length);
			array = extendedArray;
		}
		return new BigInteger(array);
	}

	/**
	 * Converts a BigInteger into a byte array ignoring the sign of the BigInteger, according to SRP specification
	 *
	 * @param value	the value, must not be <code>null</code>
	 * @param size	The size in bits of the `value`
	 * @param byteOrder	The type of endianness: either {@link ByteOrder#LITTLE_ENDIAN} or {@link ByteOrder#BIG_ENDIAN}.
	 * @return	The {@link BitSet} representing the given value.
	 */
	public static BitSet integerToBits(final BigInteger value, final int size, final ByteOrder byteOrder){
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
			//NOTE: need to reverse the bytes because BigInteger is big-endian and BitSet is little-endian
			reverse(array);
		return BitSet.valueOf(array);
	}

	/**
	 * Reverses the order of the given array.
	 *
	 * @param array	The array to reverse, may be {@code null}
	 */
	private static void reverse(final byte[] array){
		int i = 0;
		int j = array.length - 1;
		byte tmp;
		while(j > i){
			tmp = array[j];
			array[j] = array[i];
			array[i] = tmp;
			j --;
			i ++;
		}
	}

}
