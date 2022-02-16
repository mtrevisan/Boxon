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

import io.github.mtrevisan.boxon.internal.codecs.queclink.ACKMessageHex;
import io.github.mtrevisan.boxon.internal.codecs.queclink.DeviceTypes;
import io.github.mtrevisan.boxon.exceptions.AnnotationException;
import io.github.mtrevisan.boxon.exceptions.ConfigurationException;
import io.github.mtrevisan.boxon.exceptions.TemplateException;
import io.github.mtrevisan.boxon.internal.StringHelper;
import io.github.mtrevisan.boxon.utils.MultithreadingHelper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.concurrent.ExecutionException;


@SuppressWarnings("ALL")
class ComposerThreadedTest{

	private static final byte[] PAYLOAD = StringHelper.toByteArray("2b41434b066f2446010a0311235e40035110420600ffff07e30405083639001265b60d0a");


	@Test
	void concurrencySingleParserSingleCore() throws NoSuchMethodException, AnnotationException, TemplateException, ConfigurationException,
			ExecutionException, InterruptedException{
		DeviceTypes deviceTypes = DeviceTypes.create()
			.with("QUECLINK_GB200S", (byte)0x46);
		BoxonCore core = BoxonCoreBuilder.builder()
			.addToContext("deviceTypes", deviceTypes)
			.withContextFunction(ParserTest.class, "headerSize")
			.withDefaultCodecs()
			.withTemplates(ACKMessageHex.class)
			.create();
		Parser parser = Parser.create(core);
		Composer composer = Composer.create(core);

		//parse:
		ParseResponse parseResult = parser.parse(PAYLOAD);

		//compose:
		int threadCount = 10;
		MultithreadingHelper.testMultithreading(
			() -> composer.composeMessage(parseResult.getParsedMessageAt(0)),
			composeResult -> Assertions.assertArrayEquals(PAYLOAD, composeResult.getComposedMessage()),
			threadCount
		);
	}

	@Test
	void concurrencyMultipleParserSingleCore() throws NoSuchMethodException, AnnotationException, TemplateException, ConfigurationException,
			ExecutionException, InterruptedException{
		DeviceTypes deviceTypes = DeviceTypes.create()
			.with("QUECLINK_GB200S", (byte)0x46);
		BoxonCore core = BoxonCoreBuilder.builder()
			.addToContext("deviceTypes", deviceTypes)
			.withContextFunction(ParserTest.class, "headerSize")
			.withDefaultCodecs()
			.withTemplates(ACKMessageHex.class)
			.create();
		Parser parser = Parser.create(core);

		//parse:
		ParseResponse parseResult = parser.parse(PAYLOAD);

		//compose:
		int threadCount = 10;
		MultithreadingHelper.testMultithreading(
			() -> {
				Composer composer = Composer.create(core);
				return composer.composeMessage(parseResult.getParsedMessageAt(0));
			},
			composeResult -> Assertions.assertArrayEquals(PAYLOAD, composeResult.getComposedMessage()),
			threadCount
		);
	}

	@Test
	void concurrencyMultipleParserMultipleCore() throws NoSuchMethodException, AnnotationException, TemplateException,
			ConfigurationException, ExecutionException, InterruptedException{
		DeviceTypes deviceTypes = DeviceTypes.create()
			.with("QUECLINK_GB200S", (byte)0x46);

		//compose:
		int threadCount = 10;
		MultithreadingHelper.testMultithreading(
			() -> {
				BoxonCore core = BoxonCoreBuilder.builder()
					.addToContext("deviceTypes", deviceTypes)
					.withContextFunction(ParserTest.class, "headerSize")
					.withDefaultCodecs()
					.withTemplates(ACKMessageHex.class)
					.create();
				Parser parser = Parser.create(core);

				//parse:
				ParseResponse parseResult = parser.parse(PAYLOAD);

				Composer composer = Composer.create(core);
				return composer.composeMessage(parseResult.getParsedMessageAt(0));
			},
			composeResult -> Assertions.assertArrayEquals(PAYLOAD, composeResult.getComposedMessage()),
			threadCount
		);
	}

}
