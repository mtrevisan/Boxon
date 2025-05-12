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
package io.github.mtrevisan.boxon.exceptions;

import io.github.mtrevisan.boxon.helpers.JavaHelper;

import java.io.Serial;


/**
 * Thrown if a parsing (decoding) went bad.
 */
public final class DecodeException extends BoxonException{

	@Serial
	private static final long serialVersionUID = 5375434179637246605L;


	/** Index of the decoded message this error refers to. */
	private final int errorIndex;


	/**
	 * Constructs a new exception with the specified index of a decoded message and cause.
	 *
	 * @param errorIndex	Index of the decoded message this error refers to.
	 * @param cause	The cause (which is saved for later retrieval by the {@link #getCause()} method). (A {@code null} value is
	 * 	permitted, and indicates that the cause is nonexistent or unknown.)
	 * @return	An instance of this exception.
	 */
	public static DecodeException create(final int errorIndex, final Throwable cause){
		return new DecodeException(errorIndex, cause);
	}


	private DecodeException(final int errorIndex, final Throwable cause){
		super(cause);

		this.errorIndex = errorIndex;
	}


	/**
	 * Get the index of the decoded message this error is referring to.
	 *
	 * @return	The index.
	 */
	public int getErrorIndex(){
		return errorIndex;
	}

	@Override
	public String getMessage(){
		String message = JavaHelper.EMPTY_STRING;
		final Throwable cause = getCause();
		if(cause != null)
			message += cause.getMessage();
		if(errorIndex >= 0)
			message += System.lineSeparator() + "   at index " + errorIndex;
		return message;
	}

}
