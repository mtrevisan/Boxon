/*
 * Copyright (c) 2021-2024 Mauro Trevisan
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
package io.github.mtrevisan.boxon.core.similarity.distances;


/**
 * Implements the {@link MetricData} interface for strings.
 * <p>>
 * This class provides methods to get the length of the string, get an element at a specified index, compare equality of the entire
 * string with another string, and compare equality of characters at specified indexes between two strings.
 * </p>
 */
public final class StringMetricData implements MetricData<StringMetricData>{

	private final String data;


	/**
	 * Creates a new instance with the given string.
	 *
	 * @param str	The string to be wrapped.
	 * @return	A new instance containing the provided string.
	 */
	public static StringMetricData of(final String str){
		return new StringMetricData(str);
	}


	private StringMetricData(final String str){
		data = str;
	}


	@Override
	public int length(){
		return data.length();
	}

	@Override
	public Object elementAt(final int index){
		return data.charAt(index);
	}

	@Override
	public boolean equals(final StringMetricData other){
		return data.equals(other.data);
	}

	@Override
	public boolean equalsAtIndex(final int index, final StringMetricData other, final int otherIndex){
		return (data.charAt(index) == other.data.charAt(otherIndex));
	}

}
