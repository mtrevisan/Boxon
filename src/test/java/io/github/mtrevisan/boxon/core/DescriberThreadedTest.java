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

import io.github.mtrevisan.boxon.core.codecs.queclink.ACKMessageHex;
import io.github.mtrevisan.boxon.core.codecs.queclink.DeviceTypes;
import io.github.mtrevisan.boxon.utils.MultithreadingHelper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;


class DescriberThreadedTest{

	@Test
	void concurrencySingleParserSingleCore() throws Exception{
		DeviceTypes<Byte> deviceTypes = DeviceTypes.<Byte>create()
			.with((byte)0x46, "QUECLINK_GB200S");
		Core core = CoreBuilder.builder()
			.withContext("deviceTypes", deviceTypes)
			.withContext(ParserTest.class.getDeclaredMethod("headerLength"))
			.withDefaultCodecs()
			.withTemplate(ACKMessageHex.class)
			.build();
		Describer describer = Describer.create(core);

		int threadCount = 10;
		AtomicInteger counter = new AtomicInteger();
		MultithreadingHelper.testMultithreading(describer::describeTemplate,
			descriptions -> counter.addAndGet(descriptions.size()),
			threadCount
		);

		Assertions.assertEquals(threadCount, counter.get());
	}

	@Test
	void concurrencyMultipleParserSingleCore() throws Exception{
		DeviceTypes<Byte> deviceTypes = DeviceTypes.<Byte>create()
			.with((byte)0x46, "QUECLINK_GB200S");
		Core core = CoreBuilder.builder()
			.withContext("deviceTypes", deviceTypes)
			.withContext(ParserTest.class.getDeclaredMethod("headerLength"))
			.withDefaultCodecs()
			.withTemplate(ACKMessageHex.class)
			.build();

		int threadCount = 10;
		AtomicInteger counter = new AtomicInteger();
		MultithreadingHelper.testMultithreading(
			() -> {
				Describer describer = Describer.create(core);
				return describer.describeTemplate();
			},
			descriptions -> counter.addAndGet(descriptions.size()),
			threadCount
		);

		Assertions.assertEquals(threadCount, counter.get());
	}

	@Test
	void concurrencyMultipleParserMultipleCore() throws Exception{
		DeviceTypes<Byte> deviceTypes = DeviceTypes.<Byte>create()
			.with((byte)0x46, "QUECLINK_GB200S");

		int threadCount = 10;
		AtomicInteger counter = new AtomicInteger();
		MultithreadingHelper.testMultithreading(
			() -> {
				Core core = CoreBuilder.builder()
					.withContext("deviceTypes", deviceTypes)
					.withContext(ParserTest.class.getDeclaredMethod("headerLength"))
					.withDefaultCodecs()
					.withTemplate(ACKMessageHex.class)
					.build();
				Describer describer = Describer.create(core);
				return describer.describeTemplate();
			},
			descriptions -> counter.addAndGet(descriptions.size()),
			threadCount
		);

		Assertions.assertEquals(threadCount, counter.get());
	}

}
