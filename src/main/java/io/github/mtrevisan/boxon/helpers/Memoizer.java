/**
 * Copyright (c) 2019-2020 Mauro Trevisan
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
package io.github.mtrevisan.boxon.helpers;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;
import java.util.function.Supplier;


public final class Memoizer{

	private static final Object DEFAULT_OBJECT = new Object();


	private Memoizer(){}

	public static <OUT> Supplier<OUT> memoize(final Supplier<OUT> supplier){
		final Map<Object, OUT> cache = new ConcurrentHashMap<>();
		return () -> cache.computeIfAbsent(DEFAULT_OBJECT, t -> supplier.get());
	}

	public static <IN, OUT> Function<IN, OUT> memoize(final Function<IN, OUT> function){
		final Map<IN, OUT> cache = new ConcurrentHashMap<>();
		return input -> cache.computeIfAbsent(input, function);
	}

	/**
	 * Thread-safe and recursion-safe implementation using a re-entrant lock.
	 *
	 * @param <OUT>			Type of input to the function.
	 * @param supplier	The function to be memoized.
	 * @return				The new memoized function.
	 *
	 * @see <a href="https://opencredo.com/lambda-memoization-in-java-8/">Lambda memoization in Java 8</a>
	 */
	public static <OUT> Supplier<OUT> memoizeThreadAndRecursionSafe(final Supplier<OUT> supplier){
		final Map<Object, OUT> cache = new HashMap<>(0);
		final ReentrantLock lock = new ReentrantLock();
		return () -> {
			lock.lock();
			try{
				return cache.computeIfAbsent(DEFAULT_OBJECT, t -> supplier.get());
			}
			finally{
				lock.unlock();
			}
		};
	}

	/**
	 * Thread-safe and recursion-safe implementation using a re-entrant lock.
	 *
	 * @param <IN>			Type of input to the function.
	 * @param <OUT>			Type of output from the function.
	 * @param function	The function to be memoized.
	 * @return				The new memoized function.
	 *
	 * @see <a href="https://opencredo.com/lambda-memoization-in-java-8/">Lambda memoization in Java 8</a>
	 */
	public static <IN, OUT> Function<IN, OUT> memoizeThreadAndRecursionSafe(final Function<IN, OUT> function){
		final Map<IN, OUT> cache = new HashMap<>(0);
		final ReentrantLock lock = new ReentrantLock();
		return input -> {
			lock.lock();
			try{
				return cache.computeIfAbsent(input, function);
			}
			finally{
				lock.unlock();
			}
		};
	}

}
