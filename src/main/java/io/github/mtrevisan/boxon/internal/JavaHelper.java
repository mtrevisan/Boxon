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
package io.github.mtrevisan.boxon.internal;

import io.github.mtrevisan.boxon.exceptions.CodecException;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;


public final class JavaHelper{

	/** An empty immutable {@code String} array. */
	public static final String[] EMPTY_ARRAY = new String[0];

	static final String METHOD_VALUE_OF = "valueOf";


	private JavaHelper(){}

	public static Object getValueOrDefault(final Class<?> fieldType, final Object value) throws CodecException{
		return (String.class.isInstance(value)
			? getValue(fieldType, (String)value)
			: value);
	}

	@SuppressWarnings("ReturnOfNull")
	public static Object getValue(final Class<?> fieldType, final String value) throws CodecException{
		if(fieldType == String.class)
			return value;
		if(value == null || value.isEmpty())
			return null;

		final Class<?> objectiveType = ParserDataType.toObjectiveTypeOrSelf(fieldType);
		//try convert to a number...
		final Object val = StringHelper.toNumber(value, objectiveType);
		//... otherwise convert it to an object
		return (val == null? toObjectValue(value, objectiveType): val);
	}

	private static Object toObjectValue(final String value, final Class<?> objectiveType) throws CodecException{
		try{
			final Method method = objectiveType.getDeclaredMethod(METHOD_VALUE_OF, String.class);
			return method.invoke(null, value);
		}
		catch(final NoSuchMethodException | IllegalAccessException | InvocationTargetException ignored){
			throw CodecException.create("Cannot interpret {} as {}", value, objectiveType.getSimpleName());
		}
	}


	/**
	 * Convert the value to signed primitive.
	 *
	 * @param value	Field value.
	 * @param size	Length in bits of the field.
	 * @return	The 2-complement expressed as int.
	 */
	@SuppressWarnings("ShiftOutOfRange")
	public static long extendSign(final long value, final int size){
		if(size <= 0)
			throw new IllegalArgumentException("Size must be a positive value, was " + size);

		final int shift = -size;
		return (value << shift) >> shift;
	}


	public static <T> T nonNullOrDefault(final T obj, final T defaultObject){
		return (obj != null? obj: defaultObject);
	}

	public static <T> int lengthOrZero(final T[] array){
		return (array != null? array.length: 0);
	}

}
