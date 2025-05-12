/**
 * Copyright (c) 2021 Mauro Trevisan
 * <p>
 * Permission is hereby granted, free of charge, to any person
 * getting a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 * <p>
 * The above copyright notice and this permission notice shall be
 * included in all copies or significant portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */
package io.github.mtrevisan.boxon.core.similarity.metrics;

import io.github.mtrevisan.boxon.core.similarity.distances.MetricData;


/**
 * Metric interface for measuring similarity and distance between data of type {@code D}.
 *
 * @param <D>	The type of data that this metric can handle, extending {@link MetricData}.
 */
public interface Metric<D extends MetricData<D>>{

	/**
	 * Finds the similarity between two inputs.
	 *
	 * @param input1	The first object, must not be {@code null}.
	 * @param input2	The second object, must not be {@code null}.
	 * @return	Result similarity, a number between {@code 0} (not similar) and {@code 1} (equals) inclusive.
	 * @throws IllegalArgumentException	If either input is {@code null}.
	 */
	double similarity(final D input1, final D input2);

	/**
	 * Compute the distance between inputs: the minimum number of operations needed to transform one input into the other (insertion,
	 * deletion, substitution, transposition of a single element).
	 * <p>
	 * It is always at least the difference of the sizes of the two inputs.
	 * It is at most the length of the longer input (if all the costs are 1).
	 * It is zero if and only if the inputs are equal.
	 * If the inputs are the same size, the Hamming distance is an upper bound on the Levenshtein distance.
	 * </p>
	 *
	 * @param input1	The first object, must not be {@code null}.
	 * @param input2	The second object, must not be {@code null}.
	 * @return	The computed distance.
	 * @throws IllegalArgumentException	If either input is {@code null}.
	 */
	int distance(final D input1, final D input2);

}
