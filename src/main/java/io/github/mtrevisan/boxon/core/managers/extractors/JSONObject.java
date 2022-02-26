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

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;


final class JSONObject implements JSONTypeInterface, Map<String, Object>{

	private final Map<String, Object> map;


	public static JSONObject createUnordered(){
		return new JSONObject(false);
	}

	public static JSONObject createOrdered(){
		return new JSONObject(true);
	}


	private JSONObject(final boolean isOrdered){
		map = (isOrdered? new LinkedHashMap<>(0): new HashMap<>(0));
	}


	@Override
	public int size(){
		return map.size();
	}

	@Override
	public boolean isEmpty(){
		return map.isEmpty();
	}

	@Override
	public boolean containsKey(final Object key){
		return map.containsKey(key);
	}

	@Override
	public boolean containsValue(final Object value){
		return map.containsValue(value);
	}

	@Override
	public Object get(final Object key){
		return map.get(key);
	}

	@Override
	public Object put(final String key, final Object value){
		return map.put(key, value);
	}

	@Override
	public Object remove(final Object key){
		return map.remove(key);
	}

	@Override
	public void putAll(final Map<? extends String, ?> m){
		map.putAll(m);
	}

	@Override
	public void clear(){
		map.clear();
	}

	@Override
	public Set<String> keySet(){
		return map.keySet();
	}

	@Override
	public Collection<Object> values(){
		return map.values();
	}

	@Override
	public Set<Entry<String, Object>> entrySet(){
		return map.entrySet();
	}

	@Override
	public String toString(){
		//FIXME
		return map.toString();
	}

}
