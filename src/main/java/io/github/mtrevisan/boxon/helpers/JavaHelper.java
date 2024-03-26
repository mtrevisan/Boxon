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

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.Collection;


/**
 * A collection of convenience methods for simplifying Java capabilities.
 */
public final class JavaHelper{

	/** An empty {@code String}. */
	public static final String EMPTY_STRING = "";

	private static final String HEXADECIMAL_PREFIX = "0x";


	private JavaHelper(){}


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


	public static boolean isIntegerNumber(final String text){
		return (isDecimalIntegerNumber(text) || isHexadecimalNumber(text));
	}

	/**
	 * <p>Checks if the text contains only Unicode digits.
	 * A decimal point is not a Unicode digit and returns false.</p>
	 *
	 * <p>{@code null} will return {@code false}.
	 * An empty text ({@code length() = 0}) will return {@code false}.</p>
	 *
	 * <p>Note that the method does not allow for a leading sign, either positive or negative.
	 * Also, if a String passes the numeric test, it may still generate a NumberFormatException
	 * when parsed by Integer.parseInt or Long.parseLong, e.g. if the value is outside the range
	 * for int or long respectively.</p>
	 *
	 * <pre>
	 * isNumeric(null)   = false
	 * isNumeric("")     = false
	 * isNumeric("  ")   = false
	 * isNumeric("123")  = true
	 * isNumeric("\u0967\u0968\u0969") = true
	 * isNumeric("12 3") = false
	 * isNumeric("ab2c") = false
	 * isNumeric("12-3") = false
	 * isNumeric("12.3") = false
	 * isNumeric("-123") = false
	 * isNumeric("+123") = false
	 * </pre>
	 *
	 * @param text	The text to check, may be {@code null}.
	 * @return	Whether the given text contains only digits and is non-{@code null}.
	 */
	public static boolean isDecimalIntegerNumber(final String text){
		return isDecimalIntegerNumber(text, 0, 10);
	}

	private static boolean isDecimalIntegerNumber(final String text, final int offset, final int radix){
		if(StringHelper.isBlank(text))
			return false;

		final byte[] bytes = text.getBytes(StandardCharsets.US_ASCII);
		for(int i = offset, length = bytes.length; i < length; i ++){
			final byte chr = bytes[i];

			if(Character.digit(chr, radix) < 0)
				return false;
		}
		return true;
	}

	private static boolean isHexadecimalNumber(final String text){
		return (!StringHelper.isBlank(text) && text.startsWith(HEXADECIMAL_PREFIX) && isDecimalIntegerNumber(text, 2, 16));
	}

	/**
	 * Convert a string value (decimal or hexadecimal) to {@link BigInteger}.
	 *
	 * @param value	The value.
	 * @return	The converted {@link BigInteger}.
	 */
	public static BigInteger toBigInteger(final String value){
		try{
			if(isHexadecimalNumber(value))
				return new BigInteger(value.substring(2), 16);

			return new BigInteger(value);
		}
		catch(final NumberFormatException ignored){
			return null;
		}
	}

	/**
	 * Convert a string value (decimal or hexadecimal if integer) to {@link BigDecimal}.
	 *
	 * @param value	The value.
	 * @return	The converted {@link BigDecimal}.
	 */
	public static BigDecimal toBigDecimal(final String value){
		try{
			if(isHexadecimalNumber(value))
				return new BigDecimal(new BigInteger(value.substring(2), 16).toString());

			return new BigDecimal(value);
		}
		catch(final NumberFormatException ignored){
			return null;
		}
	}

}
