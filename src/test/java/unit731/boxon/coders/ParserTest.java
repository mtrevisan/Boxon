/**
 * Copyright (c) 2020 Mauro Trevisan
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
package unit731.boxon.coders;

import unit731.boxon.coders.dtos.ComposeResponse;
import unit731.boxon.coders.dtos.ParseResponse;
import unit731.boxon.coders.queclink.DeviceTypes;
import unit731.boxon.helpers.ByteHelper;
import org.apache.commons.lang3.ArrayUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import unit731.boxon.helpers.TimeWatch;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;


class ParserTest{

	@Test
	void parseAndComposeSingeMessageHex(){
		DeviceTypes deviceTypes = new DeviceTypes();
		deviceTypes.add("QUECLINK_GB200S", (byte)0x46);
		Map<String, Object> context = Collections.singletonMap("deviceTypes", deviceTypes);
		Parser parser = Parser.create()
			.withContext(context)
			.withDefaultCoders()
			.withDefaultCodecs();

		//parse:
		byte[] payload = ByteHelper.toByteArray("2b41434b066f2446010a0311235e40035110420600ffff07e30405083639001265b60d0a");
		ParseResponse parseResult = parser.parse(payload);

		Assertions.assertFalse(parseResult.hasErrors());
		Assertions.assertEquals(1, parseResult.getParsedMessages().size());

		//compose:
		ComposeResponse composeResult = parser.compose(parseResult.getParsedMessages().get(0));

		Assertions.assertFalse(composeResult.hasErrors());
		Assertions.assertArrayEquals(payload, composeResult.getComposedMessage());
	}

	@Test
	void parseMultipleMessagesHex(){
		DeviceTypes deviceTypes = new DeviceTypes();
		deviceTypes.add("QUECLINK_GB200S", (byte)0x46);
		Map<String, Object> context = Collections.singletonMap("deviceTypes", deviceTypes);
		Parser parser = Parser.create()
			.withContext(context)
			.withDefaultCoders()
			.withDefaultCodecs();

		byte[] payload = ByteHelper.toByteArray("2b41434b066f2446010a0311235e40035110420600ffff07e30405083639001265b60d0a2b41434b066f2446010a0311235e40035110420600ffff07e30405083639001265b60d0a");
		ParseResponse result = parser.parse(payload);

		Assertions.assertFalse(result.hasErrors());
		Assertions.assertEquals(2, result.getParsedMessages().size());
	}

	@Test
	void parseAndComposeSingleMessageASCII(){
		DeviceTypes deviceTypes = new DeviceTypes();
		deviceTypes.add("QUECLINK_GV350M", (byte)0xCF);
		Map<String, Object> context = Collections.singletonMap("deviceTypes", deviceTypes);
		Parser parser = Parser.create()
			.withContext(context)
			.withDefaultCoders()
			.withDefaultCodecs();

		//parse:
		byte[] payload = "+ACK:GTIOB,CF8002,359464038116666,GV350MG,2,0020,20170101123542,11F0$".getBytes(StandardCharsets.ISO_8859_1);
		ParseResponse parseResult = parser.parse(payload);

		Assertions.assertFalse(parseResult.hasErrors());
		Assertions.assertEquals(1, parseResult.getParsedMessages().size());

		//compose:
		ComposeResponse composeResult = parser.compose(parseResult.getParsedMessages().get(0));

		Assertions.assertFalse(composeResult.hasErrors());
		Assertions.assertArrayEquals(payload, composeResult.getComposedMessage());
	}

	@Test
	void parseMultipleMessagesASCII(){
		DeviceTypes deviceTypes = new DeviceTypes();
		deviceTypes.add("QUECLINK_GV350M", (byte)0xCF);
		Map<String, Object> context = Collections.singletonMap("deviceTypes", deviceTypes);
		Parser parser = Parser.create()
			.withContext(context)
			.withDefaultCoders()
			.withDefaultCodecs();

		byte[] payload = "+ACK:GTIOB,CF8002,359464038116666,GV350MG,2,0020,20170101123542,11F0$+ACK:GTIOB,CF8002,359464038116666,GV350MG,2,0020,20170101123542,11F0$".getBytes(StandardCharsets.ISO_8859_1);
		ParseResponse result = parser.parse(payload);

		Assertions.assertFalse(result.hasErrors());
		Assertions.assertEquals(2, result.getParsedMessages().size());
	}

	@Test
	void parseMultipleMessagesHexASCII(){
		DeviceTypes deviceTypes = new DeviceTypes();
		deviceTypes.add("QUECLINK_GB200S", (byte)0x46);
		deviceTypes.add("QUECLINK_GV350M", (byte)0xCF);
		Map<String, Object> context = Collections.singletonMap("deviceTypes", deviceTypes);
		Parser parser = Parser.create()
			.withContext(context)
			.withDefaultCoders()
			.withDefaultCodecs();

		byte[] payload1 = ByteHelper.toByteArray("2b41434b066f2446010a0311235e40035110420600ffff07e30405083639001265b60d0a");
		byte[] payload2 = "+ACK:GTIOB,CF8002,359464038116666,GV350MG,2,0020,20170101123542,11F0$".getBytes(StandardCharsets.ISO_8859_1);
		byte[] payload = ArrayUtils.addAll(payload1, payload2);
		ParseResponse result = parser.parse(payload);

		Assertions.assertFalse(result.hasErrors());
		Assertions.assertEquals(2, result.getParsedMessages().size());
	}

	@Test
	void parseMultipleMessagesASCIIHex(){
		DeviceTypes deviceTypes = new DeviceTypes();
		deviceTypes.add("QUECLINK_GB200S", (byte)0x46);
		deviceTypes.add("QUECLINK_GV350M", (byte)0xCF);
		Map<String, Object> context = Collections.singletonMap("deviceTypes", deviceTypes);
		Parser parser = Parser.create()
			.withContext(context)
			.withDefaultCoders()
			.withDefaultCodecs();

		byte[] payload1 = ByteHelper.toByteArray("2b41434b066f2446010a0311235e40035110420600ffff07e30405083639001265b60d0a");
		byte[] payload2 = "+ACK:GTIOB,CF8002,359464038116666,GV350MG,2,0020,20170101123542,11F0$".getBytes(StandardCharsets.ISO_8859_1);
		byte[] payload = ArrayUtils.addAll(payload2, payload1);
		ParseResponse result = parser.parse(payload);

		Assertions.assertFalse(result.hasErrors());
		Assertions.assertEquals(2, result.getParsedMessages().size());
	}

}
