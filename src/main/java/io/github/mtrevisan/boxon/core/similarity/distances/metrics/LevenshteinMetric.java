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
package io.github.mtrevisan.boxon.core.similarity.distances.metrics;

import io.github.mtrevisan.boxon.core.similarity.distances.MetricData;


/**
 * Implementation of Levenshtein distance.
 * <p>It is the minimum number of operations needed to transform one input into the other, where an operation is defined as an insertion,
 * deletion, or substitution of a single element.</p>
 * <p>It does respect triangle inequality (the distance between two inputs is no greater than the sum Levenshtein distances from a third
 * input), and is thus a metric distance.</p>
 *
 * @see <a href="https://en.wikipedia.org/wiki/Levenshtein_distance">Levenshtein distance</a>
 * @see <a href="https://github.com/tdebatty/java-string-similarity/blob/master/src/main/java/info/debatty/java/stringsimilarity/Levenshtein.java">Levenstein.java</a>
 */
public final class LevenshteinMetric<D extends MetricData<D>> implements Metric<D>{

	private final int insertionCost;
	private final int deletionCost;
	private final int substitutionCost;

	private final int maxCost;


	public static <D extends MetricData<D>> LevenshteinMetric<D> create(){
		return new LevenshteinMetric<>(1, 1, 1);
	}

	public static <D extends MetricData<D>> LevenshteinMetric<D> create(final int insertionCost, final int deletionCost,
			final int substitutionCost){
		return new LevenshteinMetric<>(insertionCost, deletionCost, substitutionCost);
	}


	private LevenshteinMetric(final int insertionCost, final int deletionCost, final int substitutionCost){
		if(insertionCost <= 0)
			throw new IllegalArgumentException("Insertion cost should be positive");
		if(deletionCost <= 0)
			throw new IllegalArgumentException("Deletion cost should be positive");
		if(substitutionCost <= 0)
			throw new IllegalArgumentException("Substitution cost should be positive");

		this.insertionCost = insertionCost;
		this.deletionCost = deletionCost;
		this.substitutionCost = substitutionCost;

		maxCost = -min(-insertionCost, -deletionCost, -substitutionCost);
	}


	@Override
	public double similarity(final D input1, final D input2){
		if(input1 == null || input2 == null)
			throw new IllegalArgumentException("Inputs must not be null");
		if(insertionCost != 1 || deletionCost != 1 || substitutionCost != 1)
			throw new IllegalArgumentException("Cannot calculate similarity if all the costs are not 1");

		final int maxLength = Math.max(input1.length(), input2.length());
		return 1. - (maxLength > 0? (double)distance(input1, input2) / (maxLength * maxCost): 0.);
	}

	@Override
	public int distance(final D input1, final D input2){
		if(input1 == null || input2 == null)
			throw new IllegalArgumentException("Inputs must not be null");

		if(input1.equals(input2))
			return 0;
		final int length1 = input1.length();
		final int length2 = input2.length();
		if(length1 == 0)
			return length2;
		if(length2 == 0)
			return length1;

		final int length = Math.min(length1, length2);
		//remove prefix from both inputs
		int startIndex = 0;
		while(startIndex < length && input1.equalsAtIndex(startIndex, input2, startIndex))
			startIndex ++;
		//remove suffix from both inputs
		int endIndex = 0;
		while(endIndex < length && input1.equalsAtIndex(length1 - endIndex - 1, input2, length2 - endIndex - 1))
			endIndex ++;

		//swap to reduce space complexity
		return (length1 > length2
			? distanceInternal(input1, input2, startIndex, endIndex)
			: distanceInternal(input2, input1, startIndex, endIndex));
	}

	private int distanceInternal(final D input1, final D input2, final int startIndex, final int endIndex){
		//determine corrected lengths of the inputs
		final int length1 = input1.length() - startIndex - endIndex;
		final int length2 = input2.length() - startIndex - endIndex;

		//initialize and fill the cost array
		final int[] cost = initializeBaseCostArray(length2);

		//define and compute the cost array for the general case
		for(int i = 1; i <= length1; i ++){
			cost[0] = i * deletionCost;

			int previousAbove = i - 1;
			for(int j = 1; j <= length2; j ++){
				//determine whether characters match at current indices, assign appropriate cost
				final boolean equalInput = input1.equalsAtIndex(startIndex + i - 1, input2, startIndex + j - 1);
				final int indicator = (equalInput? 0: substitutionCost);

				final int actual = previousAbove + indicator;
				previousAbove = cost[j];

				//calculate the total cost considering the possible operations
				cost[j] = min(previousAbove + insertionCost,
					cost[j - 1] + deletionCost,
					actual);
			}
		}

		return cost[length2];
	}

	private int[] initializeBaseCostArray(final int length){
		//initialize the cost array
		final int[] cost = new int[length + 1];

		//fill the cost array for the base case (when `input1` is empty)
		for(int j = 1; j <= length; j ++)
			cost[j] = j * deletionCost;

		return cost;
	}

	private static int min(final int a, final int b, final int c){
		return Math.min(a, Math.min(b, c));
	}

}
