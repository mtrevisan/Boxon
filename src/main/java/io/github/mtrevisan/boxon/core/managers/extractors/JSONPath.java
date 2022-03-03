/*
 * Copyright (c) 2021-2022 Mauro Trevisan
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
package io.github.mtrevisan.boxon.core.managers.extractors;

import io.github.mtrevisan.boxon.exceptions.JSONPathException;
import io.github.mtrevisan.boxon.helpers.ReflectionHelper;
import io.github.mtrevisan.boxon.helpers.StringHelper;
import io.github.mtrevisan.boxon.io.ParserDataType;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;


/**
 * An implementation of <a href="https://tools.ietf.org/html/rfc6901">RFC6901 - JavaScript Object Notation (JSON) Pointer</a>.
 */
public final class JSONPath{

	private static final Pattern PATTERN_HEX_URL_ENCODE = Pattern.compile("%x([0-9a-fA-F]{2})");


	private static final String SLASH = "/";
	private static final char DECODED_TILDE = '~';
	private static final char DECODED_SLASH = '/';
	/** Encoding of a tilde. */
	private static final String TILDE_ZERO = "~0";
	/** Encoding of a slash. */
	private static final String TILDE_ONE = "~1";


	private JSONPath(){}


	/**
	 * Extract the value from the given object pointed by the given path.
	 *
	 * @param path	The path used to reference the value in the data object.
	 * @param data	The data from which to extract the value.
	 * @param <T>	The value class type.
	 * @return	The value.
	 * @throws JSONPathException	If the path is not well formatted.
	 */
	@SuppressWarnings("unchecked")
	public static <T> T extract(final String path, final Object data) throws JSONPathException{
		if(path == null || !path.isEmpty()){
			try{
				final String[] pathComponents = parsePath(path);
				return extract(pathComponents, data);
			}
			catch(final NoSuchFieldException nsfe){
				throw JSONPathException.create("No field '{}' found on path '{}'", nsfe.getMessage(), path);
			}
		}
		return (T)data;
	}

	private static String[] parsePath(final String path) throws JSONPathException{
		if(path == null || path.charAt(0) != DECODED_SLASH)
			throw JSONPathException.create("invalid path '{}'", path);

		final String[] components = StringHelper.split(path, DECODED_SLASH);
		for(int i = 0; i < components.length; i ++)
			if(!StringHelper.isBlank(components[i])){
				//NOTE: the order here is important!
				components[i] = replace(components[i], TILDE_ONE, DECODED_SLASH);
				components[i] = replace(components[i], TILDE_ZERO, DECODED_TILDE);

				components[i] = urlDecode(components[i]);
			}
		return components;
	}

	/**
	 *	Percent-decode a text.
	 *
	 * @param text	The text to be URL decoded.
	 * @return	The URL-decoded text.
	 *
	 * @see <a href="https://datatracker.ietf.org/doc/html/rfc3986">RFC3986 - Uniform Resource Identifier (URI): Generic Syntax</a>
	 */
	private static String urlDecode(String text){
		//convert (hexadecimal) %x?? to (decimal) %??:
		text = PATTERN_HEX_URL_ENCODE.matcher(text)
			.replaceAll(match -> Character.toString((char)Integer.parseInt(match.group(1), 16)));
		return URLDecoder.decode(text, StandardCharsets.UTF_8);
	}

	/**
	 * Replaces all occurrences of a String within another String.
	 * <p>A {@code null} reference passed to this method is a no-op.</p>
	 *
	 * <pre>
	 * StringUtils.replace(null, *, *)        = null
	 * StringUtils.replace("", *, *)          = ""
	 * StringUtils.replace("any", null, *)    = "any"
	 * StringUtils.replace("any", *, null)    = "any"
	 * StringUtils.replace("any", "", *)      = "any"
	 * StringUtils.replace("aba", "a", null)  = "aba"
	 * StringUtils.replace("aba", "a", "")    = "b"
	 * StringUtils.replace("aba", "a", "z")   = "zbz"
	 * </pre>
	 *
	 * @param text	Text to search and replace in, may be {@code null}.
	 * @param searchString	The String to search for, may be {@code null}.
	 * @param replacement	The String to replace it with, may be {@code null}.
	 * @return	The text with any replacements processed, {@code null} if {@code null} String input.
	 */
	private static String replace(final String text, final String searchString, final char replacement){
		int start = 0;
		int end = text.indexOf(searchString, start);
		if(end < 0)
			return text;

		final int replacementLength = searchString.length();
		final StringBuilder sb = new StringBuilder(text.length());
		while(end >= 0){
			sb.append(text, start, end)
				.append(replacement);

			start = end + replacementLength;
			end = text.indexOf(searchString, start);
		}
		sb.append(text, start, text.length());
		return sb.toString();
	}

	@SuppressWarnings("unchecked")
	private static <T> T extract(final String[] path, Object data) throws JSONPathException, NoSuchFieldException{
		for(int i = 0; i < path.length; i ++){
			final String currentPath = path[i];

			final Integer idx = extractIndex(currentPath);

			validatePath(data, currentPath, idx, path);

			if(idx != null)
				data = extractPath(data, idx);
			else
				data = extractPath(data, currentPath);
		}
		return (T)data;
	}

	@SuppressWarnings("ReturnOfNull")
	private static Integer extractIndex(final String currentPath){
		return (ParserDataType.isDecimalNumber(currentPath) && (currentPath.charAt(0) != '0' || currentPath.length() <= 1)
			? Integer.valueOf(currentPath)
			: null);
	}

	private static void validatePath(final Object data, final String currentPath, final Integer idx, final String[] path)
			throws JSONPathException{
		final Class<?> dataClass = data.getClass();
		if(idx != null ^ (dataClass.isArray() || List.class.isAssignableFrom(dataClass)))
			throw JSONPathException.create("No array field '{}' found on path '{}'", currentPath, toJSONPath(path));
	}

	private static String toJSONPath(final String[] path){
		return SLASH + String.join(SLASH, path);
	}

	private static Object extractPath(final Object data, final Integer idx){
		return (List.class.isInstance(data)
			? ((List<?>)data).get(idx)
			: Array.get(data, idx));
	}

	private static Object extractPath(final Object data, final String currentPath) throws NoSuchFieldException{
		final Object nextData;
		if(Map.class.isInstance(data))
			nextData = ((Map<?, ?>)data).get(currentPath);
		else{
			final Field currentField = data.getClass().getDeclaredField(currentPath);
			currentField.setAccessible(true);
			nextData = ReflectionHelper.getValue(data, currentField);
		}
		return nextData;
	}

}
