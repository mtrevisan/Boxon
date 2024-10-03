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
package io.github.mtrevisan.boxon.helpers;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;


/**
 * A wrapper around a calculation that takes a parameter and returns a result.
 * <br />
 * The results for the calculation will be cached for future requests, with a maximum cache size if specified.
 */
public final class Memoizer{

	private Memoizer(){}


	/**
	 * Thread-safe and recursion-safe implementation using a re-entrant lock.
	 *
	 * @param function	The function to be memoized.
	 * @param <IN>	Type of input to the function. The class MUST implement {@code equals(Object)} and {@code hashCode()}.
	 * @param <OUT>	Type of output from the function.
	 * @return	The new memoized function.
	 *
	 * @see <a href="https://opencredo.com/lambda-memoization-in-java-8/">Lambda memoization in Java 8</a>
	 */
	public static <IN, OUT> Function<IN, OUT> memoize(final Function<? super IN, ? extends OUT> function){
		return memoize(function, -1);
	}

	/**
	 * Thread-safe and recursion-safe implementation using a re-entrant lock.
	 *
	 * @param function	The function to be memoized.
	 * @param maxSize	The maximum number of cache entries.
	 * @param <IN>	Type of input to the function. The class MUST implement {@code equals(Object)} and {@code hashCode()}.
	 * @param <OUT>	Type of output from the function.
	 * @return	The new memoized function.
	 *
	 * @see <a href="https://opencredo.com/lambda-memoization-in-java-8/">Lambda memoization in Java 8</a>
	 */
	public static <IN, OUT> Function<IN, OUT> memoize(final Function<? super IN, ? extends OUT> function, final int maxSize){
		return new Function<>(){
			private final Map<IN, OUT> cache = createCache(maxSize);
			private final Lock lock = new ReentrantLock();

			@Override
			public OUT apply(final IN input){
				lock.lock();
				try{
					return cache.computeIfAbsent(input, function);
				}
				finally{
					lock.unlock();
				}
			}
		};
	}

	/**
	 * Thread-safe and recursion-safe implementation using a re-entrant lock.
	 *
	 * @param function	The function to be memoized.
	 * @param <IN>	Type of input to the function. The class MUST implement {@code equals(Object)} and {@code hashCode()}.
	 * @param <OUT>	Type of output from the function.
	 * @param <E>	Type of exception thrown by the function.
	 * @return	The new memoized function.
	 *
	 * @see <a href="https://opencredo.com/lambda-memoization-in-java-8/">Lambda memoization in Java 8</a>
	 */
	public static <IN, OUT, E extends Exception> ThrowingFunction<IN, OUT, E> throwingMemoize(
			final ThrowingFunction<? super IN, ? extends OUT, ? extends E> function){
		return throwingMemoize(function, -1);
	}

	/**
	 * Thread-safe and recursion-safe implementation using a re-entrant lock.
	 *
	 * @param function	The function to be memoized.
	 * @param maxSize	The maximum number of cache entries.
	 * @param <IN>	Type of input to the function. The class MUST implement {@code equals(Object)} and {@code hashCode()}.
	 * @param <OUT>	Type of output from the function.
	 * @param <E>	Type of exception thrown by the function.
	 * @return	The new memoized function.
	 *
	 * @see <a href="https://opencredo.com/lambda-memoization-in-java-8/">Lambda memoization in Java 8</a>
	 */
	public static <IN, OUT, E extends Exception> ThrowingFunction<IN, OUT, E> throwingMemoize(
			final ThrowingFunction<? super IN, ? extends OUT, ? extends E> function, final int maxSize){
		return new ThrowingFunction<>(){
			private final Map<IN, OUT> cache = createCache(maxSize);
			private final Lock lock = new ReentrantLock();

			@Override
			public OUT apply(final IN input) throws E{
				lock.lock();
				try{
					OUT value = cache.get(input);
					if(value == null){
						value = function.apply(input);
						cache.put(input, value);
					}
					return value;
				}
				finally{
					lock.unlock();
				}
			}
		};
	}


	/**
	 * Creates a thread-safe cache with a maximum size that removes the oldest entries when the size limit is exceeded.
	 *
	 * @param maxSize	The maximum number of cache entries.
	 * @param <K>	The type of keys maintained by the cache.
	 * @param <V>	The type of mapped values.
	 * @return	A map that removes the oldest entries when the cache is full.
	 */
	private static <K, V> Map<K, V> createCache(final int maxSize){
		if(maxSize <= 0)
			return new ConcurrentHashMap<>(0);

		return new LinkedHashMap<>(maxSize, 0.75f, true){
			@Override
			protected boolean removeEldestEntry(final Map.Entry<K, V> eldest){
				return (size() > maxSize);
			}
		};
	}

}
