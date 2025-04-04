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

import org.slf4j.helpers.MessageFormatter;

import java.lang.reflect.Array;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;
import java.util.Locale;
import java.util.regex.Pattern;


/**
 * A collection of convenience methods for working with {@link String} objects.
 */
public final class StringHelper{

	/** An empty {@code String} array. */
	private static final String[] EMPTY_STRING_ARRAY = new String[0];


	private StringHelper(){}


	/**
	 * Checks whether the provided text matches the given pattern.
	 *
	 * @param text	The text to check against the pattern.
	 * @param pattern	The pattern to match against the text.
	 * @return	Whether the text matches the pattern.
	 */
	public static boolean matches(final CharSequence text, final Pattern pattern){
		return pattern.matcher(text)
			.matches();
	}

	/**
	 * Checks whether the provided text matches the given pattern or is blank.
	 *
	 * @param text	The text to check against the pattern.
	 * @param pattern	The pattern to match against the text.
	 * @return	Whether the text matches the pattern or is blank.
	 */
	public static boolean matchesOrBlank(final String text, final Pattern pattern){
		return (isBlank(text) || matches(text, pattern));
	}


	/**
	 * Performs argument substitution.
	 *
	 * @param message	The message pattern which will be parsed and formatted (see {@link MessageFormatter}).
	 * @param parameters	The arguments to be substituted in place of the formatting anchors.
	 * @return	The formatted message.
	 */
	public static String format(final String message, Object... parameters){
		if(parameters.getClass().isArray() && Array.getLength(parameters) == 1 && parameters[0].getClass().isArray())
			parameters = (Object[])parameters[0];
		return MessageFormatter.arrayFormat(message, parameters)
			.getMessage();
	}

	/**
	 * Left pad a string with a specified character.
	 *
	 * @param text	The string to pad out, must not be {@code null}.
	 * @param size	The size to pad to.
	 * @param padChar	The character to pad with.
	 * @return	Left padded string or original string if no padding is necessary.
	 */
	public static String leftPad(final String text, final int size, final char padChar){
		final int pads = size - text.length();
		if(pads <= 0)
			return text;

		return new StringBuilder()
			.repeat(padChar, pads)
			.append(text)
			.toString();
	}

	/**
	 * Checks if the given text contains the specified character.
	 *
	 * @param text	The text to search within.
	 * @param chr	The character to search for.
	 * @return	Whether the text contains the character.
	 */
	public static boolean contains(final String text, final char chr){
		return (text.indexOf(chr) > 0);
	}

	/**
	 * Split the given text into an array, separator specified.
	 * <p>
	 * The separator is not included in the returned String array.
	 * Adjacent separators are treated as one separator.
	 * </p>
	 *
	 * @param text	The text to parse.
	 * @param separatorChar	The character used as the delimiter.
	 * @return	A list of parsed strings.
	 */
	public static String[] split(final String text, final char separatorChar){
		return split(text, 0, separatorChar);
	}

	/**
	 * Split the given text into an array, separator specified.
	 * <p>
	 * The separator is not included in the returned String array.
	 * Adjacent separators are treated as one separator.
	 * </p>
	 *
	 * @param text	The text to parse.
	 * @param fromIndex	Index in text to start from.
	 * @param separatorChar	The character used as the delimiter.
	 * @return	A list of parsed strings.
	 */
	public static String[] split(final String text, final int fromIndex, final char separatorChar){
		final int length = text.length();
		if(length == 0)
			return EMPTY_STRING_ARRAY;

		final byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
		final String[] result = createSplitResult(bytes, fromIndex, separatorChar);

		int currentIndex = 0;
		int start = fromIndex;
		for(int i = fromIndex; i < length; i ++)
			if(bytes[i] == separatorChar){
				if(start != i)
					result[currentIndex ++] = text.substring(start, i);

				start = i + 1;
			}
		if(start != length)
			result[currentIndex] = text.substring(start);

		return result;
	}

