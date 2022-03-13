/*
 * Copyright (c) 2020-2022 Mauro Trevisan
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

import java.util.ArrayList;
import java.util.Arrays;
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

		return repeat(padChar, pads) + text;
	}

	/**
	 * Returns padding using the specified delimiter repeated to a given length.
	 *
	 * @param chr	Character to repeat.
	 * @param count	Number of times to repeat char.
	 * @return	String with repeated character.
	 */
	public static String repeat(final char chr, final int count){
		final char[] buf = new char[count];
		Arrays.fill(buf, chr);
		return new String(buf);
	}


	/**
	 * Split the given text into an array, separator specified.
	 * <p>
	 *    The separator is not included in the returned String array.
	 * Adjacent separators are treated as one separator.
	 * </p>
	 *
	 * @param str	The text to parse.
	 * @param separatorChar	The character used as the delimiter.
	 * @return	An array of parsed strings.
	 */
	public static String[] split(final String str, final char separatorChar){
		final int len = str.length();
		if(len == 0)
			return JavaHelper.EMPTY_STRING_ARRAY;

		final Collection<String> list = new ArrayList<>(str.length() >> 1);
		int i = 0;
		int start = 0;
		boolean match = false;
		while(i < len){
			if(str.charAt(i) == separatorChar){
				if(match){
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
		return list.toArray(String[]::new);
	}


	/**
	 * Converts an array of bytes into a string representing the hexadecimal values of each byte in order.
	 *
	 * @param array	Array to be converted to hexadecimal characters.
	 * @return	The hexadecimal characters.
	 */
	public static String toHexString(final byte[] array){
		final int length = JavaHelper.lengthOrZero(array);
		final StringBuilder sb = new StringBuilder(length << 1);
		for(int i = 0; i < length; i ++){
			final byte elem = array[i];
			final char highDigit = Character.forDigit((elem >>> 4) & 0x0F, 16);
			final char lowDigit = Character.forDigit(elem & 0x0F, 16);
			sb.append(highDigit);
			sb.append(lowDigit);
		}
		return sb.toString()
			.toUpperCase(Locale.ROOT);
	}

	/**
	 * Converts a string representing the hexadecimal values of each byte to an array of bytes in order.
	 *
	 * @param hexString	The hexadecimal string.
	 * @return	Array of converted hexadecimal characters.
	 */
	public static byte[] toByteArray(final CharSequence hexString){
		final int len = JavaHelper.lengthOrZero(hexString);
		if(len % 2 != 0)
			throw new IllegalArgumentException("Input should be of even length, was " + len);

		final byte[] data = new byte[len >>> 1];
		for(int i = 0; i < len; i += 2){
			final int highDigit = Character.digit(hexString.charAt(i), 16);
			final int lowDigit = Character.digit(hexString.charAt(i + 1), 16);
			data[i >>> 1] = (byte)((highDigit << 4) + lowDigit);
		}
		return data;
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
		for(int i = 0; i < JavaHelper.lengthOrZero(text); i ++)
			if(!Character.isWhitespace(text.charAt(i)))
				return false;
		return true;
	}

}
