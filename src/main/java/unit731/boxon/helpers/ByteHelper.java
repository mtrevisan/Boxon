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

import unit731.boxon.codecs.ByteOrder;

import java.lang.reflect.Array;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.BitSet;


public class ByteHelper{

	private ByteHelper(){}

	public static boolean hasBit(final byte mask, final int index){
		if(index < 0 || index >= Byte.SIZE)
			throw new IllegalArgumentException("Index value must be between 0 and " + (Byte.SIZE - 1) + " inclusive, was " + index);

		return ((mask & (1 << (index % Byte.SIZE))) != 0);
	}

	public static long clearBit(final long value, final int index){
		return (value & ~(1 << index));
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
	 * @param byteArray	Array to be converted to hexadecimal characters
	 * @return	The hexadecimal characters
	 */
	public static String byteArrayToHexString(final byte[] byteArray){
		final StringBuffer sb = new StringBuffer(byteArray.length << 1);
		for(final byte b : byteArray){
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
	 * @param mask	The mask
	 * @return	The masked and shifter value
	 */
	public static byte applyMaskAndShift(byte value, byte mask){
		final byte ctz = countTrailingZeros(mask);
		value = (byte)((value & mask) >> ctz);
		mask = (byte)(0xFF >>> ctz);
		return (byte)(value & mask);
	}

	/**
	 * Apply mask and shift right (<code>maskByte(27, 0x18) = 3</code>)
	 *
	 * @param value	The value to which to apply the mask and the right shift
	 * @param mask	The mask
	 * @return	The masked and shifter value
	 */
	public static int applyMaskAndShift(int value, int mask){
		final int ctz = countTrailingZeros(mask);
		value = ((value & mask) >> ctz);
		mask = (0xFFFF_FFFF >>> ctz);
		return (value & mask);
	}

	/**
	 * Compute <code>((highValue &amp; highMask) &lt;&lt; count-leading-ones(lowMask) | (lowValue &amp; lowMask) &gt;&gt;&gt; count-trailing-zeros(lowMask))</code>
	 *
	 * @param highValue	The high value
	 * @param highMask	The mask to apply to the high value
	 * @param lowValue	The low value
	 * @param lowMask	The mask to apply to the low value
	 * @return	The composed value of high value, masked with its mask, and shifted by the low mask length, with the masked low value
	 */
	public static int compose(final byte highValue, final byte highMask, final byte lowValue, final byte lowMask){
		final byte clo = countTrailingZeros((byte)~applyMaskAndShift((byte)0xFF, lowMask));
		return ((highValue & highMask) << clo) | applyMaskAndShift((int)lowValue & 0xFF, lowMask);
	}

	private static byte countTrailingZeros(byte x){
		byte n = Byte.SIZE;
		if(x != 0){
			n = 0;
			if((x & 0x0F) == 0){
				n += 4;
				x >>= 4;
			}
			if((x & 0x03) == 0){
				n += 2;
				x >>= 2;
			}
			if((x & 0x01) == 0)
				n ++;
		}
		return n;
	}

	private static int countTrailingZeros(int x){
		byte n = Integer.SIZE;
		if(x != 0){
			n = 0;
			if((x & 0x0000_FFFF) == 0){
				n += 8;
				x >>= 8;
			}
			if((x & 0x0000_00FF) == 0){
				n += 8;
				x >>= 8;
			}
			if((x & 0x0000_000F) == 0){
				n += 4;
				x >>= 4;
			}
			if((x & 0x0000_0003) == 0){
				n += 2;
				x >>= 2;
			}
			if((x & 0x0000_0001) == 0)
				n ++;
		}
		return n;
	}

	/**
	 * Convert the value to signed primitive
	 *
	 * @param value	Field value
	 * @param size	Length in bits of the field
	 * @return	The 2-complement expressed as int
	 */
	public static int convert2Complement(final int value, final int size){
		final int shift = Integer.SIZE - size;
		return (value << shift) >> shift;
	}

	public static long extendSign(final long value, final int size){
		final long mask = 1l << (size - 1);
		return (value ^ mask) - mask;
	}

	public static void reverseBits(final BitSet input, final int size){
		for(int i = 0; i < size / 2; i ++){
			final boolean t = input.get(i);
			input.set(i, input.get(size - i - 1));
			input.set(size - i - 1, t);
		}
	}

	public static BigInteger bitsToBigInteger(final BitSet bits, final int size, final ByteOrder byteOrder, final boolean unsigned){
		byte[] array = bits.toByteArray();
		if(byteOrder == ByteOrder.LITTLE_ENDIAN)
			//NOTE: need to reverse the bytes because BigInteger is big-endian and BitSet is little-endian
			array = reverseBytes(array);

		if(size >= array.length * Byte.SIZE){
			final byte[] extendedArray = new byte[array.length + 1];
//			if((array[0] & 0x80) != 0x00)
//				extendedArray[0] = (byte)0xFF;
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
	 * @return	The byte array (leading byte is always different from <code>0</code>), empty array if the value is zero.
	 */
	public static byte[] bigIntegerToBytes(final BigInteger value, final int size, final ByteOrder byteOrder/*, final boolean unsigned*/){
		byte[] array = value.toByteArray();
		if(size < array.length * Byte.SIZE)
			array = Arrays.copyOfRange(array, 1, array.length);
		if(byteOrder == ByteOrder.LITTLE_ENDIAN)
			//NOTE: need to reverse the bytes because BigInteger is big-endian and BitSet is little-endian
			array = reverseBytes(array);
		return array;
	}

	private static byte[] reverseBytes(final byte[] bytes){
		for(int i = 0; i < bytes.length / 2; i ++){
			final byte temp = bytes[i];
			bytes[i] = bytes[bytes.length - i - 1];
			bytes[bytes.length - i - 1] = temp;
		}
		return bytes;
	}

}
