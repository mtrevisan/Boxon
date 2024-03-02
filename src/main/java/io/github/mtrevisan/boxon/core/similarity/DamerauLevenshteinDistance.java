package io.github.mtrevisan.boxon.core.similarity;

import java.util.HashMap;
import java.util.Map;


/**
 * Implementation of Damerau-Levenshtein distance with transposition (also sometimes calls unrestricted Damerau-Levenshtein distance).
 * <p>It is the minimum number of operations needed to transform one string into the other, where an operation is defined as an insertion,
 * deletion, or substitution of a single character, or a transposition of two adjacent characters.</p>
 * <p>It does respect triangle inequality, and is thus a metric distance.</p>
 *
 * @see <a href="https://en.wikipedia.org/wiki/Damerau%E2%80%93Levenshtein_distance">Damerau–Levenshtein distance</a>
 * @see <a href="https://github.com/tdebatty/java-string-similarity/blob/master/src/main/java/info/debatty/java/stringsimilarity/Damerau.java">Damerau.java</a>
 */
public class DamerauLevenshteinDistance{

	private DamerauLevenshteinDistance(){}


	/**
	 * Finds the similarity between two Strings.
	 *
	 * @param str1	The first string, must not be {@code null}.
	 * @param str2	The second string, must not be {@code null}.
	 * @return	Result similarity, a number between {@code 0} (not similar) and {@code 1} (equals).
	 * @throws IllegalArgumentException	If either String input {@code null}.
	 */
	public static double similarity(final CharSequence str1, final CharSequence str2){
		if(str1.equals(str2))
			return 1.;

		final int maxLength = Math.max(str1.length(), str2.length());
		return 1. - (double)distance(str1, str2) / maxLength;
	}

	/**
	 * Compute the distance between strings: the minimum number of operations needed to transform one string into the other (insertion,
	 * deletion, substitution of a single character, or a transposition of two adjacent characters).
	 *
	 * @param str1	The first string, must not be {@code null}.
	 * @param str2	The second string, must not be {@code null}.
	 * @return	The computed distance.
	 * @throws IllegalArgumentException	If either String input {@code null}.
	 */
	public static int distance(final CharSequence str1, final CharSequence str2){
		if(str1 == null || str2 == null)
			throw new IllegalArgumentException("Strings must not be null");

		if(str1.equals(str2))
			return 0;

		//(max distance)
		final int sumLength = str1.length() + str2.length();

		//create and initialize the character array indices
		final Map<Character, Integer> da = new HashMap<>();
		for(int d = 0; d < str1.length(); d ++)
			da.put(str1.charAt(d), 0);
		for(int d = 0; d < str2.length(); d ++)
			da.put(str2.charAt(d), 0);

		//create the distance matrix `H[0 .. s1_{length + 1}][0 .. s2_{length + 1}]`
		final int[][] h = new int[str1.length() + 2][str2.length() + 2];
		//initialize the left and top edges of `H`
		for(int i = 0; i <= str1.length(); i ++){
			h[i + 1][0] = sumLength;
			h[i + 1][1] = i;
		}
		for(int j = 0; j <= str2.length(); j ++){
			h[0][j + 1] = sumLength;
			h[1][j + 1] = j;
		}

		//fill in the distance matrix `H`
		//look at each character in `str1`
		for(int i = 1; i <= str1.length(); i ++){
			int db = 0;

			//look at each character in `str2`
			for(int j = 1; j <= str2.length(); j ++){
				final int i1 = da.get(str2.charAt(j - 1));
				final int j1 = db;

				int cost = 1;
				if(str1.charAt(i - 1) == str2.charAt(j - 1)){
					cost = 0;
					db = j;
				}

				h[i + 1][j + 1] = min(h[i][j] + cost,	//substitution
					h[i + 1][j] + 1,	//insertion
					h[i][j + 1] + 1,	//deletion
					h[i1][j1] + (i - i1 - 1) + 1 + (j - j1 - 1));
			}

			da.put(str1.charAt(i - 1), i);
		}

		return h[str1.length() + 1][str2.length() + 1];
	}

	private static int min(final int a, final int b, final int c, final int d){
		return Math.min(a, Math.min(b, Math.min(c, d)));
	}

}
