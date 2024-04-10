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

import io.github.mtrevisan.boxon.helpers.StringHelper;

import java.io.Serial;


/**
 * Thrown if a configuration has validation errors.
 */
public final class ConfigurationException extends FieldException{

	@Serial
	private static final long serialVersionUID = -1344270549034327649L;


	/**
	 * Constructs a new exception with the specified message, possibly with parameters.
	 *
	 * @param message	The message to be formatted (see {@link StringHelper#format(String, Object...)}).
	 * @param parameters	The parameters of the message.
	 * @return	An instance of this exception.
	 */
	public static ConfigurationException create(final String message, final Object... parameters){
		return new ConfigurationException(StringHelper.format(message, parameters));
	}


	private ConfigurationException(final String message){
		super(message);
	}

}
