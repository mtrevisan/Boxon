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
package io.github.mtrevisan.boxon.annotations.checksummers;


/** The checksum algorithm to be applied. */
public interface Checksummer{

	/**
	 * Method used to calculate the checksum.
	 *
	 * @param data	The byte array from which to calculate the checksum.
	 * @param start	The starting byte on the given array.
	 * @param end	The ending byte on the given array.
	 * @return	The checksum.
	 */
	Number calculateChecksum(byte[] data, int start, int end);


	static Number calculateChecksumReversed(final byte[] data, final int polynomialReversed, final int start, final int end){
		int checksum = 0x0000;
		for(int i = Math.max(start, 0), length = Math.min(end, data.length); i < length; i ++){
			final byte datum = data[i];

			checksum = updateChecksum(datum, polynomialReversed, checksum);
		}
		return checksum;
	}

	private static int updateChecksum(final byte datum, final int polynomialReversed, int checksum){
		checksum ^= datum & 0xFF;
		for(int j = 0; j < Byte.SIZE; j ++){
			final boolean carry = ((checksum & 0x01) != 0);
			checksum >>>= 1;
			if(carry)
				checksum ^= polynomialReversed;
		}
		return checksum;
	}

}
