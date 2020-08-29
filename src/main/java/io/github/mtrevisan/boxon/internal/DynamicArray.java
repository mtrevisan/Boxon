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
package io.github.mtrevisan.boxon.internal;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.StringJoiner;
import java.util.function.Function;
import java.util.function.Predicate;


public final class DynamicArray<T>{

	private static final float DEFAULT_GROWTH_RATE = 1.2f;


	public T[] data;
	public int limit;

	private final float growthRate;


	public static <T> DynamicArray<T> wrap(final T[] array){
		return new DynamicArray<>(array);
	}

	public static <T> DynamicArray<T> create(final Class<T> type){
		return new DynamicArray<>(type, 0, DEFAULT_GROWTH_RATE);
	}

	public static <T> DynamicArray<T> create(final Class<T> type, final int capacity){
		return new DynamicArray<>(type, capacity, DEFAULT_GROWTH_RATE);
	}

	public static <T> DynamicArray<T> create(final Class<T> type, final int capacity, final float growthRate){
		return new DynamicArray<>(type, capacity, growthRate);
	}

	private DynamicArray(final T[] array){
		data = array;
		limit = array.length;

		growthRate = DEFAULT_GROWTH_RATE;
	}

	@SuppressWarnings("unchecked")
	private DynamicArray(final Class<T> type, final int capacity, final float growthRate){
		data = (T[])Array.newInstance(type, capacity);

		this.growthRate = growthRate;
	}

	/**
	 * Appends the specified element to the end of this array.
	 *
	 * @param elem	Element to be appended to the internal array.
	 */
	public void add(final T elem){
		grow(1);

		data[limit ++] = elem;
	}

	/**
	 * Appends all of the elements in the specified collection to the end of this array.
	 *
	 * @param array	Collection containing elements to be added to this array.
	 */
	public void addAll(final DynamicArray<T> array){
		grow(array.limit);

		System.arraycopy(array.data, 0, data, limit, array.limit);
		limit += array.limit;
	}

	/**
	 * Inserts all of the elements in the specified collection into this array at the specified position.
	 * <p>Shifts the element currently at that position (if any) and any subsequent elements to the right
	 * (increases their indices).</p>
	 *
	 * @param index	Index at which to insert the first element from the specified collection.
	 * @param array	Collection containing elements to be added to this array.
	 */
	public void addAll(final int index, final DynamicArray<T> array){
		final int addLength = array.limit;
		if(addLength != 0){
			grow(addLength);

			if(index < limit)
				System.arraycopy(data, index, data, index + addLength, limit - index);
			System.arraycopy(array.data, 0, data, index, addLength);
			limit += addLength;
		}
	}

	/**
	 * Increases the capacity of the internal array, if necessary, to ensure that it can hold at least the number of elements
	 * specified by the minimum capacity argument.
	 *
	 * @param newCapacity	The desired minimum capacity.
	 */
	public void ensureCapacity(final int newCapacity){
		grow(newCapacity - limit);
	}

	public void filter(final Predicate<? super T> filter){
		reset();
		for(final T elem : data)
			if(filter.test(elem))
				data[limit ++] = elem;
	}

	public void join(final Function<? super T, String> reducer, final StringJoiner joiner){
		for(int i = 0; i < limit; i ++)
			joiner.add(reducer.apply(data[i]));
	}

	private void grow(final int size){
		final int delta = limit - data.length + size;
		if(delta > 0){
			final int newLength = data.length + (int)Math.ceil(delta * growthRate);
			final T[] copy = newInstance(newLength);
			System.arraycopy(data, 0, copy, 0, Math.min(data.length, newLength));
			data = copy;
		}
	}

	/**
	 * Returns whether this array contains no elements.
	 *
	 * @return	Whether this array contains no elements.
	 */
	public boolean isEmpty(){
		return (limit == 0);
	}

	/** Removes all of the elements from this array. */
	public void reset(){
		limit = 0;
	}

	/**
	 * Removes all of the elements from this array.
	 * <p>The array will be emptied after this call returns.</p>
	 */
	public void clear(){
		data = null;
		limit = -1;
	}

	/**
	 * NOTE: this method should be called the least possible because it is inefficient.
	 *
	 * @return	A copy of the array.
	 */
	public T[] extractCopy(){
		final T[] copy = newInstance(limit);
		System.arraycopy(data, 0, copy, 0, limit);
		return copy;
	}

	@SuppressWarnings("unchecked")
	private T[] newInstance(final int size){
		final Class<?> type = getDataType();
		return (T[])Array.newInstance(type, size);
	}

	private Class<?> getDataType(){
		return data.getClass().getComponentType();
	}

	@Override
	public String toString(){
		return Arrays.toString(data);
	}

	@Override
	public boolean equals(final Object obj){
		if(obj == this)
			return true;
		if(obj == null || obj.getClass() != getClass())
			return false;

		final DynamicArray<?> rhs = (DynamicArray<?>)obj;
		return (limit == rhs.limit && Arrays.equals(data, rhs.data));
	}

	@Override
	public int hashCode(){
		return Integer.hashCode(limit) ^ Arrays.hashCode(data);
	}

}
