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

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.StringJoiner;
import java.util.function.Function;


public class SimpleDynamicArray<T>{

	private static final float DEFAULT_GROWTH_RATE = 1.2f;


	public T[] data;
	public int limit;
	private final float growthRate;


	public static <T> SimpleDynamicArray<T> create(final Class<T> type){
		return new SimpleDynamicArray<>(type, 0, DEFAULT_GROWTH_RATE);
	}

	public static <T> SimpleDynamicArray<T> create(final Class<T> type, final int capacity){
		return new SimpleDynamicArray<>(type, capacity, DEFAULT_GROWTH_RATE);
	}

	@SuppressWarnings("unchecked")
	private SimpleDynamicArray(final Class<T> type, final int capacity, final float growthRate){
		data = (T[])Array.newInstance(type, capacity);

		this.growthRate = growthRate;
	}

	public synchronized void add(final T elem){
		grow(1);

		data[limit ++] = elem;
	}

	public synchronized void addAll(final T[] array){
		addAll(array, array.length);
	}

	public synchronized void addAll(final SimpleDynamicArray<T> array){
		addAll(array.data, array.limit);
	}

	private void addAll(final T[] array, final int size){
		grow(size);

		System.arraycopy(array, 0, data, limit, size);
		limit += size;
	}

	public void ensureCapacity(final int newCapacity){
		grow(newCapacity - limit);
	}

	private void grow(final int size){
		final int delta = limit - data.length + size;
		if(delta > 0)
			data = Arrays.copyOf(data, data.length + (int)Math.ceil(delta * growthRate));
	}

	private Class<?> getDataType(){
		return data.getClass().getComponentType();
	}

	public synchronized T get(final int index){
		return data[index];
	}

	public synchronized int length(){
		return limit;
	}

	public synchronized boolean isEmpty(){
		return (limit == 0);
	}

	public synchronized void reset(){
		limit = 0;
	}

	public synchronized void clear(){
		data = null;
		limit = -1;
	}

	public synchronized StringJoiner join(final Function<T, String> reducer, final StringJoiner joiner){
		for(int i = 0; i < limit; i ++)
			joiner.add(reducer.apply(data[i]));
		return joiner;
	}

	@Override
	public synchronized boolean equals(final Object obj){
		if(obj == this)
			return true;
		if(obj == null || obj.getClass() != getClass())
			return false;

		final SimpleDynamicArray<?> rhs = (SimpleDynamicArray<?>)obj;
		return (limit == rhs.limit && Arrays.equals(data, rhs.data));
	}

	@Override
	public synchronized int hashCode(){
		return Integer.hashCode(limit) ^ Arrays.hashCode(data);
	}

}
