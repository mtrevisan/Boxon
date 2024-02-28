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

import java.lang.reflect.Field;


/**
 * Represents an error in coding/decoding of a value.
 */
public class FieldException extends Exception{

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
	public static FieldException create(final Throwable cause){
		return new FieldException(cause);
	}


	/**
	 * Constructs a new exception with the specified message.
	 *
	 * @param message	The message.
	 */
	protected FieldException(final String message){
		super(message);
	}

	/**
	 * Constructs a new exception with the specified message and cause.
	 *
	 * @param message	The message.
	 * @param cause	The cause (which is saved for later retrieval by the {@link #getCause()} method). (A {@code null} value is
	 * 	permitted, and indicates that the cause is nonexistent or unknown.)
	 */
	protected FieldException(final String message, final Throwable cause){
		super(message, cause);
	}

	/**
	 * Constructs a new exception with the specified cause.
	 *
	 * @param cause	The cause (which is saved for later retrieval by the {@link #getCause()} method). (A {@code null} value is
	 * 	permitted, and indicates that the cause is nonexistent or unknown.)
	 */
	protected FieldException(final Throwable cause){
		super(cause);
	}

	/**
	 * Adds class and field names to the exception.
	 *
	 * @param type	The class type.
	 * @param field	The field.
	 * @return	The exception itself.
	 */
	public final FieldException withClassAndField(final Class<?> type, final Field field){
		return withClassNameAndFieldName(type.getName(), field.getName());
	}

	/**
	 * Adds class and field names to the exception.
	 *
	 * @param className	The class name.
	 * @param fieldName	The field name.
	 * @return	The exception itself.
	 */
	public final FieldException withClassNameAndFieldName(final String className, final String fieldName){
		this.className = className;
		this.fieldName = fieldName;

		return this;
	}

	@Override
	public final String getMessage(){
		return super.getMessage()
			+ (className != null && fieldName != null? " in field " + className + "." + fieldName: JavaHelper.EMPTY_STRING);
	}

}
