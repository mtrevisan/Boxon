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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * Similar to {@link java.util.LinkedHashMap} when access ordered except that it is access ordered
 * on groups of bitmaps rather than individual objects. The idea is to be able to find the LRU
 * bitmap size, rather than the LRU bitmap object. We can then remove bitmaps from the least
 * recently used size of bitmap when we need to reduce our cache size.
 *
 * <p>For the purposes of the LRU, we count gets for a particular size of bitmap as an access, even
 * if no bitmaps of that size are present. We do not count addition or removal of bitmaps as an access.
 */
public final class GroupedLinkedMap<K extends Poolable, V>{

	private final LinkedEntry<K, V> head = new LinkedEntry<>();
	private final Map<K, LinkedEntry<K, V>> keyToEntry = new HashMap<>(0);


	public static <K extends Poolable, V> GroupedLinkedMap<K, V> create(){
		return new GroupedLinkedMap<>();
	}


	private GroupedLinkedMap(){}


	public void put(final K key, final V value){
		LinkedEntry<K, V> entry = keyToEntry.get(key);

		if(entry == null){
			entry = new LinkedEntry<>(key);
			makeTail(entry);
			keyToEntry.put(key, entry);
		}
		else
			key.offer();

		entry.add(value);
	}

	public V get(final K key){
		LinkedEntry<K, V> entry = keyToEntry.get(key);
		if(entry == null){
			entry = new LinkedEntry<>(key);
			keyToEntry.put(key, entry);
		}
		else
			key.offer();

		makeHead(entry);

		return entry.removeLast();
	}

	public V removeLast(){
		LinkedEntry<K, V> last = head.prev;
		while(!last.equals(head)){
			final V removed = last.removeLast();
			if(removed != null)
				return removed;

			// We will clean up empty lru entries since they are likely to have been one off or
			// unusual sizes and
			// are not likely to be requested again so the gc thrash should be minimal. Doing so will
			// speed up our
			// removeLast operation in the future and prevent our linked list from growing to
			// arbitrarily large
			// sizes.
			removeEntry(last);
			keyToEntry.remove(last.key);
			last.key.offer();

			last = last.prev;
		}

		return null;
	}

	@Override
	public String toString(){
		final StringBuilder sb = new StringBuilder("GroupedLinkedMap( ");
		LinkedEntry<K, V> current = head.next;
		boolean hadAtLeastOneItem = false;
		while(!current.equals(head)){
			hadAtLeastOneItem = true;
			sb.append('{').append(current.key).append(':').append(current.size()).append("}, ");
			current = current.next;
		}
		if(hadAtLeastOneItem)
			sb.delete(sb.length() - 2, sb.length());
		return sb.append(" )").toString();
	}

	// Make the entry the most recently used item.
	private void makeHead(final LinkedEntry<K, V> entry){
		removeEntry(entry);
		entry.prev = head;
		entry.next = head.next;
		updateEntry(entry);
	}

	// Make the entry the least recently used item.
	private void makeTail(final LinkedEntry<K, V> entry){
		removeEntry(entry);
		entry.prev = head.prev;
		entry.next = head;
		updateEntry(entry);
	}

	private static <K, V> void updateEntry(final LinkedEntry<K, V> entry){
		entry.next.prev = entry;
		entry.prev.next = entry;
	}

	private static <K, V> void removeEntry(final LinkedEntry<K, V> entry){
		entry.prev.next = entry.next;
		entry.next.prev = entry.prev;
	}


	private static class LinkedEntry<K, V>{
//		@Synthetic
		final K key;
		private List<V> values;
		LinkedEntry<K, V> next;
		LinkedEntry<K, V> prev;

		// Used only for the first item in the list which we will treat specially and which will not
		// contain a value.
		LinkedEntry(){
			this(null);
		}

		LinkedEntry(final K key){
			next = prev = this;
			this.key = key;
		}

		final V removeLast(){
			final int valueSize = size();
			return (valueSize > 0? values.remove(valueSize - 1): null);
		}

		final int size(){
			return (values != null? values.size(): 0);
		}

		final void add(final V value){
			if(values == null)
				values = new ArrayList<>(1);
			values.add(value);
		}
	}

}
