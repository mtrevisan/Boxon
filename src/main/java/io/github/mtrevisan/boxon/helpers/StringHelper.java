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

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Locale;


/**
 * A collection of convenience methods for working with {@link String} objects.
 */
public final class StringHelper{

	private StringHelper(){}


	/**
	 * Performs argument substitution.
	 *
	 * @param message	The message pattern which will be parsed and formatted (see {@link MessageFormatter}).
	 * @param parameters	The arguments to be substituted in place of the formatting anchors.
	 * @return	The formatted message.
	 */
	public static String format(final String message, final Object... parameters){
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
	 * Split the given text into an array, separator specified.
	 * <p>
	 *    The separator is not included in the returned String array.
	 * Adjacent separators are treated as one separator.
	 * </p>
	 *
	 * @param text	The text to parse.
	 * @param separatorChar	The character used as the delimiter.
	 * @return	An array of parsed strings.
	 */
	public static String[] split(final String text, final char separatorChar){
		final int length = text.length();
		if(length == 0)
			return JavaHelper.EMPTY_STRING_ARRAY;

		final byte[] bytes = text.getBytes();
		final Collection<String> list = new ArrayList<>(length >> 1);
		int i = 0;
		int start = 0;
		boolean match = false;
		while(i < length){
			if(bytes[i] == separatorChar){
				if(match){
					list.add(text.substring(start, i));
					match = false;
				}

				start = ++ i;
			}
			else{
				match = true;
				i ++;
			}
		}
		if(match)
			list.add(text.substring(start, i));
		return list.toArray(String[]::new);
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
		final int length = JavaHelper.lengthOrZero(text);
		if(length > 0){
			final byte[] bytes = text.getBytes();
			for(int i = 0; i < length; i ++)
				if(!Character.isWhitespace(bytes[i]))
					return false;
		}
		return true;
	}


	/**
	 * Converts an array of bytes into a string representing the hexadecimal values of each byte in order.
	 *
	 * @param array	Array to be converted to hexadecimal characters.
	 * @return	The hexadecimal characters.
	 */
	public static String toHexString(final byte[] array){
		final int length = JavaHelper.lengthOrZero(array);
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
	 */
	public static byte[] hexToByteArray(final String hexString){
		final int length = JavaHelper.lengthOrZero(hexString);
		if(length % 2 != 0)
			throw new IllegalArgumentException("Input should be of even length, was " + length);

		final byte[] data = new byte[length >>> 1];
		if(length > 0){
			final byte[] bytes = hexString.getBytes();
			for(int i = 0; i < length; i += 2){
				final int highDigit = Character.digit(bytes[i], 16);
				final int lowDigit = Character.digit(bytes[i + 1], 16);

				data[i >>> 1] = (byte)((highDigit << 4) + lowDigit);
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
		final int length = JavaHelper.lengthOrZero(array);
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
		final int length = JavaHelper.lengthOrZero(asciiString);
		final byte[] data = new byte[length];
		if(length > 0){
			final byte[] bytes = asciiString.getBytes();
			for(int i = 0; i < length; i ++)
				data[i] = bytes[i];
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
		if(JavaHelper.lengthOrZero(byteArray) < asciiString.length())
			return false;

		final byte[] stringBytes = asciiString.getBytes(StandardCharsets.US_ASCII);
		for(int i = 0, length = stringBytes.length; i < length; i ++)
			if(byteArray[i] != stringBytes[i])
				return false;
		return true;
	}

}
