/*
 * Copyright (c) 2021-2022 Mauro Trevisan
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
package io.github.mtrevisan.boxon.core.managers.extractors;

import io.github.mtrevisan.boxon.helpers.StringHelper;


public final class JSONPathException extends RuntimeException{

	private static final long serialVersionUID = 8273242566993098760L;


	/**
	 * Constructs a new exception with the specified cause and message, possibly with parameters.
	 *
	 * @param cause	The cause (which is saved for later retrieval by the {@link #getCause()} method). (A {@code null} value is
	 * 					permitted, and indicates that the cause is nonexistent or unknown.)
	 * @param message	The message to be formatted (see {@link StringHelper#format(String, Object...)}).
	 * @param parameters	The parameters of the message.
	 * @return	An instance of this exception.
	 */
	public static JSONPathException create(final Throwable cause, final String message, final Object... parameters){
		return new JSONPathException(StringHelper.format(message, parameters), cause);
	}

	/**
	 * Constructs a new exception with the specified message, possibly with parameters.
	 *
	 * @param message	The message to be formatted (see {@link StringHelper#format(String, Object...)}).
	 * @param parameters	The parameters of the message.
	 * @return	An instance of this exception.
	 */
	public static JSONPathException create(final String message, final Object... parameters){
		return new JSONPathException(StringHelper.format(message, parameters));
	}


	private JSONPathException(final String message){
		super(message);
	}

	private JSONPathException(final String message, final Throwable cause){
		super(message, cause);
	}

}
