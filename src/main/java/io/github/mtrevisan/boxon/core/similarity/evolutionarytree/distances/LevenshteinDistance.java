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

package io.github.mtrevisan.boxon.core.similarity.evolutionarytree.distances;


/**
 * Implementation of Levenshtein distance.
 * <p>It is the minimum number of operations needed to transform one string into the other, where an operation is defined as an insertion,
 * deletion, or substitution of a single character.</p>
 * <p>It does respect triangle inequality (the distance between two strings is no greater than the sum Levenshtein distances from a third
 * string), and is thus a metric distance.</p>
 *
 * @see <a href="https://en.wikipedia.org/wiki/Levenshtein_distance">Levenshtein distance</a>
 * @see <a href="https://github.com/tdebatty/java-string-similarity/blob/master/src/main/java/info/debatty/java/stringsimilarity/Levenshtein.java">Levenstein.java</a>
 */
public final class LevenshteinDistance{

	private LevenshteinDistance(){}


	/**
	 * Finds the similarity between two Strings.
	 *
	 * @param str1	The first object, must not be {@code null}.
	 * @param str2	The second object, must not be {@code null}.
	 * @return	Result similarity, a number between {@code 0} (not similar) and {@code 1} (equals).
	 * @throws IllegalArgumentException	If either input is {@code null}.
	 */
	public static double similarity(final DistanceDataInterface str1, final DistanceDataInterface str2){
		if(str1 == null || str2 == null)
			throw new IllegalArgumentException("Strings must not be null");

		final int maxLength = Math.max(str1.length(), str2.length());
		return 1. - (maxLength > 0? (double)distance(str1, str2) / maxLength: 0.);
	}

	/**
	 * Compute the distance between strings: the minimum number of operations needed to transform one string into the other (insertion,
	 * deletion, substitution of a single character).
	 * <p>
	 * It is always at least the difference of the sizes of the two strings.
	 * It is at most the length of the longer string.
	 * It is zero if and only if the strings are equal.
	 * If the strings are the same size, the Hamming distance is an upper bound on the Levenshtein distance.
	 * </p>
	 * <p>Implementation uses dynamic programming (Wagner–Fischer algorithm), with only 2 rows of data. The space requirement is thus `O(m)`
	 * and the algorithm runs in `O(m·n)`.</p>
	 *
	 * @param str1	The first object, must not be {@code null}.
	 * @param str2	The second object, must not be {@code null}.
	 * @return	The computed distance.
	 * @throws IllegalArgumentException	If either input is {@code null}.
	 */
	public static int distance(final DistanceDataInterface str1, final DistanceDataInterface str2){
		if(str1 == null || str2 == null)
			throw new IllegalArgumentException("Strings must not be null");

		if(str1.equals(str2))
			return 0;
		final int length1 = str1.length();
		final int length2 = str2.length();
		if(length1 == 0)
			return length2;
		if(length2 == 0)
			return length1;

		//create two work vectors of integer distances
		int[] v0 = new int[length2 + 1];
		int[] v1 = new int[length2 + 1];
		int[] vtemp;

		//initialize `v0` (the previous row of distances)
		//this row is `A[0][i]`: edit distance for an empty `str1`
		//the distance is just the number of characters to delete from `str2`
		for(int i = 0; i < v0.length; i ++)
			v0[i] = i;

		//fill in the rest of the rows
		for(int i = 0; i < length1; i ++){
			//calculate `v1` (current row distances) from the previous row `v0`
			//first element of `v1` is `A[i+1][0]`
			//edit distance is `delete (i+1)` chars from `s` to match empty `t`
			v1[0] = i + 1;

			//int minv1 = v1[0];

			for(int j = 0; j < length2; j ++){
				int cost = 1;
				if(str1.equalsAtIndex(i, str2, j))
					cost = 0;
				v1[j + 1] = Math.min(v1[j] + 1,   //cost of insertion
					Math.min(v0[j + 1] + 1,   //cost of deletion
						v0[j] + cost));   //cost of substitution

				//minv1 = Math.min(minv1, v1[j + 1]);
			}

			//`limit` is the maximum result to compute before stopping. This means that the calculation can terminate early if you only care
			//about strings with a certain similarity. Set this to Integer.MAX_VALUE if you want to run the calculation to completion in
			//every case.
			//if(minv1 >= limit)
			//	return limit;

			//flip references to current and previous row
			vtemp = v0;
			v0 = v1;
			v1 = vtemp;
		}

		return v0[length2];
	}

}
