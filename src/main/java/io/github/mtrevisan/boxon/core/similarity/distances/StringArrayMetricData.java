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

import java.util.Arrays;


/**
 * A class representing metric data encapsulated in a string array.
 * <p>
 * It implements the {@link MetricData} interface to provide methods for data comparison and metric-specific operations.
 * </p>
 */
public final class StringArrayMetricData implements MetricData<StringArrayMetricData>{

	private final String[] data;


	public static StringArrayMetricData of(final String... genome){
		return new StringArrayMetricData(genome);
	}


	private StringArrayMetricData(final String[] genome){
		data = genome;
	}


	@Override
	public int length(){
		return data.length;
	}

	@Override
	public Object elementAt(final int index){
		return data[index];
	}

	@Override
	public boolean equals(final StringArrayMetricData other){
		return Arrays.equals(data, other.data);
	}

	@Override
	public boolean equalsAtIndex(final int index, final StringArrayMetricData other, final int otherIndex){
		return (data[index].equals(other.data[otherIndex]));
	}

}
