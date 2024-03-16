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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;


class CRC16CCITTTest{

	@Test
	void test(){
		CRC16CCITT crc = new CRC16CCITT();
		Number crc16 = crc.calculateChecksum("9142656".getBytes(StandardCharsets.US_ASCII), 0, 7, CRC16CCITT.START_VALUE_0xFFFF);

		Assertions.assertEquals((short)0x763A, crc16.shortValue());
	}

	@Test
	void oneToFour(){
		CRC16CCITT crc = new CRC16CCITT();
		Number crc16 = crc.calculateChecksum(new byte[]{0x01, 0x02, 0x03, 0x04}, 0, 4, CRC16CCITT.START_VALUE_0xFFFF);

		Assertions.assertEquals((short)0x89C3, crc16.shortValue());
	}

}
