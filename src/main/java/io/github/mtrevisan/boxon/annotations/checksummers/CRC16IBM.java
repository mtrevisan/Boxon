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

	/** x^16 + x^15 + x^2 + 1 -> 1000_0000_0000_0101 = 0x8005. */
	private static final int POLYNOMIAL = 0x0000_8005;


	@Override
	public int crcSize(){
		return 16;
	}

	@Override
	public long polynomial(){
		return POLYNOMIAL;
	}

	@Override
	public boolean reflectData(){
		return true;
	}

	@Override
	public boolean reflectCRCOut(){
		return true;
	}

	@Override
	public Number calculateChecksum(final byte[] data, final int start, final int end){
		final Number crc = calculateCRC(data, start, end);
		return crc.shortValue();
	}

	private static Number calculateChecksumReversed(final byte[] data, final int polynomialReversed, final int initialValue,
		final int start, final int end){
		int checksum = initialValue;
		for(int i = Math.max(start, 0), length = Math.min(end, data.length); i < length; i ++){
			final byte datum = data[i];

			checksum = updateChecksum(datum, polynomialReversed, checksum);
		}
		return checksum;
	}

	private static int updateChecksum(final byte datum, final int polynomialReversed, int crc){
		crc ^= datum & 0xFF;
		for(int j = 0; j < Byte.SIZE; j ++){
			final boolean carry = ((crc & 0x01) != 0);
			crc >>>= 1;
			if(carry)
				crc ^= polynomialReversed;
		}
		return crc;
	}

}
