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


	/**
	 * Returns the size of the CRC (Cyclic Redundancy Check) in bits.
	 *
	 * @return	The number of bits used for the CRC.
	 */
	int crcSize();

	/**
	 * Returns the polynomial value used in the checksum calculation.
	 *
	 * @return	The polynomial value as a long.
	 */
	long polynomial();

	/**
	 * Determines whether the input data should be reflected (bitwise reversed) before performing a checksum calculation.
	 *
	 * @return	Whether data reflection is enabled.
	 */
	default boolean reflectData(){
		return false;
	}

	/**
	 * Provides the initial value used in the checksum calculation.
	 *
	 * @return	The initial value for the checksum computation.
	 */
	default long initialValue(){
		return 0;
	}

	/**
	 * Determines whether the CRC (Cyclic Redundancy Check) output should be reflected (bitwise reversed) after computation.
	 *
	 * @return	Whether the CRC output should be reflected.
	 */
	default boolean reflectCRCOut(){
		return false;
	}

	/**
	 * Returns the XOR-out value used in the checksum calculation process.
	 * <p>
	 * The XOR-out value is a final bitwise operation applied to the computed checksum value.
	 * </p>
	 *
	 * @return	The XOR-out value.
	 */
	default long xorOut(){
		return 0;
	}

	/**
	 * Compute a generic CRC.
	 *
	 * @param data	Array of bytes to process.
	 * @param start	Start index of the input array.
	 * @param end	End index of the input array.
	 * @return	The computed CRC value.
	 */
	default Number calculateCRC(final byte[] data, final int start, final int end){
		final int width = crcSize();
		final int topBit = 1 << (width - 1);
		final long crcMask = (1l << width) - 1;

		final long polynomial = polynomial();
		final boolean reflectData = reflectData();
		long crc = initialValue();
		for(int i = Math.max(start, 0), length = Math.min(end, data.length); i < length; i ++){
			long datum = data[i] & 0xFF;

			if(reflectData)
				datum = reflect(datum, Byte.SIZE);
			crc ^= (datum << (width - Byte.SIZE));

			for(int j = 0; j < Byte.SIZE; j ++){
				final boolean highBit = ((crc & topBit) != 0);
				crc <<= 1;
				if(highBit)
					crc ^= polynomial;
			}

			crc &= crcMask;
		}

		if(reflectCRCOut())
			crc = reflect(crc, width);
		crc ^= xorOut();
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
