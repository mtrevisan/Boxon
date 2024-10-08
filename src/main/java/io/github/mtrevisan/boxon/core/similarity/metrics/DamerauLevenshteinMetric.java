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
package io.github.mtrevisan.boxon.core.similarity.metrics;

import io.github.mtrevisan.boxon.core.similarity.distances.MetricData;
import io.github.mtrevisan.boxon.helpers.JavaHelper;

import java.util.Map;


/**
 * Implementation of Damerau-Levenshtein distance.
 * <p>It is the minimum number of operations needed to transform one input into the other, where an operation is defined as an insertion,
 * deletion, substitution, or transposition of a single element.</p>
 * <p>It does respect triangle inequality (the distance between two inputs is no greater than the sum Levenshtein distances from a third
 * input), and is thus a metric distance.</p>
 *
 * @see <a href="https://en.wikipedia.org/wiki/Damerau%E2%80%93Levenshtein_distance">Damerau-Levenshtein distance</a>
 * @see <a href="https://github.com/tdebatty/java-string-similarity/blob/master/src/main/java/info/debatty/java/stringsimilarity/Damerau.java">Damerau.java</a>
 */
public final class DamerauLevenshteinMetric<D extends MetricData<D>> implements Metric<D>{

	private final int insertionCost;
	private final int deletionCost;
	private final int substitutionCost;
	private final int transpositionCost;

	private final int maxCost;


	public static <D extends MetricData<D>> DamerauLevenshteinMetric<D> create(){
		return new DamerauLevenshteinMetric<>(1, 1, 1, 1);
	}

	public static <D extends MetricData<D>> DamerauLevenshteinMetric<D> create(final int insertionCost, final int deletionCost,
			final int substitutionCost, final int transpositionCost){
		return new DamerauLevenshteinMetric<>(insertionCost, deletionCost, substitutionCost, transpositionCost);
	}


	private DamerauLevenshteinMetric(final int insertionCost, final int deletionCost, final int substitutionCost,
			final int transpositionCost){
		if(insertionCost <= 0)
			throw new IllegalArgumentException("Insertion cost should be positive");
		if(deletionCost <= 0)
			throw new IllegalArgumentException("Deletion cost should be positive");
		if(substitutionCost <= 0)
			throw new IllegalArgumentException("Substitution cost should be positive");
		if(transpositionCost <= 0)
			throw new IllegalArgumentException("Transposition cost should be positive");

		this.insertionCost = insertionCost;
		this.deletionCost = deletionCost;
		this.substitutionCost = substitutionCost;
		this.transpositionCost = transpositionCost;

		maxCost = -min(-insertionCost, -deletionCost, -substitutionCost, 0);
	}


	@Override
	public double similarity(final D input1, final D input2){
		validateInputs(input1, input2);

		final int maxLength = Math.max(input1.length(), input2.length());
		final int maxDistance = (maxLength > 1
			? Math.max(maxLength * maxCost, (maxLength - 1) * transpositionCost)
			: maxLength * maxCost);
		return 1. - (maxLength > 0? (double)distance(input1, input2) / maxDistance: 0.);
	}

	@Override
	public int distance(final D input1, final D input2){
		validateInputs(input1, input2);

		if(input1.equals(input2))
			return 0;
		final int length1 = input1.length();
		final int length2 = input2.length();
		if(length1 == 0)
			return length2;
		if(length2 == 0)
			return length1;

		//maximum distance is the max possible distance
		final int maximumDistance = length1 + length2;

		//create and initialize the element array indices
		//TODO use fastutil?
		final Map<Object, Integer> da = JavaHelper.createMapOrEmpty(maximumDistance);
		for(int d = 0; d < length1; d ++)
			da.put(input1.elementAt(d), 0);
		for(int d = 0; d < length2; d ++)
			da.put(input2.elementAt(d), 0);

		final int[][] h = createInitialDistanceMatrix(length1, length2, maximumDistance);

		//fill in the rest of the rows
		for(int i = 1; i <= length1; i ++){
			int db = 0;

			for(int j = 1; j <= length2; j ++){
				final int i1 = da.get(input2.elementAt(j - 1));
				final int j1 = db;

				int subCost = substitutionCost;
				if(input1.equalsAtIndex(i - 1, input2, j - 1)){
					subCost = 0;
					db = j;
				}

				h[i + 1][j + 1] = min(
					h[i][j] + subCost,
					h[i + 1][j] + insertionCost,
					h[i][j + 1] + deletionCost,
					h[i1][j1] + (i - i1 - 1) + (j - j1 - 1) + transpositionCost);
				if(h[i + 1][j + 1] < 0)
					throw new IllegalArgumentException("Cannot calculate distance: some costs are too high");
			}

			da.put(input1.elementAt(i - 1), i);
		}

		return h[length1 + 1][length2 + 1];
	}

	private static <D extends MetricData<D>> void validateInputs(final D input1, final D input2){
		if(input1 == null || input2 == null)
			throw new IllegalArgumentException("Inputs must not be null");
	}

	private static int[][] createInitialDistanceMatrix(final int length1, final int length2, final int maximumDistance){
		//create the distance matrix `H[0 .. s1_{length+1}][0 .. s2_{length+1}]`
		final int[][] h = new int[length1 + 2][length2 + 2];
		//initialize the left and top edges of `H`
		for(int i = 0; i <= length1; i ++){
			h[i + 1][0] = maximumDistance;
			h[i + 1][1] = i;
		}
		for(int j = 0; j <= length2; j ++){
			h[0][j + 1] = maximumDistance;
			h[1][j + 1] = j;
		}
		return h;
	}

	private static int min(final int a, final int b, final int c, final int d){
		return Math.min(Math.min(a, b), Math.min(c, d));
	}

}
