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
 * Calculates an 8 bit BSD checksum from a sequence of bytes.
 *
 * @see <a href="https://en.wikipedia.org/wiki/BSD_checksum">BSD checksum</a>
 */
public final class BSD8 implements Checksummer{

	/** Starting value 0x00. */
	public static final byte START_VALUE_0x00 = 0x00;


	BSD8(){}


	@Override
	public short calculateChecksum(final byte[] data, final int start, final int end, final int startValue){
		byte value = (byte)startValue;
		for(int i = Math.max(start, 0), length = Math.min(end, data.length); i < length; i ++){
			final byte datum = data[i];

			//apply circular right shift and add new value
			value = (byte)((value >>> 1) + ((value & 0x01) << 7) + datum);
		}
		return value;
	}

}
