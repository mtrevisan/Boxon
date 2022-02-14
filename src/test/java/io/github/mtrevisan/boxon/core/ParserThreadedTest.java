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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;


@SuppressWarnings("ALL")
class ParserThreadedTest{

	private static final byte[] PAYLOAD = StringHelper.toByteArray("2b41434b066f2446010a0311235e40035110420600ffff07e30405083639001265b60d0a2b41434b066f2446010a0311235e40035110420600ffff07e30405083639001265b60d0a");


	@Test
	void concurrency() throws NoSuchMethodException, TemplateException, ConfigurationException, AnnotationException, ExecutionException,
			InterruptedException{
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

		int threads = 100;
		ExecutorService service = Executors.newFixedThreadPool(threads);

		CountDownLatch latch = new CountDownLatch(1);
		AtomicBoolean running = new AtomicBoolean();
		AtomicInteger overlaps = new AtomicInteger();
		Collection<Future<ParseResponse>> futures = new ArrayList<>(threads);
		//assure overlaps happens
		while(overlaps.get() == 0){
			futures.clear();
			for(int t = 0; t < threads; t ++)
				futures.add(service.submit(() -> {
					latch.await();
					if(running.get())
						overlaps.incrementAndGet();

					running.set(true);
					ParseResponse result = parser.parse(PAYLOAD);
					running.set(false);
					return result;
				}));

			latch.countDown();

			int errors = 0;
			for(Future<ParseResponse> f : futures)
				errors += f.get().getErrorCount();
			Assertions.assertEquals(0, errors);
		}
	}

}
