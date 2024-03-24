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
package io.github.mtrevisan.boxon.helpers.pools;

import io.github.mtrevisan.boxon.io.ParserDataType;

import java.lang.reflect.Array;
import java.util.HashMap;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;


/** A fixed size Array Pool that evicts arrays using an LRU strategy to keep the pool under the maximum byte size. */
//https://github.com/bumptech
public class LRUArrayPool implements ArrayPool{

	//4MB
	private static final int DEFAULT_POOL_SIZE = 4 * 1024 * 1024;


	/** The maximum number of times larger an int array may be to be than a requested size to eligible to be returned from the pool. */
	static final int MAX_OVER_SIZE_MULTIPLE = 8;
	/** Used to calculate the maximum % of the total pool size a single byte array may consume. */
	private static final int SINGLE_ARRAY_MAX_SIZE_DIVISOR = 2;

	private final GroupedLinkedMap<Key, Object> groupedMap = GroupedLinkedMap.create();
	private final KeyPool keyPool = new KeyPool();
	private final Map<Class<?>, NavigableMap<Integer, Integer>> sortedSizes = new HashMap<>(0);
	private final int maxPoolSize;
	private int currentPoolSize;


	public LRUArrayPool(){
		maxPoolSize = DEFAULT_POOL_SIZE;
	}

	/**
	 * Constructor for a new pool.
	 *
	 * @param maxPoolSize The maximum size in integers of the pool.
	 */
	public LRUArrayPool(final int maxPoolSize){
		this.maxPoolSize = maxPoolSize;
	}


	@Override
	public synchronized <T> void put(final T array){
		final int size = Array.getLength(array);
		@SuppressWarnings("unchecked")
		//TODO verificare se basta getComponentType
		final Class<T> arrayClass = (Class<T>)array.getClass()
			.getComponentType();
		final int arrayBytes = size * ParserDataType.getSize(arrayClass);
		if(isSmallEnoughForReuse(arrayBytes)){
			final Key key = keyPool.get(size, arrayClass);

			groupedMap.put(key, array);
			final NavigableMap<Integer, Integer> sizes = getSizesForAdapter(arrayClass);
			final Integer current = sizes.get(key.size);
			sizes.put(key.size, current == null? 1: current + 1);
			currentPoolSize += arrayBytes;

			evict();
		}
	}

	private boolean isSmallEnoughForReuse(final int byteSize){
		return (byteSize <= maxPoolSize / SINGLE_ARRAY_MAX_SIZE_DIVISOR);
	}

	@Override
	public synchronized <T> T getExact(final int size, final Class<T> arrayClass){
		final Key key = keyPool.get(size, arrayClass);
		return getForKey(key, arrayClass);
	}

	@Override
	public synchronized <T> T get(final int size, final Class<T> arrayClass){
		final Integer possibleSize = getSizesForAdapter(arrayClass)
			.ceilingKey(size);
		final Key key = (mayFillRequest(size, possibleSize)
			? keyPool.get(possibleSize, arrayClass)
			: keyPool.get(size, arrayClass));
		return getForKey(key, arrayClass);
	}

	@SuppressWarnings("unchecked")
	private <T> T getForKey(final Key key, final Class<T> arrayClass){
		T result = getArrayForKey(key);
		if(result != null){
			//TODO verificare se basta getComponentType
			currentPoolSize -= Array.getLength(result) * ParserDataType.getSize(arrayClass.getComponentType());
			decrementArrayOfSize(Array.getLength(result), arrayClass);
		}
		else
			result = (T)Array.newInstance(arrayClass, key.size);
		return result;
	}

	//the cast is safe because the key is based on the type
	@SuppressWarnings({"unchecked", "TypeParameterUnusedInFormals"})
	private <T> T getArrayForKey(final Key key){
		return (T)groupedMap.get(key);
	}

	private boolean mayFillRequest(final int requestedSize, final Integer actualSize){
		return (actualSize != null && (isNoMoreThanHalfFull() || actualSize <= MAX_OVER_SIZE_MULTIPLE * requestedSize));
	}

	private boolean isNoMoreThanHalfFull(){
		return (currentPoolSize == 0 || maxPoolSize >= 2 * currentPoolSize);
	}

	@Override
	public synchronized void clearMemory(){
		evictToSize(0);
	}

	@Override
	public synchronized void trimMemory(final int level){
		if(level >= TRIM_MEMORY_BACKGROUND)
			clearMemory();
		else if(level >= TRIM_MEMORY_UI_HIDDEN || level == TRIM_MEMORY_RUNNING_CRITICAL)
			evictToSize(maxPoolSize / 2);
	}

	private void evict(){
		evictToSize(maxPoolSize);
	}

	private void evictToSize(final int size){
		while(currentPoolSize > size){
			final Object evicted = groupedMap.removeLast();
			assert evicted != null;

			//TODO verificare se basta getComponentType
			currentPoolSize -= Array.getLength(evicted) * ParserDataType.getSize(evicted.getClass().getComponentType());
			decrementArrayOfSize(Array.getLength(evicted), evicted.getClass());
		}
	}

	private void decrementArrayOfSize(final int size, final Class<?> arrayClass){
		final NavigableMap<Integer, Integer> sizes = getSizesForAdapter(arrayClass);
		final Integer current = sizes.get(size);
		if(current == null)
			throw new NullPointerException("Tried to decrement empty size, size: " + size + ", this: " + this);

		if(current == 1)
			sizes.remove(size);
		else
			sizes.put(size, current - 1);
	}

	private NavigableMap<Integer, Integer> getSizesForAdapter(final Class<?> arrayClass){
		return sortedSizes.computeIfAbsent(arrayClass, k -> new TreeMap<>());
	}

	int getCurrentPoolSize(){
		int currentSize = 0;
		for(final Map.Entry<Class<?>, NavigableMap<Integer, Integer>> entry : sortedSizes.entrySet())
			for(final Integer size : entry.getValue().keySet())
				//TODO verificare se basta getComponentType
				currentSize += size * entry.getValue().get(size) * ParserDataType.getSize(entry.getKey().getComponentType());
		return currentSize;
	}

}
