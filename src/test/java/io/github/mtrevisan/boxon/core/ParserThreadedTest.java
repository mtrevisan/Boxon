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

import io.github.mtrevisan.boxon.codecs.queclink.DeviceTypes;
import io.github.mtrevisan.boxon.exceptions.AnnotationException;
import io.github.mtrevisan.boxon.exceptions.ConfigurationException;
import io.github.mtrevisan.boxon.exceptions.TemplateException;
import io.github.mtrevisan.boxon.internal.StringHelper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;


@SuppressWarnings("ALL")
class ParserThreadedTest{

	private static final byte[] PAYLOAD = StringHelper.toByteArray("2b41434b066f2446010a0311235e40035110420600ffff07e30405083639001265b60d0a2b41434b066f2446010a0311235e40035110420600ffff07e30405083639001265b60d0a");


	@Test
	void concurrencySingleParserSingleCore() throws NoSuchMethodException, TemplateException, ConfigurationException, AnnotationException,
			ExecutionException, InterruptedException{
		DeviceTypes deviceTypes = new DeviceTypes();
		deviceTypes.add("QUECLINK_GB200S", (byte)0x46);
		Map<String, Object> context = Collections.singletonMap("deviceTypes", deviceTypes);
		BoxonCore core = BoxonCoreBuilder.builder()
			.withContext(context)
			.withContextFunction(ParserTest.class.getDeclaredMethod("headerSize"))
			.withDefaultCodecs()
			.withDefaultTemplates()
			.create();
		Parser parser = Parser.create(core);

		AtomicInteger errors = new AtomicInteger();
		testMultithreading(
			() -> parser.parse(PAYLOAD),
			parseResponse -> errors.addAndGet(parseResponse.getErrorCount()),
			10
		);
		Assertions.assertEquals(0, errors.get());
	}

	@Test
	void concurrencyMultipleParserSingleCore() throws NoSuchMethodException, TemplateException, ConfigurationException, AnnotationException,
			ExecutionException, InterruptedException{
		DeviceTypes deviceTypes = new DeviceTypes();
		deviceTypes.add("QUECLINK_GB200S", (byte)0x46);
		Map<String, Object> context = Collections.singletonMap("deviceTypes", deviceTypes);
		BoxonCore core = BoxonCoreBuilder.builder()
			.withContext(context)
			.withContextFunction(ParserTest.class.getDeclaredMethod("headerSize"))
			.withDefaultCodecs()
			.withDefaultTemplates()
			.create();

		AtomicInteger errors = new AtomicInteger();
		testMultithreading(
			() -> {
				Parser parser = Parser.create(core);
				return parser.parse(PAYLOAD);
			},
			parseResponse -> errors.addAndGet(parseResponse.getErrorCount()),
			10
		);
		Assertions.assertEquals(0, errors.get());
	}

	@Test
	void concurrencyMultipleParserMultipleCore() throws NoSuchMethodException, TemplateException, ConfigurationException,
			AnnotationException, ExecutionException, InterruptedException{
		DeviceTypes deviceTypes = new DeviceTypes();
		deviceTypes.add("QUECLINK_GB200S", (byte)0x46);
		Map<String, Object> context = Collections.singletonMap("deviceTypes", deviceTypes);

		AtomicInteger errors = new AtomicInteger();
		testMultithreading(
			() -> {
				BoxonCore core = BoxonCoreBuilder.builder()
					.withContext(context)
					.withContextFunction(ParserTest.class.getDeclaredMethod("headerSize"))
					.withDefaultCodecs()
					.withDefaultTemplates()
					.create();
				Parser parser = Parser.create(core);
				return parser.parse(PAYLOAD);
			},
			parseResponse -> errors.addAndGet(parseResponse.getErrorCount()),
			10
		);
		Assertions.assertEquals(0, errors.get());
	}


	private static <T> void testMultithreading(final Callable<T> fun, final Consumer<T> combiner, final int threadCount)
			throws ExecutionException, InterruptedException{
		final ExecutorService service = Executors.newFixedThreadPool(threadCount);

		final CountDownLatch latch = new CountDownLatch(1);
		final AtomicBoolean running = new AtomicBoolean();
		final AtomicInteger overlaps = new AtomicInteger();
		final Future<T>[] futures = new Future[threadCount];
		//assure overlaps happens (cycle until some overlaps happens)
		while(overlaps.get() == 0){
			for(int t = 0; t < threadCount; t ++)
				futures[t] = service.submit(() -> {
					latch.await();
					if(running.get())
						overlaps.incrementAndGet();

					running.set(true);
					final T result = fun.call();
					running.set(false);

					return result;
				});

			//start all the thread simultaneously
			latch.countDown();

			for(final Future<T> f : futures)
				combiner.accept(f.get());
		}
	}

}
