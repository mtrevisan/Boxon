/**
 * Copyright (c) 2020-2021 Mauro Trevisan
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

import io.github.mtrevisan.boxon.exceptions.AnnotationException;
import io.github.mtrevisan.boxon.exceptions.ConfigurationException;
import io.github.mtrevisan.boxon.exceptions.TemplateException;
import io.github.mtrevisan.boxon.internal.StringHelper;
import io.github.mtrevisan.boxon.internal.TimeWatch;
import io.github.mtrevisan.boxon.core.codecs.queclink.ACKMessageHex;
import io.github.mtrevisan.boxon.core.codecs.queclink.DeviceTypes;
import io.github.mtrevisan.boxon.core.codecs.queclink.REGConfigurationASCII;
import org.apache.commons.lang3.ArrayUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;


@SuppressWarnings("ALL")
class ParserTest{

	public static void main(String[] args) throws NoSuchMethodException, AnnotationException, TemplateException, ConfigurationException{
		DeviceTypes deviceTypes = new DeviceTypes();
		deviceTypes.add("QUECLINK_GB200S", (byte)0x46);
		Map<String, Object> context = Collections.singletonMap("deviceTypes", deviceTypes);
		Parser parser = Parser.create()
			.withContext(context)
			.withDefaultCodecs()
			.withTemplates(ACKMessageHex.class)
			.withDefaultConfigurations()
			.withContextFunction(ParserTest.class.getDeclaredMethod("headerSize"));

		//~251 µs/msg = 4 kHz
		byte[] payload = StringHelper.toByteArray("2b41434b066f2446010a0311235e40035110420600ffff07e30405083639001265b60d0a2b41434b066f2446010a0311235e40035110420600ffff07e30405083639001265b60d0a");
		//warm-up
		for(int i = 0; i < 2_000; i ++)
			parser.parse(payload);
		TimeWatch watch = TimeWatch.start();
		for(int i = 0; i < 20_000; i ++)
			parser.parse(payload);
		watch.stop();
		System.out.println(watch.toStringMicros(20_000));
	}

	@Test
	void parseAndComposeSingleMessageHex() throws NoSuchMethodException, AnnotationException, TemplateException{
		DeviceTypes deviceTypes = new DeviceTypes();
		deviceTypes.add("QUECLINK_GB200S", (byte)0x46);
		Parser parser = Parser.create()
			.addToContext("deviceTypes", deviceTypes)
			.withDefaultCodecs()
			.withTemplates(ACKMessageHex.class)
			.withContextFunction(ParserTest.class, "headerSize");

		//parse:
		byte[] payload = StringHelper.toByteArray("2b41434b066f2446010a0311235e40035110420600ffff07e30405083639001265b60d0a");
		ParseResponse parseResult = parser.parse(payload);

		Assertions.assertFalse(parseResult.hasErrors());
		Assertions.assertEquals(1, parseResult.getParsedMessageCount());

		//compose:
		ComposeResponse composeResult = parser.composeMessage(parseResult.getParsedMessageAt(0));

		Assertions.assertFalse(composeResult.hasErrors());
		Assertions.assertArrayEquals(payload, composeResult.getComposedMessage());
	}

	private static int headerSize(){
		return 4;
	}

	@Test
	void parseMultipleMessagesHex() throws NoSuchMethodException, AnnotationException, TemplateException{
		DeviceTypes deviceTypes = new DeviceTypes();
		deviceTypes.add("QUECLINK_GB200S", (byte)0x46);
		Map<String, Object> context = Collections.singletonMap("deviceTypes", deviceTypes);
		Parser parser = Parser.create()
			.withContext(context)
			.withDefaultCodecs()
			.withDefaultTemplates()
			.withContextFunction(ParserTest.class.getDeclaredMethod("headerSize"));

		byte[] payload = StringHelper.toByteArray("2b41434b066f2446010a0311235e40035110420600ffff07e30405083639001265b60d0a2b41434b066f2446010a0311235e40035110420600ffff07e30405083639001265b60d0a");
		ParseResponse result = parser.parse(payload);

		Assertions.assertFalse(result.hasErrors());
		Assertions.assertEquals(2, result.getParsedMessageCount());
	}

	@Test
	void parseAndComposeSingleMessageASCII() throws AnnotationException, TemplateException{
		DeviceTypes deviceTypes = new DeviceTypes();
		deviceTypes.add("QUECLINK_GV350M", (byte)0xCF);
		Map<String, Object> context = Collections.singletonMap("deviceTypes", deviceTypes);
		Parser parser = Parser.create()
			.withContext(context)
			.withDefaultCodecs()
			.withTemplates(ACKMessageHex.class);

		//parse:
		byte[] payload = "+ACK:GTIOB,CF8002,359464038116666,GV350MG,2,0020,20170101123542,11F0$".getBytes(StandardCharsets.ISO_8859_1);
		ParseResponse parseResult = parser.parse(payload);

		Assertions.assertFalse(parseResult.hasErrors());
		Assertions.assertEquals(1, parseResult.getParsedMessageCount());

		//compose:
		ComposeResponse composeResult = parser.composeMessage(parseResult.getParsedMessageAt(0));

		Assertions.assertFalse(composeResult.hasErrors());
		Assertions.assertArrayEquals(payload, composeResult.getComposedMessage());
	}

	@Test
	void parseMultipleMessagesASCII() throws AnnotationException, TemplateException{
		DeviceTypes deviceTypes = new DeviceTypes();
		deviceTypes.add("QUECLINK_GV350M", (byte)0xCF);
		Map<String, Object> context = Collections.singletonMap("deviceTypes", deviceTypes);
		Parser parser = Parser.create()
			.withContext(context)
			.withDefaultCodecs()
			.withTemplates(ACKMessageHex.class);

		byte[] payload = "+ACK:GTIOB,CF8002,359464038116666,GV350MG,2,0020,20170101123542,11F0$+ACK:GTIOB,CF8002,359464038116666,GV350MG,2,0020,20170101123542,11F0$".getBytes(StandardCharsets.ISO_8859_1);
		ParseResponse result = parser.parse(payload);

		Assertions.assertFalse(result.hasErrors());
		Assertions.assertEquals(2, result.getParsedMessageCount());
	}

	@Test
	void parseMultipleMessagesHexASCII() throws NoSuchMethodException, AnnotationException, TemplateException{
		DeviceTypes deviceTypes = new DeviceTypes();
		deviceTypes.add("QUECLINK_GB200S", (byte)0x46);
		deviceTypes.add("QUECLINK_GV350M", (byte)0xCF);
		Map<String, Object> context = Collections.singletonMap("deviceTypes", deviceTypes);
		Parser parser = Parser.create()
			.withContext(context)
			.withDefaultCodecs()
			.withTemplates(ACKMessageHex.class)
			.withContextFunction(ParserTest.class.getDeclaredMethod("headerSize"));

		byte[] payload1 = StringHelper.toByteArray("2b41434b066f2446010a0311235e40035110420600ffff07e30405083639001265b60d0a");
		byte[] payload2 = "+ACK:GTIOB,CF8002,359464038116666,GV350MG,2,0020,20170101123542,11F0$".getBytes(StandardCharsets.ISO_8859_1);
		byte[] payload = ArrayUtils.addAll(payload1, payload2);
		ParseResponse result = parser.parse(payload);

		Assertions.assertFalse(result.hasErrors());
		Assertions.assertEquals(2, result.getParsedMessageCount());
	}

	@Test
	void parseMultipleMessagesASCIIHex() throws AnnotationException, TemplateException{
		DeviceTypes deviceTypes = new DeviceTypes();
		deviceTypes.add("QUECLINK_GB200S", (byte)0x46);
		deviceTypes.add("QUECLINK_GV350M", (byte)0xCF);
		Map<String, Object> context = Collections.singletonMap("deviceTypes", deviceTypes);
		Parser parser = Parser.create()
			.withContext(context)
			.withDefaultCodecs()
			.withTemplates(ACKMessageHex.class);

		byte[] payload1 = StringHelper.toByteArray("2b41434b066f2446010a0311235e40035110420600ffff07e30405083639001265b60d0a");
		byte[] payload2 = "+ACK:GTIOB,CF8002,359464038116666,GV350MG,2,0020,20170101123542,11F0$".getBytes(StandardCharsets.ISO_8859_1);
		byte[] payload = ArrayUtils.addAll(payload2, payload1);
		ParseResponse result = parser.parse(payload);

		Assertions.assertFalse(result.hasErrors());
		Assertions.assertEquals(2, result.getParsedMessageCount());
	}


	@Test
	void composeSingleConfigurationMessage() throws NoSuchMethodException, AnnotationException, TemplateException, ConfigurationException{
		DeviceTypes deviceTypes = new DeviceTypes();
		deviceTypes.add("QUECLINK_GB200S", (byte)0x46);
		Parser parser = Parser.create()
			.withDefaultCodecs()
			.withConfigurations(REGConfigurationASCII.class);

		//data:
		Map<String, Object> configurationData = new HashMap<>();
		configurationData.put("Weekday", "TUESDAY|WEDNESDAY");
		configurationData.put("Update Over-The-Air", "TRUE");
		configurationData.put("Header", "GTREG");
		configurationData.put("Download protocol", "HTTP");
		configurationData.put("Download URL", Map.of(
			"URL", "http://url.com",
			"username", "username",
			"password", "password"
		));
		configurationData.put("Update mode", 0);
		configurationData.put("Maximum download retry count", 2);
		configurationData.put("Message counter", 123);
		configurationData.put("Operation mode", 1);
		configurationData.put("Password", "pass");
		configurationData.put("Download timeout", 25);

		//compose:
		ComposeResponse composeResult = parser.composeConfiguration("1.20", Collections.singletonMap("AT+", configurationData));

		Assertions.assertFalse(composeResult.hasErrors());
		Assertions.assertEquals("AT+GTREG=pass,1,1,0,2,25,0,http://url.com@username@password,3600,3600,6,,7b$",
			new String(composeResult.getComposedMessage()));
	}

}
