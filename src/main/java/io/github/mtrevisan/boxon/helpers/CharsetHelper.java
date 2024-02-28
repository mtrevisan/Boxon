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
package io.github.mtrevisan.boxon.helpers;

import java.nio.charset.Charset;
import java.nio.charset.UnsupportedCharsetException;
import java.util.function.Function;


/**
 * A collection of convenience methods for working with {@link Charset} objects.
 */
public final class CharsetHelper{

	/** The default character set. */
	public static final String DEFAULT_CHARSET = "UTF-8";


	private static final Function<String, Charset> CHARSETS = Memoizer.memoize(Charset::forName);


	private CharsetHelper(){}


	/**
	 * Get the charset object for the named charset.
	 *
	 * @param charsetName	The charset name.
	 * @return	The charset.
	 * @throws UnsupportedCharsetException	If the name is not valid.
	 */
	public static Charset lookup(final String charsetName) throws UnsupportedCharsetException{
		return CHARSETS.apply(charsetName);
	}

}
