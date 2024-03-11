/*
 * Copyright (c) 2021-2024 Mauro Trevisan
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
package io.github.mtrevisan.boxon.core.helpers;

import java.util.Collections;
import java.util.Comparator;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.function.Function;


/**
 * Helper used for fast retrieval of enum.
 *
 * @param <T>	The enum class type.
 * @param <K>	The value class type.
 */
public final class ValueOf<T extends Enum<T>, K>{

	private final Class<T> type;
	private final Map<K, T> values;


	/**
	 * Create an instance given the enum and a field accessor.
	 *
	 * @param type	The class type of the enum.
	 * @param fieldAccessor	The field accessor whose value is to be considered.
	 * @param <T>	The enum class type.
	 * @param <K>	The value class type.
	 * @return	An instance.
	 */
	public static <T extends Enum<T>, K> ValueOf<T, K> create(final Class<T> type, final Function<T, K> fieldAccessor){
		return new ValueOf<>(type, fieldAccessor, null);
	}

	/**
	 * Create an instance given the enum and a field accessor.
	 *
	 * @param type	The class type of the enum.
	 * @param fieldAccessor	The field accessor whose value is to be considered.
	 * @param comparator	A comparator user to compare values.
	 * @param <T>	The enum class type.
	 * @param <K>	The value class type.
	 * @return	An instance.
	 */
	public static <T extends Enum<T>, K> ValueOf<T, K> create(final Class<T> type, final Function<T, K> fieldAccessor,
			final Comparator<K> comparator){
		Objects.requireNonNull(comparator, "Comparator has not to be null");

		return new ValueOf<>(type, fieldAccessor, comparator);
	}


	private ValueOf(final Class<T> type, final Function<T, K> fieldAccessor, final Comparator<K> comparator){
		this.type = type;

		final T[] enumConstants = type.getEnumConstants();
		final Map<K, T> map = createMap(comparator, enumConstants);
		for(int i = 0, length = enumConstants.length; i < length; i ++){
			final K key = fieldAccessor.apply(enumConstants[i]);
			if(map.put(key, enumConstants[i]) != null)
				throw new IllegalStateException("Duplicate key in enum " + type.getSimpleName() + ": " + key);
		}

		values = Collections.unmodifiableMap(map);
	}

	private Map<K, T> createMap(final Comparator<K> comparator, final T[] enumConstants){
		return (comparator != null
			? new ConcurrentSkipListMap<>(comparator)
			: new ConcurrentHashMap<>(enumConstants.length));
	}

	/**
	 * Return the enum value corresponding to the given value.
	 *
	 * @param key	The value.
	 * @return	The enum.
	 */
	public T get(final K key){
		return values.get(key);
	}

	/**
	 * Return the enum value corresponding to the given value.
	 *
	 * @param key	The value.
	 * @return	The enum.
	 * @throws IllegalArgumentException	If no enum exists.
	 */
	public T getOrElseThrow(final K key){
		final T value = values.get(key);
		if(value == null)
			throw new IllegalArgumentException("No enum constant " + type.getName() + "." + key);

		return value;
	}

}
