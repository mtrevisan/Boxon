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
import io.github.mtrevisan.boxon.helpers.StringHelper;

import java.io.Serial;
import java.lang.reflect.Field;


/**
 * Represents a generic error.
 */
public class BoxonException extends Exception{

	@Serial
	private static final long serialVersionUID = -8863756843240934380L;


	/** Class name that generates the error. */
	private String className;
	/** Name of the field in the class that generates the error. */
	private String fieldName;


	/**
	 * Constructs a new exception with the specified cause.
	 *
	 * @param cause	The cause (which is saved for later retrieval by the {@link #getCause()} method). (A {@code null} value is
	 * 	permitted, and indicates that the cause is nonexistent or unknown.)
	 * @return	An instance of this exception.
	 */
	public static BoxonException create(final Throwable cause){
		return new BoxonException(cause);
	}

	/**
	 * Constructs a new exception with the specified message, possibly with parameters.
	 *
	 * @param message	The message to be formatted.
	 * @param parameters	The parameters of the message.
	 * @return	An instance of this exception.
	 */
	public static BoxonException create(final String message, final Object... parameters){
		return new BoxonException(null, message, parameters);
	}


	/**
	 * Constructs a new exception with the specified message.
	 *
	 * @param message	The message.
	 * @param parameters	The parameters of the message.
	 */
	protected BoxonException(final String message, final Object... parameters){
		this(null, message, parameters);
	}

	/**
	 * Constructs a new exception with the specified message and cause.
	 *
	 * @param cause	The cause (which is saved for later retrieval by the {@link #getCause()} method). (A {@code null} value is permitted,
	 * 	and indicates that the cause is nonexistent or unknown.)
	 * @param message	The message.
	 * @param parameters	The parameters of the message.
	 */
	protected BoxonException(final Throwable cause, final String message, final Object... parameters){
		super(StringHelper.format(message, parameters), cause);
	}

	/**
	 * Constructs a new exception with the specified cause.
	 *
	 * @param cause	The cause (which is saved for later retrieval by the {@link #getCause()} method). (A {@code null} value is
	 * 	permitted, and indicates that the cause is nonexistent or unknown.)
	 */
	protected BoxonException(final Throwable cause){
		super(cause);
	}

	/**
	 * Adds class and field names to the exception.
	 *
	 * @param type	The class type.
	 * @param field	The field.
	 * @return	The exception itself.
	 */
	public final BoxonException withClassAndField(final Class<?> type, final Field field){
		return withClassNameAndFieldName(type.getName(), field.getName());
	}

	/**
	 * Adds class and field names to the exception.
	 *
	 * @param className	The class name.
	 * @param fieldName	The field name.
	 * @return	The exception itself.
	 */
	public final BoxonException withClassNameAndFieldName(final String className, final String fieldName){
		this.className = className;
		this.fieldName = fieldName;

		return this;
	}

	@Override
	public String getMessage(){
		return super.getMessage()
			+ (className != null && fieldName != null
				? " in field " + className + "." + fieldName
				: JavaHelper.EMPTY_STRING
			);
	}

}
