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
import java.util.List;


/**
 * An implementation of <a href="https://tools.ietf.org/html/rfc6901">RFC6901 - JavaScript Object Notation (JSON) Pointer</a>.
 */
public final class JSONPath{

	private static final char SLASH = '/';
	private static final char TILDE = '~';


	private JSONPath(){}


	@SuppressWarnings("unchecked")
	public static <T> T extract(final String path, final JSONTypeInterface json){
		return (path == null || !path.isEmpty()? extract(path, parse(path), json, 0): (T)json);
	}

	@SuppressWarnings("unchecked")
	private static <T> T extract(final String raw, final List<String> path, final Object json, int index){
		final int len = path.size();
		if(index == len)
			return (T)json;

		if(index > len)
			throw JSONPathException.create("unmatched path ['{}']", raw);
		if(!(json instanceof JSONTypeInterface))
			throw JSONPathException.create("unmatched path ['{}']", raw);

		final String p = path.get(index);
		if(json instanceof JSONArray){
			final JSONArray array = (JSONArray)json;
			try{
				final int idx = Integer.parseInt(p);
				return extract(raw, path, array.get(idx), ++ index);
			}
			catch(final Exception e){
				throw JSONPathException.create(e, "unmatched path ['{}'] at ['{}'], expect a valid number.", raw, p);
			}
		}

		if(json instanceof JSONObject){
			final JSONObject obj = (JSONObject)json;
			if(!obj.containsKey(p))
				throw JSONPathException.create("not found path ['{}'] at ['{}']", raw, p);

			return extract(raw, path, obj.get(p), ++ index);
		}

		throw JSONPathException.create("unmatched path ['{}'] at ['{}']", raw, p);
	}

	private static List<String> parse(final String path){
		if(path == null || path.charAt(0) != SLASH)
			throw JSONPathException.create("invalid path ['{}']", path);

		final char[] array = path.toCharArray();
		final List<String> r = new ArrayList<>(array.length);
		final StringBuilder sb = new StringBuilder();
		char prev = SLASH;
		for(int i = 1; i < array.length; i ++){
			final char c = array[i];
			switch(c){
				case TILDE:
					break;

				case SLASH:
					if(prev == TILDE)
						sb.append(TILDE);
					r.add(sb.toString());
					sb.setLength(0);
					break;

				case '0':
					sb.append(prev == TILDE? TILDE: c);
					break;

				case '1':
					sb.append(prev == TILDE? SLASH: c);
					break;

				default:
					if(prev == TILDE)
						sb.append(TILDE);
					sb.append(c);
			}
			prev = c;
		}
		if(prev == TILDE)
			sb.append(TILDE);
		r.add(sb.toString());
		return r;
	}

}
