/**
 * Copyright (c) 2021 Mauro Trevisan
 * <p>
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 * <p>
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */
package io.github.mtrevisan.boxon.codecs.managers;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.function.Function;


final class ValueOf<T extends Enum<T>, K>{

	private static final String ERROR_MESSAGE = "No enum constant %s.%s";


	private final Class<T> type;
	private final Map<K, T> values;


	public static <T extends Enum<T>, K> ValueOf<T, K> create(final Class<T> type, final Function<T, K> fieldAccessor){
		return new ValueOf<>(type, fieldAccessor, null);
	}

	public static <T extends Enum<T>, K> ValueOf<T, K> create(final Class<T> type, final Function<T, K> fieldAccessor,
			final Comparator<K> comparator){
		Objects.requireNonNull(comparator, "Comparator has not to be null");

		return new ValueOf<>(type, fieldAccessor, comparator);
	}

	private ValueOf(final Class<T> type, final Function<T, K> fieldAccessor, final Comparator<K> comparator){
		this.type = type;

		final T[] enumConstants = type.getEnumConstants();
		final Map<K, T> map = (comparator != null? new TreeMap<>(comparator): new HashMap<>(enumConstants.length));
		for(int i = 0; i < enumConstants.length; i ++){
			final K key = fieldAccessor.apply(enumConstants[i]);
			if(map.put(key, enumConstants[i]) != null)
				throw new IllegalStateException("Duplicate key in enum " + type.getSimpleName() + ": " + key);
		}

		values = Collections.unmodifiableMap(map);
	}

	public T get(final K key){
		return values.get(key);
	}

	public T getOrElseThrow(final K key){
		final T value = values.get(key);
		if(value == null)
			throw new IllegalArgumentException(String.format(ERROR_MESSAGE, type.getName(), key));

		return value;
	}

}
