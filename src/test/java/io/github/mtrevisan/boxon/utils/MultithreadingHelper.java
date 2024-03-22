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
package io.github.mtrevisan.boxon.utils;

import java.lang.reflect.Array;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;


public final class MultithreadingHelper{

	private MultithreadingHelper(){}


	public static <T> void testMultithreading(final Callable<? extends T> fun, final Consumer<? super T> combiner, final int threadCount)
			throws ExecutionException, InterruptedException{
		try(final ExecutorService service = Executors.newFixedThreadPool(threadCount)){

			final CountDownLatch latch = new CountDownLatch(1);
			final AtomicBoolean running = new AtomicBoolean();
			final AtomicInteger overlaps = new AtomicInteger();
			@SuppressWarnings("unchecked")
			final Future<T>[] futures = (Future<T>[])Array.newInstance(Future.class, threadCount);
			//assure overlaps happens (cycle until some overlaps happens)
			while(overlaps.get() == 0){
				for(int t = 0; t < threadCount; t++)
					futures[t] = service.submit(() -> {
						latch.await();

						if(!running.compareAndSet(false, true))
							overlaps.incrementAndGet();

						final T result = fun.call();

						running.set(false);

						return result;
					});

				//start all the thread simultaneously
				latch.countDown();
				for(int t = 0; t < threadCount; t ++)
					futures[t].get();
			}

			for(int t = 0; t < threadCount; t ++)
				combiner.accept(futures[t].get());
		}
	}

}
