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
package io.github.mtrevisan.boxon.core;

import io.github.mtrevisan.boxon.core.codecs.queclink.ACKMessageASCII;
import io.github.mtrevisan.boxon.core.codecs.queclink.ACKMessageHex;
import io.github.mtrevisan.boxon.core.codecs.queclink.DeviceTypes;
import io.github.mtrevisan.boxon.helpers.StringHelper;
import io.github.mtrevisan.boxon.utils.TestHelper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.Map;


class ComposerTest{

	@Test
	void parseAndComposeSingleMessageHex() throws Exception{
		DeviceTypes<Byte> deviceTypes = DeviceTypes.<Byte>create()
			.with((byte)0x46, "QUECLINK_GB200S");
		Core core = CoreBuilder.builder()
			.withContext("deviceTypes", deviceTypes)
			.withContext(ParserTest.class, "headerLength")
			.withDefaultCodecs()
			.withTemplate(ACKMessageHex.class)
			.build();
		Parser parser = Parser.create(core);
		Composer composer = Composer.create(core);

		//parse:
		byte[] payload = StringHelper.hexToByteArray("2b41434b066f2446010a0311235e40035110420600ffff07e30405083639001265b60d0a");
		List<Response<byte[], Object>> result = parser.parse(payload);

		Assertions.assertNotNull(result);
		Assertions.assertEquals(1, result.size());
		Response<byte[], Object> response = result.getFirst();
		Assertions.assertArrayEquals(payload, response.getSource());
		if(response.hasError())
			Assertions.fail(response.getError());

		//compose:
		Response<ACKMessageHex, byte[]> composeResult = composer.compose((ACKMessageHex)response.getMessage());

		if(composeResult.hasError())
			Assertions.fail(composeResult.getError());
		Assertions.assertArrayEquals(payload, composeResult.getMessage());
	}

	@Test
	void parseAndComposeSingleMessageASCII() throws Exception{
		DeviceTypes<Byte> deviceTypes = DeviceTypes.<Byte>create()
			.with((byte)0xCF, "QUECLINK_GV350M");
		Map<String, Object> context = Collections.singletonMap("deviceTypes", deviceTypes);
		Core core = CoreBuilder.builder()
			.withContext(context)
			.withDefaultCodecs()
			.withTemplate(ACKMessageASCII.class)
			.build();
		Parser parser = Parser.create(core);
		Composer composer = Composer.create(core);

		//parse:
		byte[] payload = TestHelper.toByteArray("+ACK:GTIOB,CF8002,359464038116666,45.5,2,0020,,,20170101123542,11F0$");
		List<Response<byte[], Object>> result = parser.parse(payload);

		Assertions.assertNotNull(result);
		Assertions.assertEquals(1, result.size());
		Response<byte[], Object> response = result.getFirst();
		Assertions.assertArrayEquals(payload, response.getSource());
		if(response.hasError())
			Assertions.fail(response.getError());

		//compose:
		Response<ACKMessageASCII, byte[]> composeResult = composer.compose((ACKMessageASCII)response.getMessage());

		if(composeResult.hasError())
			Assertions.fail(composeResult.getError());
		Assertions.assertArrayEquals(payload, composeResult.getMessage());
	}

}
