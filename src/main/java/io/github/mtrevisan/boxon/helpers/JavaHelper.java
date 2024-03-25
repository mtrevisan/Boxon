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
package io.github.mtrevisan.boxon.helpers;

import io.github.mtrevisan.boxon.exceptions.DataException;

import java.util.Collection;


/**
 * A collection of convenience methods for simplifying Java capabilities.
 */
public final class JavaHelper{

	/** An empty {@code String}. */
	public static final String EMPTY_STRING = "";

	/** An empty {@code String} array. */
	static final String[] EMPTY_STRING_ARRAY = new String[0];


	private JavaHelper(){}


	/**
	 * Convert the value to signed primitive.
	 *
	 * @param value	Field value.
	 * @param size	Length in bits of the field.
	 * @return	The 2-complement expressed as int.
	 * @throws DataException	If the value is non-positive.
	 */
	@SuppressWarnings("ShiftOutOfRange")
	public static long extendSign(final long value, final int size){
		if(size <= 0)
			throw DataException.create("Size must be a positive value, was {}", size);

		final int shift = -size;
		return (value << shift) >> shift;
	}


	public static int roundBitsToByte(final int bits){
		return roundTo(bits, Byte.SIZE) >>> 3;
	}

	private static int roundTo(final int value, final int roundTo){
		return (value + roundTo - 1) & -roundTo;
	}


	/**
	 * Return the given object if non-null, the default object otherwise.
	 *
	 * @param obj	The object.
	 * @param defaultObject	The default object to be returned if the given object is {@code null}.
	 * @param <T>	The class type of the object.
	 * @return	The object, or the default object if {@code null}.
	 */
	public static <T> T nonNullOrDefault(final T obj, final T defaultObject){
		return (obj != null? obj: defaultObject);
	}


	/**
	 * Return the length of the text, or {@code 0} if {@code null}.
	 *
	 * @param text	The text.
	 * @return	The length of the text, or {@code 0} if {@code null}.
	 */
	public static int lengthOrZero(final CharSequence text){
		return (text != null? text.length(): 0);
	}

	/**
	 * Return the length of the array, or {@code 0} if {@code null}.
	 *
	 * @param array	The array.
	 * @return	The length of the array, or {@code 0} if {@code null}.
	 */
	static int lengthOrZero(final byte[] array){
		return (array != null? array.length: 0);
	}

	/**
	 * Return the length of the array, or {@code 0} if {@code null}.
	 *
	 * @param array	The array.
	 * @param <T>	The class type of the array.
	 * @return	The length of the array, or {@code 0} if {@code null}.
	 */
	public static <T> int lengthOrZero(final T[] array){
		return (array != null? array.length: 0);
	}

	/**
	 * Return the length of the list, or {@code 0} if {@code null}.
	 *
	 * @param array	The list.
	 * @param <T>	The class type of the list.
	 * @return	The length of the list, or {@code 0} if {@code null}.
	 */
	public static <T> int lengthOrZero(final Collection<T> array){
		return (array != null? array.size(): 0);
	}

}
