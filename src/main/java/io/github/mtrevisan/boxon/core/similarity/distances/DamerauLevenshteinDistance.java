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

import java.util.HashMap;
import java.util.Map;


/**
 * Implementation of Damerau-Levenshtein distance.
 * <p>It is the minimum number of operations needed to transform one string into the other, where an operation is defined as an insertion,
 * deletion, substitution, or transposition of a single character.</p>
 * <p>It does respect triangle inequality (the distance between two strings is no greater than the sum Levenshtein distances from a third
 * string), and is thus a metric distance.</p>
 *
 * @see <a href="https://en.wikipedia.org/wiki/Damerau%E2%80%93Levenshtein_distance">Damerau-Levenshtein distance</a>
 * @see <a href="https://github.com/tdebatty/java-string-similarity/blob/master/src/main/java/info/debatty/java/stringsimilarity/Damerau.java">Damerau.java</a>
 */
public final class DamerauLevenshteinDistance{

	private final int insertionCost;
	private final int deletionCost;
	private final int substitutionCost;
	private final int transpositionCost;


	public static DamerauLevenshteinDistance create(){
		return new DamerauLevenshteinDistance(1, 1, 1, 1);
	}

	public static DamerauLevenshteinDistance create(final int insertionCost, final int deletionCost, final int substitutionCost,
			final int transpositionCost){
		return new DamerauLevenshteinDistance(insertionCost, deletionCost, substitutionCost, transpositionCost);
	}

	private DamerauLevenshteinDistance(final int insertionCost, final int deletionCost, final int substitutionCost,
			final int transpositionCost){
		this.insertionCost = insertionCost;
		this.deletionCost = deletionCost;
		this.substitutionCost = substitutionCost;
		this.transpositionCost = transpositionCost;
	}


	/**
	 * Finds the similarity between two Strings.
	 *
	 * @param str1	The first object, must not be {@code null}.
	 * @param str2	The second object, must not be {@code null}.
	 * @return	Result similarity, a number between {@code 0} (not similar) and {@code 1} (equals).
	 * @throws IllegalArgumentException	If either input is {@code null}.
	 */
	public double similarity(final DistanceDataInterface str1, final DistanceDataInterface str2){
		if(str1 == null || str2 == null)
			throw new IllegalArgumentException("Strings must not be null");

		final int maxLength = Math.max(str1.length(), str2.length());
		final int distance = distance(str1, str2);
		return 1. - (maxLength > 0? (double)distance / maxLength: 0.);
	}

	/**
	 * Compute the distance between strings: the minimum number of operations needed to transform one string into the other (insertion,
	 * deletion, substitution, transposition of a single character).
	 * <p>
	 * It is always at least the difference of the sizes of the two strings.
	 * It is at most the length of the longer string.
	 * It is zero if and only if the strings are equal.
	 * If the strings are the same size, the Hamming distance is an upper bound on the Levenshtein distance.
	 * </p>
	 *
	 * @param str1	The first object, must not be {@code null}.
	 * @param str2	The second object, must not be {@code null}.
	 * @return	The computed distance.
	 * @throws IllegalArgumentException	If either input is {@code null}.
	 */
	public int distance(final DistanceDataInterface str1, final DistanceDataInterface str2){
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

		//maximum distance is the max possible distance
		final int maximumDistance = str1.length() + str2.length();

		//create and initialize the character array indices
		final Map<Object, Integer> da = new HashMap<>(maximumDistance);
		for(int d = 0; d < str1.length(); d ++)
			da.put(str1.elementAt(d), 0);
		for(int d = 0; d < str2.length(); d ++)
			da.put(str2.elementAt(d), 0);

		//create the distance matrix `H[0 .. s1_{length+1}][0 .. s2_{length+1}]`
		final int[][] h = new int[str1.length() + 2][str2.length() + 2];
		//initialize the left and top edges of `H`
		for(int i = 0; i <= str1.length(); i ++){
			h[i + 1][0] = maximumDistance;
			h[i + 1][1] = i;
		}
		for(int j = 0; j <= str2.length(); j ++){
			h[0][j + 1] = maximumDistance;
			h[1][j + 1] = j;
		}

		//fill in the rest of the rows
		for(int i = 1; i <= length1; i ++){
			int db = 0;

			for(int j = 1; j <= length2; j ++){
				final int i1 = da.get(str2.elementAt(j - 1));
				final int j1 = db;

				int subCost = substitutionCost;
				if(str1.equalsAtIndex(i - 1, str2, j - 1)){
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

			da.put(str1.elementAt(i - 1), i);
		}

		return h[str1.length() + 1][str2.length() + 1];
	}

	private static int min(final int a, final int b, final int c, final int d){
		return Math.min(a, Math.min(b, Math.min(c, d)));
	}

}