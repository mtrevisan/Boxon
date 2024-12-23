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
 * An abstract class for calculating Cyclic Redundancy Checks (CRC).
 * <p>
 * This class provides a framework for implementing various CRC algorithms by defining necessary methods and utilities for CRC computation.
 * </p>
 *
 * @see <a href="https://en.wikipedia.org/wiki/Cyclic_redundancy_check">Cyclic Redundancy Check</a>
 * @see <a href="https://reveng.sourceforge.io/crc-catalogue/">CRC RevEng</a>
 * @see <a href="https://www.sunshine2k.de/articles/coding/crc/understanding_crc.html">Understanding and implementing CRC (Cyclic Redundancy Check) calculation</a>
 */
public final class CRCHelper{

	private CRCHelper(){}


	/**
	 * Compute a generic CRC.
	 *
	 * @param parameters	The parameters of the CRC algorithm.
	 * @param data	Array of bytes to process.
	 * @return	The computed CRC value.
	 */
	public static Number calculateCRC(final CRCParameters parameters, final byte[] data){
		return calculateCRC(parameters, data, 0, data.length);
	}

	/**
	 * Compute a generic CRC.
	 *
	 * @param parameters	The parameters of the CRC algorithm.
	 * @param data	Array of bytes to process.
	 * @param start	Start index of the input array.
	 * @param end	End index of the input array.
	 * @return	The computed CRC value.
	 */
	public static Number calculateCRC(final CRCParameters parameters, final byte[] data, final int start, final int end){
		final int width = parameters.width;
		final int bitOffsetToByteSize = Math.max(Byte.SIZE - width, 0);
		final int bitsToShift = Math.max(width - Byte.SIZE, 0);
		final long highBitMask = (1l << (width - 1)) << bitOffsetToByteSize;
		final long crcMask = (highBitMask << 1) - 1;
		final long polynomial = (parameters.polynomial << bitOffsetToByteSize);
		final boolean reflectInput = parameters.reflectInput;

		long crc = (parameters.initialValue << bitOffsetToByteSize);
		for(int i = start; i < end; i ++){
			final long datum = (reflectInput? reflect(data[i], Byte.SIZE): data[i]);

			//move byte into MSB of CRC and XOR with CRC
			crc ^= (datum << bitsToShift);

			for(int j = 0; j < Byte.SIZE; j ++){
				final boolean highBit = ((crc & highBitMask) != 0);
				crc <<= 1;
				if(highBit)
					crc ^= polynomial;
			}

			crc &= crcMask;
		}

		crc >>>= bitOffsetToByteSize;
		if(parameters.reflectOutput)
			crc = reflect(crc, width);
		crc ^= parameters.xorOutputValue;
		return crc & crcMask;
	}

	/**
	 * Reflects the order of bits in a value.
	 *
	 * @param value	The value to reflect.
	 * @param bitCount	The number of bits to reflect.
	 * @return	The reflected value.
	 */
	private static long reflect(final long value, final int bitCount){
		long reflected = 0;
		long topBit = 1l;
		for(int i = 0; i < bitCount; i ++){
			if((value & topBit) != 0)
				reflected |= (1l << (bitCount - 1 - i));

			topBit <<= 1;
		}
		return reflected;
	}

}
