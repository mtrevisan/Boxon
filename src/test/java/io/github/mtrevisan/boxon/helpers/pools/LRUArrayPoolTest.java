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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;


class LRUArrayPoolTest{

	private static final int MAX_SIZE = 10;
	private static final int MAX_PUT_SIZE = MAX_SIZE / 2;
	private static final Class<byte[]> ARRAY_CLASS = byte[].class;

	private LRUArrayPool pool = new LRUArrayPool(MAX_SIZE);


	@Test
	public void testNewPoolIsEmpty(){
		Assertions.assertEquals(pool.getCurrentSize(), 0);
	}

	@Test
	public void testICanAddAndGetValidArray(){
		int size = 758;
		int value = 564;
		fillPool(pool, size - 1, value);
		pool.put(createArray(ARRAY_CLASS, size, value));
		Object array = pool.get(size, ARRAY_CLASS);

		Assertions.assertNotNull(array);
		Assertions.assertTrue(array.getClass() == ARRAY_CLASS);
		Assertions.assertTrue(ADAPTER.getArrayLength((byte[])array) >= size);
		Assertions.assertTrue(((byte[])array)[0] == (byte)0);
	}

	@Test
	public void testItIsSizeLimited(){
		fillPool(pool, MAX_SIZE / ADAPTER.getElementSizeInBytes() + 1, 1);

		Assertions.assertTrue(pool.getCurrentSize() <= MAX_SIZE);
	}

	@Test
	public void testArrayLargerThanPoolIsNotAdded(){
		pool = new LRUArrayPool(MAX_SIZE);
		pool.put(createArray(ARRAY_CLASS, MAX_SIZE / ADAPTER.getElementSizeInBytes() + 1, 0));

		Assertions.assertEquals(0, pool.getCurrentSize());
	}

	@Test
	public void testClearMemoryRemovesAllArrays(){
		fillPool(pool, MAX_SIZE / ADAPTER.getElementSizeInBytes() + 1, 0);
		pool.clearMemory();

		Assertions.assertEquals(0, pool.getCurrentSize());
	}

	@Test
	public void testTrimMemoryUiHiddenOrLessRemovesHalfOfArrays(){
		testTrimMemory(MAX_SIZE, TRIM_MEMORY_UI_HIDDEN, MAX_SIZE / 2);
	}

	@Test
	public void testTrimMemoryRunningCriticalRemovesHalfOfBitmaps(){
		testTrimMemory(MAX_SIZE, TRIM_MEMORY_RUNNING_CRITICAL, MAX_SIZE / 2);
	}

	@Test
	public void testTrimMemoryUiHiddenOrLessRemovesNoArraysIfPoolLessThanHalfFull(){
		testTrimMemory(MAX_SIZE / 2, TRIM_MEMORY_UI_HIDDEN, MAX_SIZE / 2);
	}

	@Test
	public void testTrimMemoryBackgroundOrGreaterRemovesAllArrays(){
		for(int trimLevel : new int[]{TRIM_MEMORY_BACKGROUND, TRIM_MEMORY_COMPLETE}){
			testTrimMemory(MAX_SIZE, trimLevel, 0);
		}
	}

	@Test
	public void get_withEmptyPool_returnsExactArray(){
		Assertions.assertEquals(MAX_PUT_SIZE, pool.get(MAX_PUT_SIZE, byte[].class).length);
	}

	@Test
	public void get_withPoolContainingLargerArray_returnsLargerArray(){
		byte[] expected = new byte[MAX_PUT_SIZE];
		pool.put(expected);

		Assertions.assertEquals(expected, pool.get(MAX_PUT_SIZE - 1, byte[].class));
	}

	@Test
	public void get_withPoolContainingSmallerArray_returnsExactArray(){
		pool.put(new byte[MAX_PUT_SIZE - 1]);

		Assertions.assertEquals(MAX_PUT_SIZE, pool.get(MAX_PUT_SIZE, byte[].class).length);
	}

