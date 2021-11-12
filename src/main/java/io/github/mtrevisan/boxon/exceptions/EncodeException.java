/**
 * Copyright (c) 2020-2021 Mauro Trevisan
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

import io.github.mtrevisan.boxon.internal.JavaHelper;

import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;


/**
 * Thrown if a composition (encoding) went bad.
 */
public final class EncodeException extends Exception{

	private static final long serialVersionUID = 4385865753761318892L;

	private static final String EMPTY_STRING = "";


	public static EncodeException create(final Throwable cause){
		return new EncodeException(cause);
	}

	public static EncodeException create(final Throwable cause, final String message, final Object... parameters){
		return new EncodeException(JavaHelper.format(message, parameters), cause);
	}

	public static EncodeException create(final String message, final Object... parameters){
		return new EncodeException(JavaHelper.format(message, parameters));
	}

	private EncodeException(final String message, final Throwable cause){
		super(message, cause);
	}

	private EncodeException(final String message){
		super(message);
	}

	private EncodeException(final Throwable cause){
		super(cause);
	}

	@Override
	public String getMessage(){
		String messsage = EMPTY_STRING;
		final Throwable cause = getCause();
		if(cause != null)
			messsage += cause.getMessage();
		return messsage;
	}


	@SuppressWarnings("unused")
	private void writeObject(final ObjectOutputStream os) throws NotSerializableException{
		throw new NotSerializableException(getClass().getName());
	}

	@SuppressWarnings("unused")
	private void readObject(final ObjectInputStream is) throws NotSerializableException{
		throw new NotSerializableException(getClass().getName());
	}

}
