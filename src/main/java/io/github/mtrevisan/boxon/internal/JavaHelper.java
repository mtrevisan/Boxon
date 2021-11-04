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

import org.slf4j.helpers.MessageFormatter;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;


public final class JavaHelper{

	/** An empty immutable {@code String} array. */
	public static final String[] EMPTY_ARRAY = new String[0];


	private JavaHelper(){}

	public static String format(final String message, final Object... parameters){
		return MessageFormatter.arrayFormat(message, parameters)
			.getMessage();
	}


	/**
	 * Converts an array of bytes into a string representing the hexadecimal values of each byte in order.
	 *
	 * @param array	Array to be converted to hexadecimal characters.
	 * @return	The hexadecimal characters.
	 */
	public static String toHexString(final byte[] array){
		final int length = lengthOrZero(array);
		final StringBuilder sb = new StringBuilder(length << 1);
		for(int i = 0; i < length; i ++){
			final byte elem = array[i];
			sb.append(Character.forDigit((elem >>> 4) & 0x0F, 16));
			sb.append(Character.forDigit((elem & 0x0F), 16));
		}
		return sb.toString().toUpperCase(Locale.ROOT);
	}

	/**
	 * Converts a string representing the hexadecimal values of each byte to an array of bytes in order.
	 *
	 * @param hexString	The hexadecimal string.
	 * @return	Array of converted hexadecimal characters.
	 */
	public static byte[] toByteArray(final CharSequence hexString){
		final int len = lengthOrZero(hexString);
		if(len % 2 != 0)
			throw new IllegalArgumentException("Input should be of even length, was " + len);

		final byte[] data = new byte[len >>> 1];
		for(int i = 0; i < len; i += 2){
			data[i >>> 1] = (byte)((Character.digit(hexString.charAt(i), 16) << 4)
				+ Character.digit(hexString.charAt(i + 1), 16));
		}
		return data;
	}


	/**
	 * <p>Splits the provided text into an array with a maximum length, separators specified.</p>
	 *
	 * <p>The separator is not included in the returned String array.
	 * Adjacent separators are treated as one separator.</p>
	 *
	 * <p>A {@code null} input String returns {@code null}.
	 * A {@code null} `separatorChars` splits on whitespace.</p>
	 *
	 * <p>If more than {@code max} delimited substrings are found, the last
	 * returned string includes all characters after the first {@code max - 1}
	 * returned strings (including separator characters).</p>
	 *
	 * <pre>
	 * split(null, *, *)            = null
	 * split("", *, *)              = []
	 * split("ab cd ef", null, 0)   = ["ab", "cd", "ef"]
	 * split("ab   cd ef", null, 0) = ["ab", "cd", "ef"]
	 * split("ab:cd:ef", ":", 0)    = ["ab", "cd", "ef"]
	 * split("ab:cd:ef", ":", 2)    = ["ab", "cd:ef"]
	 * </pre>
	 *
	 * @param str	The text to parse, may be {@code null}.
	 * @param separator	The character used as the delimiters.
	 * @param max	The maximum number of elements to include in the array. A zero or negative value implies no limit.
	 * @return	An array of parsed Strings, {@code null} if {@code null} String input.
	 */
	public static String[] split(final String str, final char separator, final int max){
		if(str == null)
			return null;

		final int length = str.length();
		if(length == 0)
			return EMPTY_ARRAY;

		final List<String> list = new ArrayList<>();
		int sizePlus1 = 1;
		int i = 0;
		int start = 0;
		boolean match = false;
		while(i < length){
			if(str.charAt(i) == separator){
				if(match){
					if(sizePlus1 ++ == max)
						i = length;
					list.add(str.substring(start, i));
					match = false;
				}
				start = ++ i;
				continue;
			}
			match = true;
			i ++;
		}

		if(match)
			list.add(str.substring(start, i));
		return list.toArray(EMPTY_ARRAY);
	}

