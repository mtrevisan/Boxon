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


class GroupedLinkedMapTest{

	private GroupedLinkedMap<Key, Object> map = GroupedLinkedMap.create();


	@Test
	void testReturnsNullForGetWithNoBitmap(){
		Key key = new Key("key", /* width= */ 1, /* height= */ 1);

		Assertions.assertNull(map.get(key));
	}

	@Test
	void testCanAddAndRemoveABitmap(){
		Key key = new Key("key", 1, 1);
		Object expected = new Object();

		map.put(key, expected);

		Assertions.assertEquals(expected, map.get(key));
	}

	@Test
	void testCanAddAndRemoveMoreThanOneBitmapForAGivenKey(){
		Key key = new Key("key", 1, 1);
		Integer value = 20;

		int numToAdd = 10;

		for(int i = 0; i < numToAdd; i++){
			map.put(key, value);
		}

		for(int i = 0; i < numToAdd; i++){
			Assertions.assertEquals(value, map.get(key));
		}
	}

	@Test
	void testLeastRecentlyRetrievedKeyIsLeastRecentlyUsed(){
		Key firstKey = new Key("key", 1, 1);
		Integer firstValue = 10;
		map.put(firstKey, firstValue);
		map.put(firstKey, firstValue);

		Key secondKey = new Key("key", 2, 2);
		Integer secondValue = 20;
		map.put(secondKey, secondValue);

		map.get(firstKey);

		Assertions.assertEquals(secondValue, map.removeLast());
	}

	@Test
	void testAddingAnEntryDoesNotMakeItMostRecentlyUsed(){
		Key firstKey = new Key("key", 1, 1);
		Integer firstValue = 10;

		map.put(firstKey, firstValue);
		map.put(firstKey, firstValue);

		map.get(firstKey);

		Integer secondValue = 20;
		map.put(new Key("key", 2, 2), secondValue);

		Assertions.assertEquals(secondValue, map.removeLast());
	}

	private static final class Key implements Poolable{

		private final String key;
		private final int width;
		private final int height;

		Key(String key, int width, int height){
			this.key = key;
			this.width = width;
			this.height = height;
		}

		@Override
		public boolean equals(Object o){
			if(o instanceof Key){
				Key other = (Key)o;
				return key.equals(other.key) && width == other.width && height == other.height;
			}
			return false;
		}

		@Override
		public int hashCode(){
			int result = key != null? key.hashCode(): 0;
			result = 31 * result + width;
			result = 31 * result + height;
			return result;
		}

		@Override
		public void offer(){
			// Do nothing.
		}
	}

}