/*
 * Copyright (c) 2020-2024 Mauro Trevisan
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
package io.github.mtrevisan.boxon.core.parsers.matchers;


/**
 * An implementation of the Knuth-Morris-Pratt searching algorithm.
 *
 * <pre>{@code
 *  Preprocessing: Θ(m)
 *  Searching    : Θ(n)	(worst case)
 * }</pre>
 *
 * @see <a href="http://eprints.fri.uni-lj.si/4287/1/63150349-LINA_LUMBUROVSKA-%C4%8Casovno_u%C4%8Dinkoviti_algoritmi_ujemanja_nizov_in_metoda_grobe_sile.pdf">Time-efficient string matching algorithms and the brute-force method</a>
 * @see <a href="https://www.dmi.unict.it/faro/papers/conference/faro31.pdf">A fast suffix automata-based algorithm for exact online string matching</a>
 * @see <a href="https://en.wikipedia.org/wiki/Knuth%E2%80%93Morris%E2%80%93Pratt_algorithm">Knuth–Morris–Pratt algorithm</a>
 */
public final class KMPPatternMatcher implements PatternMatcher{

	private static final class SingletonHelper{
		private static final PatternMatcher INSTANCE = new KMPPatternMatcher();
	}


	/**
	 * Singleton instance of this pattern matcher.
	 *
	 * @return	The instance of this pattern matcher.
	 */
	public static PatternMatcher getInstance(){
		return SingletonHelper.INSTANCE;
	}


	private KMPPatternMatcher(){}


	/**
	 * Returns an array that points to the last valid string prefix.
	 *
	 * @param pattern	The list to search for as a subList of {@code source}.
	 * @return	The array of the Longest Prefix Suffix.
	 */
	@Override
	public int[] preProcessPattern(final byte[] pattern){
		final int length = pattern.length;
		final int[] lps = new int[length];

		int i = 1;
		int lengthPreviousLPS = 0;
		while(i < length){
			if(pattern[i] == pattern[lengthPreviousLPS])
				//when both chars before `lengthPreviousLPS` and `i` are equal, link both and move both forward
				lps[i ++] = ++ lengthPreviousLPS;
			else if(lengthPreviousLPS > 0)
				//if `lengthPreviousLPS` isn't in the very beginning, then send `lengthPreviousLPS` backward by following
				//the already set pointer to where it is pointing to
				lengthPreviousLPS = lps[lengthPreviousLPS - 1];
			else
				//`lengthPreviousLPS` has fallen all the way back to the beginning
				lps[i ++] = 0;
		}

		return lps;
	}

	/**
	 * Returns the index of the first match for the given pattern array within the specified source array,
	 * or {@code -1} if there is no such occurrence.
	 * <p>More formally, returns the lowest index such that {@code source.subArray(i, i + pattern.size()).equals(pattern)},
	 * or {@code -1} if there is no such index.</p>
	 * (Returns {@code -1} if {@code pattern.size() > source.size()})
	 * <p>This implementation uses the "Knuth-Morris-Pratt" technique of scanning over the source list.</p>
	 *
	 * @param source	The list in which to search for the first occurrence of {@code pattern}.
	 * @param offset	Offset to start the search from.
	 * @param pattern	The list to search for as a subList of {@code source}.
	 * @param failureTable	Longest Prefix Suffix array precomputed by {@link #preProcessPattern(byte[])}.
	 * @return	Returns the index of the first match for the given pattern array within the specified source list,
	 * 	or {@code -1} if there is no such occurrence.
	 */
	@Override
	public int indexOf(final byte[] source, final int offset, final byte[] pattern, final int[] failureTable){
		final int patternLength = pattern.length;
		if(patternLength == 0)
			return 0;
		final int sourceLength = source.length;
		if(sourceLength < patternLength + offset)
			return -1;

		//no candidate matched the pattern
		int index = -1;

		//current byte index in the target array
		int targetPointer = 0;
		//current byte index in the search array
		int searchPointer = offset;
		//while there is more to search with, keep searching
		while(searchPointer < sourceLength){
			if(source[searchPointer] == pattern[targetPointer]){
				//found the current byte in `targetPointer` in the search array
				targetPointer ++;
				if(targetPointer == patternLength){
					//return starting index of found target inside the searched array
					index = searchPointer - targetPointer + 1;
					break;
				}

				//move forward if not found target array
				searchPointer ++;
			}
			else if(targetPointer > 0)
				//use `failureTable` to use the pointer pointed at nearest location of usable array prefix
				targetPointer = failureTable[targetPointer - 1];
			else
				//`targetPointer` is pointing at state 0, so restart search with the current `searchPointer` index
				searchPointer ++;
		}
		return index;
	}

}
