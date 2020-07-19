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

import io.github.mtrevisan.boxon.helpers.ByteHelper;
import io.github.mtrevisan.boxon.helpers.ExceptionHelper;

import java.util.StringJoiner;


public class ParseException extends Exception{

	private static final long serialVersionUID = -7230533024483622086L;


	private final byte[] wholeMessage;
	private final int errorIndex;


	public ParseException(final byte[] wholeMessage, final int errorIndex, final Throwable cause){
		super(cause);

		this.wholeMessage = wholeMessage;
		this.errorIndex = errorIndex;
	}

	@Override
	public String getMessage(){
		final StringJoiner sj = new StringJoiner(System.lineSeparator());
		sj.add("Error decoding message: " + ByteHelper.toHexString(wholeMessage));
		final Throwable cause = getCause();
		if(cause != null)
			sj.add(ExceptionHelper.getMessageNoLineNumber(cause));
		if(errorIndex >= 0)
			sj.add("   at index " + errorIndex);
		return sj.toString();
	}

}
