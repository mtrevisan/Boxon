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
package io.github.mtrevisan.boxon.codecs.managers;

import io.github.mtrevisan.boxon.exceptions.AnnotationException;

import java.nio.charset.Charset;
import java.util.function.Function;


public final class CharsetHelper{

	private static final Function<String, Charset> CHARSETS = Memoizer.memoize(CharsetHelper::lookupName);


	private CharsetHelper(){}


	/**
	 * Get the charset object for the named charset.
	 *
	 * @param charsetName	The charset name.
	 * @return	The charset.
	 */
	public static Charset lookup(final String charsetName){
		return CHARSETS.apply(charsetName);
	}

	private static Charset lookupName(final String charsetName){
		return Charset.forName(charsetName);
	}


	static void assertValidCharset(final String charsetName) throws AnnotationException{
		try{
			lookup(charsetName);
		}
		catch(final IllegalArgumentException ignored){
			throw AnnotationException.create("Invalid charset: '{}'", charsetName);
		}
	}

}
