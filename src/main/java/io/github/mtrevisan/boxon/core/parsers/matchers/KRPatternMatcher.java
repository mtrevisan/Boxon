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
 * An implementation of the Karp-Rabin/Rabin-Karb searching algorithm.
 *
 * <pre>{@code
 *  Preprocessing: Θ(m)
 *  Searching    : Θ(m + n)	(average and best case)
 *                 O(m · n)	(worst case)
 * }</pre>
 *
 * @see <a href="https://en.wikipedia.org/wiki/Rabin%E2%80%93Karp_algorithm">Rabin–Karp algorithm</a>
 */
public final class KRPatternMatcher implements PatternMatcher{

	private static class SingletonHelper{
		private static final PatternMatcher INSTANCE = new KRPatternMatcher();
	}


	/**
	 * Singleton instance of this pattern matcher.
	 *
	 * @return	The instance of this pattern matcher.
	 */
	public static PatternMatcher getInstance(){
		return SingletonHelper.INSTANCE;
	}


	private KRPatternMatcher(){}


	@Override
	public int[] preProcessPattern(final byte[] pattern){
		//calculate the hash value of pattern
		return new int[]{calculateHash(pattern, pattern.length)};
	}

	/**
	 * Returns the starting position of the first occurrence of the specified pattern array within the specified source array,
	 * or {@code -1} if there is no such occurrence.
	 * <p>More formally, returns the lowest index such that {@code source.subArray(i, i + pattern.size()).equals(pattern)},
	 * or {@code -1} if there is no such index.</p>
	 * (Returns {@code -1} if {@code pattern.size() > source.size()})
	 *
	 * @param source	The list in which to search for the first occurrence of {@code pattern}.
	 * @param offset	Offset to start the search from.
	 * @param pattern	The list to search for as a subList of {@code source}.
	 * @param hashTable	Hash value of the pattern precomputed by {@link #preProcessPattern(byte[])}.
	 * @return	The starting position of the first occurrence of the specified pattern list within the specified source list,
	 * 	or {@code -1} if there is no such occurrence.
	 */
	@Override
	public int indexOf(final byte[] source, final int offset, final byte[] pattern, final int[] hashTable){
		if(pattern.length == 0)
			return 0;
		if(source.length < pattern.length + offset)
			return -1;

		//calculate the hash value of first window of source
		final int patternHash = hashTable[0];
		int sourceHash = calculateHash(source, offset, pattern.length);

		final int length = source.length - pattern.length;
		for(int i = offset; i <= length + offset; i ++){
			//check the hash values of current window of source and pattern
			if(patternHash == sourceHash && equals(source, i, pattern))
				return i;

			if(i < length)
				//calculate hash value for next window of text by removing leading digit add trailing digit
				sourceHash = updateHashForNextWindow(source, pattern, sourceHash, i);
		}

		return -1;
	}

	private static int calculateHash(final byte[] source, final int length){
		int hash = 0;
		for(int i = 0; i < length; i ++){
			hash <<= 1;
			hash += source[i];
		}
		return hash;
	}

	private static int calculateHash(final byte[] source, final int offset, final int length){
		int hash = 0;
		for(int i = 0; i < length; i ++){
			hash <<= 1;
			hash += source[i + offset];
		}
		return hash;
	}

	private static int updateHashForNextWindow(final byte[] source, final byte[] pattern, int sourceHash, final int index){
		sourceHash -= source[index] << (pattern.length - 1);
		return (sourceHash << 1) + source[index + pattern.length];
	}

	private static boolean equals(final byte[] array1, final int offset, final byte[] array2){
		int i = 0;
		final int length = array2.length;
		for(; i < length; i ++)
			if(array1[i + offset] != array2[i])
				break;
		return (i == length);
	}

}
