/*
 * Copyright (c) 2021-2024 Mauro Trevisan
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
package io.github.mtrevisan.boxon.core.helpers.extractors;

import io.github.mtrevisan.boxon.exceptions.JSONPathException;
import io.github.mtrevisan.boxon.helpers.JavaHelper;
import io.github.mtrevisan.boxon.helpers.ReflectionHelper;
import io.github.mtrevisan.boxon.helpers.StringHelper;

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

	/**
	 * Map containing replacement characters for the RFC6901 encoding of JSON paths, where "~0" is the encoding of a tilde, and "~1" of
	 * a slash.
	 */
	private static final Map<Character, String> RFC6901_REPLACEMENT_MAP = Map.of(
		'1', "/",
		'0', "~"
	);

	private static final char DECODED_SLASH = '/';
	private static final String SLASH = String.valueOf(DECODED_SLASH);

	private static final Pattern PATTERN_HEX_URL_ENCODE = Pattern.compile("%x([0-9a-fA-F]{2})");


	private JSONPath(){}


	/**
	 * Extract the value from the given object pointed by the given path.
	 *
	 * @param path	The path used to reference the value in the data object.
	 * @param data	The data from which to extract the value.
	 * @param defaultValue	Default value if the path is not found.
	 * @param <T>	The value class type.
	 * @return	The value.
	 * @throws JSONPathException	If the path is not well formatted.
	 */
	@SuppressWarnings("unchecked")
	public static <T> T extract(final String path, final Object data, final T defaultValue) throws JSONPathException{
		if(path == null || !path.isEmpty()){
			final List<String> pathComponents = parsePath(path);
			return extract(pathComponents, data, defaultValue);
		}
		return (T)data;
	}

	private static List<String> parsePath(final String path) throws JSONPathException{
		if(path == null || path.charAt(0) != DECODED_SLASH)
			throw JSONPathException.create("invalid path '{}'", path);

		final List<String> components = StringHelper.split(path, DECODED_SLASH);
		for(int i = 0, length = components.size(); i < length; i ++){
			final String component = components.get(i);
			if(!StringHelper.isBlank(component))
				components.set(i, urlDecode(rfc6901Replace(component)));
		}
		return components;
	}

	/**
	 * Replaces occurrences of "~1" and "~0" in a string with replacement strings based on RFC6901.
	 *
	 * @param text	The string to be processed.
	 * @return	The processed string.
	 */
	private static StringBuilder rfc6901Replace(final String text){
		final StringBuilder sb = new StringBuilder(text);
		for(int i = 0, length = text.length() - 1; i < length; i ++)
			if(sb.charAt(i) == '~'){
				final char nextChar = sb.charAt(i + 1);
				final String replacement = RFC6901_REPLACEMENT_MAP.get(nextChar);
				if(replacement != null){
					sb.replace(i, i + 2, replacement);
					length --;
				}
			}
		return sb;
	}

	/**
	 *	Percent-decode a text.
	 *
	 * @param stringBuilder	The text to be URL decoded.
	 * @return	The URL-decoded text.
	 *
	 * @see <a href="https://datatracker.ietf.org/doc/html/rfc3986">RFC3986 - Uniform Resource Identifier (URI): Generic Syntax</a>
	 */
	private static String urlDecode(final StringBuilder stringBuilder){
		//convert (hexadecimal) %x?? to (decimal) %??:
		final String text = PATTERN_HEX_URL_ENCODE.matcher(stringBuilder)
			.replaceAll(match -> Character.toString((char)Integer.parseInt(match.group(1), 16)));
		return URLDecoder.decode(text, StandardCharsets.UTF_8);
	}

	@SuppressWarnings("unchecked")
	private static <T> T extract(final List<String> path, Object data, final T defaultValue) throws JSONPathException{
		for(int i = 0, length = path.size(); i < length; i ++){
			final String currentPath = path.get(i);

			final Integer idx = extractIndex(currentPath);

			validatePath(data, currentPath, idx, path);

			if(idx != null)
				data = extractPath(data, idx, defaultValue);
			else
				data = extractPath(data, currentPath, defaultValue);
		}
		return (T)data;
	}

	private static Integer extractIndex(final String currentPath){
		return (JavaHelper.isDecimalIntegerNumber(currentPath) && (currentPath.charAt(0) != '0' || currentPath.length() == 1)
			? Integer.valueOf(currentPath)
			: null);
	}

	private static void validatePath(final Object data, final String currentPath, final Integer idx, final List<String> path)
			throws JSONPathException{
		final Class<?> dataClass = data.getClass();
		if(idx != null ^ (dataClass.isArray() || List.class.isAssignableFrom(dataClass)))
			throw JSONPathException.create("No array field '{}' found on path '{}'", currentPath, toJSONPath(path));
	}

	private static String toJSONPath(final List<String> path){
		return SLASH + String.join(SLASH, path);
	}

	private static Object extractPath(final Object data, final Integer idx, final Object defaultValue){
		return (data instanceof final List<?> lst
			? (idx >= 0 && idx < lst.size()? lst.get(idx): defaultValue)
			: (idx >= 0 && idx < Array.getLength(data)? Array.get(data, idx): defaultValue));
	}

	private static Object extractPath(final Object data, final String currentPath, final Object defaultValue){
		Object nextData;
		if(data instanceof final Map<?, ?> m)
			nextData = (m.containsKey(currentPath)
				? m.get(currentPath)
				: defaultValue);
		else{
			final Class<?> cls = data.getClass();
			try{
				final Field currentField = cls.getDeclaredField(currentPath);
				ReflectionHelper.makeAccessible(currentField);
				nextData = ReflectionHelper.getValue(data, currentField);
			}
			catch(final NoSuchFieldException nsfe){
				nextData = defaultValue;
			}
		}
		return nextData;
	}

}
