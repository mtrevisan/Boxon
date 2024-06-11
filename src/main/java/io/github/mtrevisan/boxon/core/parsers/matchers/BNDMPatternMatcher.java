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

import io.github.mtrevisan.boxon.exceptions.DataException;

import java.util.Arrays;


/**
 * An implementation of the Backwards Non-deterministic DAWG (Directed acyclic word graph) Matching algorithm by Gonzalo Navarro
 * and Mathieu Raffinot.
 * <p>See "A Bit-Parallel Approach to Suffix Automata: Fast Extended String Matching" (appeared in <em>Proceedings of the 9th Annual
 * Symposium on Combinatorial Pattern Matching, 1998</em>).</p>
 *
 * <pre><code>
 *  Preprocessing: O(m)
 *  Searching    : O(n / m)	(best case)
 *                 O(n · log<sub>&Sigma;</sub>(m) / m)	(average)
 *                 O(m · n)	(worst case)
 * </code></pre>
 *
 * @see <a href="https://github.com/johannburkard/StringSearch">StringSearch</a>
 * @see <a href="http://johannburkard.de/software/stringsearch/">StringSearch &#8211; high-performance pattern matching algorithms in Java</a>
 * @see <a href="https://users.dcc.uchile.cl/~gnavarro/ps/cpm98.pdf">A bit-parallel approach to suffix automata: fast extended string matching</a>
 * @see <a href="http://eprints.fri.uni-lj.si/4287/1/63150349-LINA_LUMBUROVSKA-%C4%8Casovno_u%C4%8Dinkoviti_algoritmi_ujemanja_nizov_in_metoda_grobe_sile.pdf">Time-efficient string matching algorithms and the brute-force method</a>
 * @see <a href="https://www.dmi.unict.it/faro/papers/conference/faro31.pdf">A fast suffix automata based algorithm for exact online string matching</a>
 */
public final class BNDMPatternMatcher implements PatternMatcher{

	private static final class SingletonHelper{
		private static final PatternMatcher INSTANCE = new BNDMPatternMatcher();
	}


	/**
	 * Singleton instance of this pattern matcher.
	 *
	 * @return	The instance of this pattern matcher.
	 */
	public static PatternMatcher getInstance(){
		return SingletonHelper.INSTANCE;
	}


	private BNDMPatternMatcher(){}


	/**
	 * Pre-processing of the pattern.
	 * <p>The pattern SHOULD NOT exceed 32 bytes in length.</p>
	 *
	 * @param pattern	The {@code byte} array containing the pattern, may not be {@code null}.
	 * @param wildcard	The wildcard {@code byte} character.
	 * @throws OutOfMemoryError	If there is insufficient memory to allocate the pre-processed pattern array.
	 * @return	An array of pre-processed pattern.
	 */
	public static int[] preProcessPatternWithWildcard(final byte[] pattern, final byte wildcard) throws OutOfMemoryError{
		final int length = pattern.length;
		assertLength(length);

		int j = 0;
		for(int i = 0, shift = length - 1; i < length; i ++, shift --)
			if(pattern[i] == wildcard)
				j |= 1 << shift;

		final int[] preprocessedPattern = createPreProcessedPatternArray();
		if(j != 0)
			Arrays.fill(preprocessedPattern, j);

		return fill(pattern, preprocessedPattern);
	}

	@Override
	public int[] preProcessPattern(final byte[] pattern) throws OutOfMemoryError{
		assertLength(pattern.length);

		final int[] preprocessedPattern = createPreProcessedPatternArray();

		return fill(pattern, preprocessedPattern);
	}

	private static int[] createPreProcessedPatternArray() throws OutOfMemoryError{
		return new int[Integer.SIZE << 3];
	}

	private static int[] fill(final byte[] pattern, final int[] preprocessedPattern){
		int j = 1;
		for(int i = pattern.length - 1; i >= 0; i --, j <<= 1)
			preprocessedPattern[pattern[i] & 0xFF] |= j;
		return preprocessedPattern;
	}

	@Override
	public int indexOf(final byte[] source, int offset, final byte[] pattern, final int[] processedPattern){
		final int patternLength = pattern.length;
		if(patternLength == 0)
			return 0;
		final int sourceLength = source.length;
		if(sourceLength < patternLength + offset)
			return -1;

		assertLength(patternLength);

		while(offset <= sourceLength - patternLength){
			int j = patternLength - 1;
			int last = patternLength;
			int pp = -1;
			while(pp != 0){
				pp &= processedPattern[source[offset + j] & 0xFF];
				if(pp != 0){
					if(j == 0)
						return offset;

					last = j;
				}
				j --;
				pp <<= 1;
			}
			offset += last;
		}
		return -1;
	}

	private static void assertLength(final int length){
		if(length >= Integer.SIZE)
			throw DataException.create("Cannot process a pattern whose length exceeds {} bytes", Integer.SIZE - 1);
	}

}
