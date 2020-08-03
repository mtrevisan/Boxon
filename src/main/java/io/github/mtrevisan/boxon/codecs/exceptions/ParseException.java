/**
 * Copyright (c) 2020 Mauro Trevisan
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
package io.github.mtrevisan.boxon.codecs.exceptions;

import io.github.mtrevisan.boxon.helpers.ExceptionHelper;


public class ParseException extends Exception{

	private static final long serialVersionUID = 5375434179637246605L;


	private final int errorIndex;


	public ParseException(final int errorIndex, final Throwable cause){
		super(cause);

		this.errorIndex = errorIndex;
	}

	public int getErrorIndex(){
		return errorIndex;
	}

	@Override
	public String getMessage(){
		final StringBuilder sj = new StringBuilder();
		final Throwable cause = getCause();
		if(cause != null)
			sj.append(ExceptionHelper.getMessageNoLineNumber(cause));
		if(errorIndex >= 0)
			sj.append(System.lineSeparator())
				.append("   at index ")
				.append(errorIndex);
		return sj.toString();
	}

}