	/**
	 * <p>Splits the provided text into an array with a maximum length, separators specified.</p>
	 *
	 * <p>The separator is not included in the returned String array.
	 * Adjacent separators are treated as one separator.</p>
	 *
	 * <p>A {@code null} input String returns {@code null}.
	 * A {@code null} `separatorChars` splits on whitespace.</p>
	 *
	 * <p>If more than {@code max} delimited substrings are found, the last
	 * returned string includes all characters after the first {@code max - 1}
	 * returned strings (including separator characters).</p>
	 *
	 * <pre>
	 * split(null, *, *)            = null
	 * split("", *, *)              = []
	 * split("ab cd ef", null, 0)   = ["ab", "cd", "ef"]
	 * split("ab   cd ef", null, 0) = ["ab", "cd", "ef"]
	 * split("ab:cd:ef", ":", 0)    = ["ab", "cd", "ef"]
	 * split("ab:cd:ef", ":", 2)    = ["ab", "cd:ef"]
	 * </pre>
	 *
	 * @param str	The text to parse, may be {@code null}.
	 * @param separatorChars	The characters used as the delimiters, {@code null} splits on whitespace.
	 * @param max	The maximum number of elements to include in the array. A zero or negative value implies no limit.
	 * @return	An array of parsed Strings, {@code null} if {@code null} String input.
	 */
	public static String[] split(final String str, final String separatorChars, final int max){
		Objects.requireNonNull(separatorChars, "Separators must be valued");
		if(separatorChars.length() == 1)
			return split(str, separatorChars.charAt(0), max);
		if(str == null)
			return null;

		final int length = str.length();
		if(length == 0)
			return EMPTY_ARRAY;

		final List<String> list = new ArrayList<>();
		int sizePlus1 = 1;
		int i = 0;
		int start = 0;
		boolean match = false;
		while(i < length){
			if(separatorChars.indexOf(str.charAt(i)) >= 0){
				if(match){
					if(sizePlus1 ++ == max)
						i = length;
					list.add(str.substring(start, i));
					match = false;
				}
				start = ++ i;
				continue;
			}
			match = true;
			i ++;
		}

		if(match)
			list.add(str.substring(start, i));
		return list.toArray(EMPTY_ARRAY);
	}


	/**
	 * <p>Checks if a text is empty (""), {@code null} or whitespace only.</p>
	 *
	 * <p>Whitespace is defined by {@link Character#isWhitespace(char)}.</p>
	 *
	 * <pre>
	 * isBlank(null)      = true
	 * isBlank("")        = true
	 * isBlank(" ")       = true
	 * isBlank("bob")     = false
	 * isBlank("  bob  ") = false
	 * </pre>
	 *
	 * @param text	The text to check, may be {@code null}.
	 * @return	Whether the given text is {@code null}, empty or whitespace only.
	 */
	public static boolean isBlank(final CharSequence text){
		for(int i = 0; i < lengthOrZero(text); i ++)
			if(!Character.isWhitespace(text.charAt(i)))
				return false;
		return true;
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
	 * isNumeric("\u0967\u0968\u0969")  = true
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
	public static boolean isNumeric(final CharSequence text){
		if(text == null || text.length() == 0)
			return false;

		final int sz = text.length();
		for(int i = 0; i < sz; i ++)
			if(!Character.isDigit(text.charAt(i)))
				return false;
		return true;
	}

	public static Object getValueOrDefault(final Class<?> fieldType, final Object value){
		return (String.class.isInstance(value)
			? getValue(fieldType, (String)value)
			: value);
	}

	public static Object getValue(final Class<?> fieldType, final String value){
		if(fieldType == String.class)
			return value;
		if(isBlank(value))
			return null;

		try{
			final Class<?> objectiveType = ParserDataType.toObjectiveTypeOrSelf(fieldType);
			final boolean hexadecimal = value.startsWith("0x");
			final boolean octal = (!hexadecimal && !value.isEmpty() && value.charAt(0) == '0');
			final Method method = (hexadecimal || octal
				? objectiveType.getDeclaredMethod("valueOf", String.class, int.class)
				: objectiveType.getDeclaredMethod("valueOf", String.class));
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


	public static Enum<?> extractEnum(final Enum<?>[] enumConstants, final String value){
		for(int i = 0; i < enumConstants.length; i ++)
			if(enumConstants[i].name().equals(value))
				return enumConstants[i];
		return null;
	}


	public static <T> T nonNullOrDefault(final T obj, final T defaultObject){
		return (obj != null? obj: defaultObject);
	}

	public static int lengthOrZero(final CharSequence text){
		return (text != null? text.length(): 0);
	}

	public static int lengthOrZero(final byte[] array){
		return (array != null? array.length: 0);
	}

	public static <T> int lengthOrZero(final T[] array){
		return (array != null? array.length: 0);
	}

}