	private static String[] createSplitResult(final byte[] bytes, final int fromIndex, final char separatorChar){
		final int length = bytes.length;
		int count = (bytes[fromIndex] == separatorChar? 0: 1);
		for(int i = fromIndex; i < length; i ++)
			if(bytes[i] == separatorChar)
				count ++;
		return new String[count];
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
	public static boolean isBlank(final String text){
		final int length = JavaHelper.sizeOrZero(text);
		if(length > 0){
			final byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
			for(int i = 0; i < length; i ++)
				if(!Character.isWhitespace(bytes[i]) || bytes[i] == '\r' || bytes[i] == '\n')
					return false;
		}
		return true;
	}

	/**
	 * Checks if the provided value is an empty string, empty collection, or void.
	 *
	 * @param value	The value to check.
	 * @return	Whether the value is an empty string, empty collection, or void.
	 */
	public static boolean isEmptyStringOrCollectionOrVoid(final Object value){
		return ((value instanceof final String v && isBlank(v))
			|| (value instanceof final Collection<?> c && c.isEmpty())
			|| value == void.class);
	}


	/**
	 * Converts a decimal value into the corresponding hexadecimal string.
	 *
	 * @param value	Value to be converted to hexadecimal characters.
	 * @return	The hexadecimal characters.
	 */
	public static String toHexString(final long value){
		return Long.toHexString(value)
			.toUpperCase(Locale.ROOT);
	}


	/**
	 * Converts a decimal value into the corresponding hexadecimal string, padded to `size` bytes.
	 *
	 * @param value	Value to be converted to hexadecimal characters.
	 * @param size	Number of bytes used to pad the final hexadecimal value.
	 * @return	The hexadecimal characters.
	 */
	public static String toHexString(final long value, final int size){
		final long mask = (size >= Byte.SIZE? -1l: (1l << (size << 3)) - 1);
		final String hex = toHexString(value & mask);
		return leftPad(hex, size << 1, '0');
	}

	/**
	 * Converts a decimal value into the corresponding hexadecimal string.
	 *
	 * @param value	Value to be converted to hexadecimal characters.
	 * @return	The hexadecimal characters.
	 */
	public static String toHexString(final BigInteger value){
		return value.toString(16)
			.toUpperCase(Locale.ROOT);
	}

	/**
	 * Converts a number into the corresponding hexadecimal string.
	 *
	 * @param value	Number to be converted to hexadecimal characters.
	 * @return	The hexadecimal characters.
	 */
	public static String toHexString(final Number value){
		return toHexString(JavaHelper.convertToBigInteger(value.toString()));
	}

	/**
	 * Converts an array of bytes into a string representing the hexadecimal values of each byte in order.
	 *
	 * @param array	Array to be converted to hexadecimal characters.
	 * @return	The hexadecimal characters.
	 */
	public static String toHexString(final byte[] array){
		final int length = JavaHelper.sizeOrZero(array);
		final char[] hexChars = new char[length << 1];
		for(int i = 0; i < length; i ++){
			final int elem = array[i] & 0xFF;

			final char highDigit = Character.forDigit((elem >>> 4) & 0x0F, 16);
			final char lowDigit = Character.forDigit(elem & 0x0F, 16);
			hexChars[i << 1] = highDigit;
			hexChars[(i << 1) + 1] = lowDigit;
		}
		return new String(hexChars)
			.toUpperCase(Locale.ROOT);
	}

	/**
	 * Converts a string representing the hexadecimal values of each byte to an array of bytes in order.
	 *
	 * @param hexString	The hexadecimal string.
	 * @return	Array of converted hexadecimal characters.
	 * @throws IllegalArgumentException	If the input has an odd length.
	 */
	public static byte[] hexToByteArray(final String hexString){
		final int length = JavaHelper.sizeOrZero(hexString);
		if(length % 2 != 0)
			throw new IllegalArgumentException("Input should be of even length, was " + length);

		final byte[] data = new byte[length >>> 1];
		if(length > 0){
			final byte[] bytes = hexString.getBytes(StandardCharsets.US_ASCII);
			for(int i = 0; i < length; i += 2){
				final int highDigit = Character.digit(bytes[i], 16);
				final int lowDigit = Character.digit(bytes[i + 1], 16);

				data[i >>> 1] = (byte)((highDigit << 4) | lowDigit);
			}
		}
		return data;
	}


	/**
	 * Converts an array of bytes into a string.
	 *
	 * @param array	Array to be converted to characters.
	 * @return	The characters.
	 */
	public static String toASCIIString(final byte[] array){
		final int length = JavaHelper.sizeOrZero(array);
		final char[] chars = new char[length];
		for(int i = 0; i < length; i ++)
			chars[i] = (char)(array[i] & 0xFF);
		return new String(chars);
	}

	/**
	 * Converts a string representing the values of each byte to an array of bytes in order.
	 *
	 * @param asciiString	The string.
	 * @return	Array of converted characters.
	 */
	public static byte[] asciiToByteArray(final String asciiString){
		final int length = JavaHelper.sizeOrZero(asciiString);
		final byte[] data = new byte[length];
		if(length > 0){
			final byte[] bytes = asciiString.getBytes(StandardCharsets.US_ASCII);
			System.arraycopy(bytes, 0, data, 0, length);
		}
		return data;
	}

	/**
	 * Checks if a byte array starts with a given ASCII string.
	 *
	 * @param byteArray	The byte array to check.
	 * @param asciiString	The ASCII string to check for.
	 * @return	Whether the byte array starts with the ASCII string.
	 */
	public static boolean byteArrayStartsWith(final byte[] byteArray, final String asciiString){
		if(JavaHelper.sizeOrZero(byteArray) < asciiString.length())
			return false;

		final byte[] stringBytes = asciiString.getBytes(StandardCharsets.US_ASCII);
		return Arrays.equals(byteArray, 0, stringBytes.length, stringBytes, 0, stringBytes.length);
	}

}
