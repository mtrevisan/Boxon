/*
 * Copyright (c) 2020-2022 Mauro Trevisan
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
import io.github.mtrevisan.boxon.exceptions.AnnotationException;
import io.github.mtrevisan.boxon.exceptions.ConfigurationException;
import io.github.mtrevisan.boxon.exceptions.TemplateException;
import io.github.mtrevisan.boxon.helpers.StringHelper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;


@SuppressWarnings("ALL")
class ComposerTest{

	@Test
	void parseAndComposeSingleMessageHex() throws NoSuchMethodException, AnnotationException, TemplateException, ConfigurationException{
		DeviceTypes deviceTypes = DeviceTypes.create()
			.with("QUECLINK_GB200S", (byte)0x46);
		BoxonCore core = BoxonCoreBuilder.builder()
			.withContext("deviceTypes", deviceTypes)
			.withContextFunction(ParserTest.class, "headerSize")
			.withDefaultCodecs()
			.withTemplatesFrom(ACKMessageHex.class)
			.create();
		Parser parser = Parser.create(core);
		Composer composer = Composer.create(core);

		//parse:
		byte[] payload = StringHelper.toByteArray("2b41434b066f2446010a0311235e40035110420600ffff07e30405083639001265b60d0a");
		ParseResponse parseResult = parser.parse(payload);

		Assertions.assertFalse(parseResult.hasErrors());
		Assertions.assertEquals(1, parseResult.getParsedMessageCount());

		//compose:
		ComposeResponse<ACKMessageHex> composeResult = composer.composeMessage(parseResult.getParsedMessageAt(0));

		Assertions.assertFalse(composeResult.hasErrors());
		Assertions.assertArrayEquals(payload, composeResult.getComposedMessage());
	}

	@Test
	void parseAndComposeSingleMessageASCII() throws AnnotationException, TemplateException, ConfigurationException{
		DeviceTypes deviceTypes = DeviceTypes.create()
			.with("QUECLINK_GV350M", (byte)0xCF);
		Map<String, Object> context = Collections.singletonMap("deviceTypes", deviceTypes);
		BoxonCore core = BoxonCoreBuilder.builder()
			.withContext(context)
			.withDefaultCodecs()
			.withTemplatesFrom(ACKMessageHex.class)
			.create();
		Parser parser = Parser.create(core);
		Composer composer = Composer.create(core);

		//parse:
		byte[] payload = "+ACK:GTIOB,CF8002,359464038116666,GV350MG,2,0020,20170101123542,11F0$".getBytes(StandardCharsets.ISO_8859_1);
		ParseResponse parseResult = parser.parse(payload);

		Assertions.assertFalse(parseResult.hasErrors());
		Assertions.assertEquals(1, parseResult.getParsedMessageCount());

		//compose:
		ComposeResponse<ACKMessageASCII> composeResult = composer.composeMessage(parseResult.getParsedMessageAt(0));

		Assertions.assertFalse(composeResult.hasErrors());
		Assertions.assertArrayEquals(payload, composeResult.getComposedMessage());
	}

}
