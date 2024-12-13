/*
 * Copyright (c) 2024 Mauro Trevisan
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
 * Calculates a 8 bit Cyclic Redundancy Check of a sequence of bytes using the Dallas/Maxim algorithm.
 *
 * @see <a href="https://en.wikipedia.org/wiki/Cyclic_redundancy_check">Cyclic Redundancy Check</a>
 */
public final class CRC8DallasMaxim implements Checksummer{

	/** x^8 + x^5 + x^4 + 1 -> 1_0011_0001 = 0x31. */
	private static final long POLYNOMIAL = 0x0000_0031;


	@Override
	public int crcSize(){
		return 8;
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
		return crc.byteValue();
	}

}