	@Test
	public void get_withPoolLessThanHalfFull_returnsFromPools(){
		int size = MAX_SIZE / 2;
		byte[] expected = new byte[size];
		pool.put(expected);

		Assertions.assertEquals(expected, pool.get(1, byte[].class));
	}

	@Test
	public void get_withPoolMoreThanHalfFull_sizeMoreThanHalfArrayInPool_returnsArray(){
		Set<byte[]> expected = new HashSet<>();
		for(int i = 0; i < 3; i++){
			byte[] toPut = new byte[MAX_SIZE / 3];
			expected.add(toPut);
			pool.put(toPut);
		}
		byte[] received = pool.get(2, byte[].class);

		Assertions.assertTrue(expected.contains(received));
	}

	@Test
	public void get_withPoolMoreThanHalfFull_sizeLessThanHalfArrayInPool_returnsNewArray(){
		pool = new LRUArrayPool(100);
		for(int i = 0; i < 3; i++){
			byte[] toPut = new byte[100 / 3];
			pool.put(toPut);
		}
		int requestedSize = 100 / 3 / LRUArrayPool.MAX_OVER_SIZE_MULTIPLE;
		byte[] received = pool.get(requestedSize, byte[].class);

		Assertions.assertEquals(requestedSize, received.length);
	}

	@Test
	public void getExact_withEmptyPool_returnsExactArray(){
		byte[] result = pool.getExact(MAX_PUT_SIZE, byte[].class);

		Assertions.assertEquals(MAX_PUT_SIZE, result.length);
	}

	@Test
	public void getExact_withPoolContainingLargerArray_returnsExactArray(){
		pool.put(new byte[MAX_PUT_SIZE]);
		int expectedSize = MAX_PUT_SIZE - 1;

		Assertions.assertEquals(expectedSize, pool.getExact(expectedSize, byte[].class).length);
	}

	@Test
	public void getExact_withPoolContainingSmallerArray_returnsExactArray(){
		pool.put(new byte[MAX_PUT_SIZE - 1]);

		Assertions.assertEquals(MAX_PUT_SIZE, pool.getExact(MAX_PUT_SIZE, byte[].class).length);
	}

	@Test
	public void getExact_withPoolContainingExactArray_returnsArray(){
		byte[] expected = new byte[MAX_PUT_SIZE];
		pool.put(expected);

		Assertions.assertEquals(expected, pool.getExact(MAX_PUT_SIZE, byte[].class));
	}

	@Test
	public void put_withArrayMoreThanHalfPoolSize_doesNotRetainArray(){
		int targetSize = (MAX_SIZE / 2) + 1;
		byte[] toPut = new byte[targetSize];
		pool.put(toPut);

		Assertions.assertEquals(0, pool.getCurrentSize());
		Assertions.assertNotEquals(toPut, pool.get(targetSize, byte[].class));
	}

	private void testTrimMemory(int fillSize, int trimLevel, int expectedSize){
		pool = new LRUArrayPool(MAX_SIZE);
		fillPool(pool, fillSize / ADAPTER.getElementSizeInBytes(), 1);
		pool.trimMemory(trimLevel);

		Assertions.assertEquals("Failed level=" + trimLevel, expectedSize, pool.getCurrentSize());
	}

	private void fillPool(LRUArrayPool pool, int arrayCount, int arrayLength){
		for(int i = 0; i < arrayCount; i++){
			pool.put(createArray(ARRAY_CLASS, arrayLength, 10));
		}
	}

	@SuppressWarnings("unchecked")
	private static <T> T createArray(Class<T> type, int size, int value){
		Object array = null;
		if(type.equals(int[].class)){
			array = new int[size];
			Arrays.fill((int[])array, value);
		}
		else if(type.equals(byte[].class)){
			array = new byte[size];
			Arrays.fill((byte[])array, (byte)value);
		}
		return (T)array;
	}

}