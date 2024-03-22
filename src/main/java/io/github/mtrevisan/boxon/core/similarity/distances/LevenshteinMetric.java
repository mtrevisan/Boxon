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
package io.github.mtrevisan.boxon.core.similarity.distances;


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
public final class LevenshteinMetric<D extends DistanceDataInterface<D>>{

	private final int insertionCost;
	private final int deletionCost;
	private final int substitutionCost;


	public static <D extends DistanceDataInterface<D>> LevenshteinMetric<D> create(){
		return new LevenshteinMetric<>(1, 1, 1);
	}

	public static <D extends DistanceDataInterface<D>> LevenshteinMetric<D> create(final int insertionCost, final int deletionCost,
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
	}


	/**
	 * Finds the similarity between two inputs.
	 *
	 * @param input1	The first object, must not be {@code null}.
	 * @param input2	The second object, must not be {@code null}.
	 * @return	Result similarity, a number between {@code 0} (not similar) and {@code 1} (equals).
	 * @throws IllegalArgumentException	If either input is {@code null}.
	 */
	public double similarity(final D input1, final D input2){
		if(input1 == null || input2 == null)
			throw new IllegalArgumentException("Inputs must not be null");
		if(insertionCost != 1 || deletionCost != 1 || substitutionCost != 1)
			throw new IllegalArgumentException("Cannot calculate similarity if all the costs are not 1");

		final int maxLength = Math.max(input1.length(), input2.length());
		return 1. - (maxLength > 0? (double)distance(input1, input2) / maxLength: 0.);
	}

	/**
	 * Compute the distance between inputs: the minimum number of operations needed to transform one input into the other (insertion,
	 * deletion, substitution of a single element).
	 * <p>
	 * It is always at least the difference of the sizes of the two inputs.
	 * It is at most the length of the longer input (if all the costs are 1).
	 * It is zero if and only if the inputs are equal.
	 * If the inputs are the same size, the Hamming distance is an upper bound on the Levenshtein distance.
	 * </p>
	 * <p>Implementation uses dynamic programming (Wagner–Fischer algorithm), with only 1 row of data. The space requirement is thus
	 * `O(min(m, n))` and the algorithm runs in `O(m·n)`.</p>
	 *
	 * @param input1	The first object, must not be {@code null}.
	 * @param input2	The second object, must not be {@code null}.
	 * @return	The computed distance.
	 * @throws IllegalArgumentException	If either input is {@code null}.
	 */
	public int distance(D input1, D input2){
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

		//remove prefix from both inputs
		int startIndex = 0;
		final int length = Math.min(length1, length2);
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
		final int length1 = input1.length() - startIndex - endIndex;
		final int length2 = input2.length() - startIndex - endIndex;

		final int[] cost = new int[length2 + 1];

		for(int j = 1; j <= length2; j ++)
			cost[j] = j * deletionCost;

		for(int i = 1; i <= length1; i ++){
			cost[0] = i * deletionCost;

			int previousAbove = i - 1;
			for(int j = 1; j <= length2; j ++){
				final int indicator = (input1.equalsAtIndex(startIndex + i - 1, input2, startIndex + j - 1)
					? 0
					: substitutionCost);
				final int actual = previousAbove + indicator;
				previousAbove = cost[j];
				cost[j] = min(previousAbove + insertionCost,
					cost[j - 1] + deletionCost,
					actual);
			}
		}

		return cost[length2];
	}

	private static int min(final int a, final int b, final int c){
		return Math.min(a, Math.min(b, c));
	}


	//Hirschberg’s algorithm
	public int distance2(final D input1, final D input2){
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

		//create two work vectors of integer distances
		final int[] previousDistances = new int[length2 + 1];
		final int[] currentDistances = new int[length2 + 1];

		//initialize `previousDistances` (the previous row of distances)
		//this row is `A[0][i]`: edit distance for an empty `input1`
		//the distance is just the number of elements to delete from `input2`
		for(int i = 0; i < previousDistances.length; i ++)
			previousDistances[i] = i * deletionCost;

		//fill in the rest of the rows
		for(int i = 0; i < length1; i ++){
			//calculate `currentDistances` (current row distances) from the previous row `previousDistances`
			//first element of `currentDistances` is `A[i+1][0]`
			//edit distance is `delete (i+1)` elements from `s` to match empty `t`
			currentDistances[0] = i + deletionCost;

			for(int j = 0; j < length2; j ++)
				currentDistances[j + 1] = min(currentDistances[j] + insertionCost,
					previousDistances[j + 1] + deletionCost,
					previousDistances[j] + (input1.equalsAtIndex(i, input2, j)? 0: substitutionCost));

			//flip references to current and previous row
			swapArrays(previousDistances, currentDistances);
		}

		return previousDistances[length2];
	}

	private void swapArrays(final int[] array1, final int[] array2){
		for(int i = 0, length = array1.length; i < length; i ++){
			array1[i] = array1[i] ^ array2[i];
			array2[i] = array1[i] ^ array2[i];
			array1[i] = array1[i] ^ array2[i];
		}
	}

}
