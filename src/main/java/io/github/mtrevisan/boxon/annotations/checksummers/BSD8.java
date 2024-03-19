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
	public static final int START_VALUE_0x00 = 0x00;

	private static final int LEFT_SHIFT = Byte.SIZE - 1;
	private static final int MASK = (1 << Byte.SIZE) - 1;


	BSD8(){}


	@Override
	public short calculateChecksum(final byte[] data, final int start, final int end, final int startValue){
		int checksum = startValue;
		for(int i = Math.max(start, 0), length = Math.min(end, data.length); i < length; i ++)
			//apply circular right shift and add new value
			checksum = MASK & ((checksum >> 1) + ((checksum & 1) << LEFT_SHIFT) + (data[i] & 0xFF));
		return (short)checksum;
	}

	public static short csum1(byte[] data){
		int lcrc = 0;
		for(int i = 0; i < data.length; i++){
			if((lcrc & 1) != 0)
				lcrc |= 0x100;
			lcrc = ((lcrc >> 1) + (data[i] & 0xFF)) & 0xFF;
		}
		return (short)lcrc;
	}
	public static short bsdChecksumFromByteArray(byte[] data) {
		int checksum = 0;
		for(int i = 0, length = data.length; i < length; i ++)
			checksum = MASK & ((checksum >> 1) + ((checksum & 1) << LEFT_SHIFT) + (data[i] & 0xFF));
		return (short)checksum;
	}
	public static void main(String[] args) {
		byte[] data = { (byte) 57, (byte) 49, (byte) 52, (byte) 50, (byte) 54, (byte) 53, (byte) 54 };

		System.out.println("Checksum BSD8: " + csum1(data));
		System.out.println("Checksum BSD8: " + bsdChecksumFromByteArray(data));
	}

}
