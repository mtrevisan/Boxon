/*
 * Copyright (c) 2021-2022 Mauro Trevisan
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
package io.github.mtrevisan.boxon.core.managers.extractors;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;


final class JSONArray implements JSONTypeInterface, Collection<Object>{


	private final List<Object> list = new ArrayList<>(0);


	public static JSONArray create(){
		return new JSONArray();
	}


	private JSONArray(){}


	@Override
	public int size(){
		return list.size();
	}

	@Override
	public boolean isEmpty(){
		return list.isEmpty();
	}

	@Override
	public boolean contains(final Object o){
		return list.contains(o);
	}

	@Override
	public Iterator<Object> iterator(){
		return list.iterator();
	}

	@Override
	public Object[] toArray(){
		return list.toArray();
	}

	@Override
	public <T> T[] toArray(final T[] a){
		return list.toArray(a);
	}

	@Override
	public boolean add(final Object o){
		return list.add(o);
	}

	@Override
	public boolean remove(final Object o){
		return list.remove(o);
	}

	@Override
	public boolean containsAll(final Collection<?> c){
		return list.containsAll(c);
	}

	@Override
	public boolean addAll(final Collection<?> c){
		return list.addAll(c);
	}

	@Override
	public boolean removeAll(final Collection<?> c){
		return list.removeAll(c);
	}

	@Override
	public boolean retainAll(final Collection<?> c){
		return list.retainAll(c);
	}

	@Override
	public void clear(){
		list.clear();
	}

	@SuppressWarnings("unchecked")
	public <T> T get(final int index){
		return (T)list.get(index);
	}

	@Override
	public String toString(){
		//FIXME
		return Arrays.toString(list.toArray());
	}

}
