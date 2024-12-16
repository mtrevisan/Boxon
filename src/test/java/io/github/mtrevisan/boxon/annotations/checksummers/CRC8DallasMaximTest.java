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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;


/**
 * @see <a href="https://www.sunshine2k.de/coding/javascript/crc/crc_js.html">CRC Calculator (Javascript)</a>
 */
class CRC8DallasMaximTest{

	@Test
	void oneToFour(){
		Number checksum = CRCHelper.calculateCRC(CRCParameters.CRC8_DALLAS_MAXIM, new byte[]{0x01, 0x02, 0x03, 0x04}, 0, 4);

		Assertions.assertEquals((byte)0xF4, checksum.byteValue());
	}

	@Test
	void test(){
		Number checksum = CRCHelper.calculateCRC(CRCParameters.CRC8_DALLAS_MAXIM, "9142656".getBytes(StandardCharsets.US_ASCII), 0, 7);

		Assertions.assertEquals((byte)0x68, checksum.byteValue());
	}

}
