/*
 * Copyright (c) 2020-2022 Mauro Trevisan
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
package io.github.mtrevisan.boxon.internal.matchers;


/** The base class for pattern matching algorithm implementations. */
public interface PatternMatcher{

	/**
	 * Pre-processes the pattern.
	 *
	 * @param pattern	The {@code byte} array containing the pattern, may not be {@code null}.
	 * @return	an array of pre-processed pattern.
	 */
	int[] preProcessPattern(byte[] pattern);

	/**
	 * Returns the position in the text at which the pattern was found.
	 * <p>Returns {@code -1} if the pattern was not found.</p>
	 *
	 * @param source	The {@code byte} array containing the text, may not be {@code null}.
	 * @param offset	At which position in the text the comparing should start.
	 * @param pattern	The pattern to search for, may not be {@code null}.
	 * @param processedPattern	Processed pattern, see {@link #preProcessPattern(byte[])}.
	 * @return the position in the text or {@code -1} if the pattern was not found.
	 */
	int indexOf(byte[] source, int offset, byte[] pattern, int[] processedPattern);

}
