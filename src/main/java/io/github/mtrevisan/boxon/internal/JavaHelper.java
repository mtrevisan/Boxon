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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;


public final class JavaHelper{

	/** An empty immutable {@code String} array. */
	public static final String[] EMPTY_ARRAY = new String[0];

	private static final String METHOD_VALUE_OF = "valueOf";


	private JavaHelper(){}

	public static Object getValueOrDefault(final Class<?> fieldType, final Object value){
		return (String.class.isInstance(value)
			? getValue(fieldType, (String)value)
			: value);
	}

	@SuppressWarnings("ReturnOfNull")
	public static Object getValue(final Class<?> fieldType, final String value){
		if(fieldType == String.class)
			return value;
		if(value == null || value.isEmpty())
			return null;

		try{
			final Class<?> objectiveType = ParserDataType.toObjectiveTypeOrSelf(fieldType);
			final boolean hexadecimal = value.startsWith("0x");
			final boolean octal = (!hexadecimal && value.charAt(0) == '0');
			final Method method = (hexadecimal || octal
				? objectiveType.getDeclaredMethod(METHOD_VALUE_OF, String.class, int.class)
				: objectiveType.getDeclaredMethod(METHOD_VALUE_OF, String.class));
			final Object response;
			if(hexadecimal)
				response = method.invoke(null, value.substring(2), 16);
			else if(octal)
				response = method.invoke(null, value, 8);
			else
				response = method.invoke(null, value);
			return response;
		}
		catch(final NoSuchMethodException | IllegalAccessException | InvocationTargetException ignored){
			return null;
		}
	}


	@SuppressWarnings("ReturnOfNull")
	public static Enum<?> extractEnum(final Enum<?>[] enumConstants, final String value){
		for(int i = 0; i < enumConstants.length; i ++)
			if(enumConstants[i].name().equals(value))
				return enumConstants[i];
		return null;
	}


	public static <T> T nonNullOrDefault(final T obj, final T defaultObject){
		return (obj != null? obj: defaultObject);
	}

	public static <T> int lengthOrZero(final T[] array){
		return (array != null? array.length: 0);
	}

}
