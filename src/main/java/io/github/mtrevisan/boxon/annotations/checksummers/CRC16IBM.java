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


/**
 * Calculates a 16 bit Cyclic Redundancy Check of a sequence of bytes using the CRC-IBM algorithm.
 * <p>Also known as CRC-16 and CRC-16-ANSI</p>
 *
 * @see <a href="https://en.wikipedia.org/wiki/Cyclic_redundancy_check">Cyclic Redundancy Check</a>
 * @see <a href="https://www.source-code.biz/snippets/java/crc16/">Crc16 - Fast byte-wise 16-bit CRC calculation</a>
 */
public final class CRC16IBM implements Checksummer{

	/** CCITT polynomial: x^16 + x^15 + x^2 + 1 -> 1000_0000_0000_0101 = 0x8005 (reversed is 0xA001). */
	private static final int POLYNOMIAL_REVERSED = 0x0000_A001;


	CRC16IBM(){}


	@Override
	public short calculateChecksum(final byte[] data, final int start, final int end){
		int value = 0x0000;
		for(int i = Math.max(start, 0), length = Math.min(end, data.length); i < length; i ++){
			final byte datum = data[i];

			value = updateChecksum(datum, value);
		}
		return (short)value;
	}

	private static int updateChecksum(final byte datum, int value){
		value ^= datum & 0xFF;
		for(int j = 0; j < Byte.SIZE; j ++){
			final boolean carry = ((value & 0x01) != 0);
			value >>>= 1;
			if(carry)
				value ^= POLYNOMIAL_REVERSED;
		}
		return value;
	}

}
