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
 * Interface representing data that can be compared using a metric.
 *
 * @param <D>	The specific type of data that extends MetricData.
 * @see <a href="https://github.com/sergio-gomez/MultiDendrograms">MultiDendrograms</a>
 */
public interface MetricData<D extends MetricData<D>>{

	/**
	 * Returns the length of the metric data.
	 *
	 * @return	The length of the metric data.
	 */
	int length();

	/**
	 * Retrieves the element at the specified index.
	 *
	 * @param index	The index of the desired element.
	 * @return	The element at the specified index.
	 */
	Object elementAt(int index);

	/**
	 * Compares this instance with another instance of the same type.
	 *
	 * @param other	The instance to compare with this instance.
	 * @return	Whether the specified instance is equal to this instance.
	 */
	boolean equals(D other);

	/**
	 * Checks if the element at the specified index in the current instance is equal to the element
	 * at the specified index in another instance of the same type.
	 *
	 * @param index	The index of the element in the current instance.
	 * @param other	The other instance to compare against.
	 * @param otherIndex	The index of the element in the other instance.
	 * @return	Whether the elements at the specified indices are equal.
	 */
	boolean equalsAtIndex(int index, D other, int otherIndex);

}
