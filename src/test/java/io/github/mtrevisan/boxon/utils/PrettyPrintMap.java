/*
 * Copyright (c) 2020-2024 Mauro Trevisan
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
package io.github.mtrevisan.boxon.utils;

import java.util.Arrays;
import java.util.Map;


public final class PrettyPrintMap{

	private PrettyPrintMap(){}


	public static String toString(final Object obj){
		final StringBuilder sb = new StringBuilder(0);
		final Class<?> type = obj.getClass();
		if(Iterable.class.isAssignableFrom(type))
			toStringAsIterable(sb, (Iterable<?>)obj);
		else if(obj instanceof final Map<?, ?> map)
			toStringAsMap(sb, map);
		else if(type.isArray())
			toStringAsArray(sb, obj);
		else
			sb.append(obj);
		return sb.toString();
	}

	private static void toStringAsIterable(final StringBuilder sb, final Iterable<?> items){
		sb.append('[');
		for(final Object item : items){
			addElementSeparator(sb);

			sb.append(toString(item));
		}
		sb.append(']');
	}

	private static void toStringAsMap(final StringBuilder sb, final Map<?, ?> map){
		sb.append('{');
		for(final Map.Entry<?, ?> entry : map.entrySet()){
			addElementSeparator(sb);

			sb.append(entry.getKey())
				.append(':')
				.append(toString(entry.getValue()));
		}
		sb.append('}');
	}

	private static void addElementSeparator(final StringBuilder sb){
		if(sb.length() > 1)
			sb.append(',');
	}

	private static void toStringAsArray(final StringBuilder sb, final Object array){
		sb.append(switch(array){
			case final int[] a -> Arrays.toString(a);
			case final long[] a -> Arrays.toString(a);
			case final short[] a -> Arrays.toString(a);
			case final char[] a -> Arrays.toString(a);
			case final byte[] a -> Arrays.toString(a);
			case final boolean[] a -> Arrays.toString(a);
			case final float[] a -> Arrays.toString(a);
			case final double[] a -> Arrays.toString(a);
			case null, default -> Arrays.deepToString((Object[])array);
		});
	}

}
