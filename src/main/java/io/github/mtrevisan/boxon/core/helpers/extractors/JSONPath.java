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

import io.github.mtrevisan.boxon.core.helpers.FieldAccessor;
import io.github.mtrevisan.boxon.core.helpers.FieldMapper;
import io.github.mtrevisan.boxon.exceptions.JSONPathException;
import io.github.mtrevisan.boxon.helpers.JavaHelper;
import io.github.mtrevisan.boxon.helpers.StringHelper;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;


/**
 * An implementation of <a href="https://tools.ietf.org/html/rfc6901">RFC6901 - JavaScript Object Notation (JSON) Pointer</a>.
 */
public final class JSONPath{

	private static final char DECODED_SLASH = '/';
	private static final String SLASH = String.valueOf(DECODED_SLASH);

	private static final Pattern PATTERN_HEX_URL_ENCODE = Pattern.compile("%x([0-9a-fA-F]{2})");

	private static final IndexExtractor LIST_INDEX_EXTRACTOR = new ListIndexExtractor();
	private static final IndexExtractor ARRAY_INDEX_EXTRACTOR = new ArrayIndexExtractor();


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
	public static <T> T extract(final String path, final Object data, final T defaultValue) throws JSONPathException{
		if(path == null || !path.isEmpty()){
			final String[] pathComponents = parsePath(path);
			return extract(pathComponents, data, defaultValue);
		}

		//path is null or empty, return the parent object
		return (T)data;
	}

	private static String[] parsePath(final String path) throws JSONPathException{
		if(path == null || path.charAt(0) != DECODED_SLASH)
			throw JSONPathException.create("invalid path '{}'", path);

		final String[] components = StringHelper.split(path, DECODED_SLASH);
		for(int i = 0, length = components.length; i < length; i ++){
			final String component = components[i];

			if(!StringHelper.isBlank(component))
				components[i] = urlDecode(rfc6901Replace(component));
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

				//replace characters for the RFC6901 encoding of JSON paths, where "~0" is the encoding of a tilde, and "~1" of a slash
				final String replacement = switch(nextChar){
					//define replacement for ~1
					case '1' -> "/";
					//define replacement for ~0
					case '0' -> "~";
					default -> null;
				};

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
	 * @param input	The text to be URL decoded.
	 * @return	The URL-decoded text.
	 *
	 * @see <a href="https://datatracker.ietf.org/doc/html/rfc3986">RFC3986 - Uniform Resource Identifier (URI): Generic Syntax</a>
	 */
	private static String urlDecode(final CharSequence input){
		//convert (hexadecimal) %x?? to (decimal) %??:
		final String text = PATTERN_HEX_URL_ENCODE.matcher(input)
			.replaceAll(match -> Character.toString((char)Integer.parseInt(match.group(1), 16)));
		return URLDecoder.decode(text, StandardCharsets.UTF_8);
	}

	private static <T> T extract(final String[] path, final Object data, final T defaultValue) throws JSONPathException{
		Object result = data;
		//traverse the object until the path is fully consumed
		for(int i = 0, length = path.length; i < length; i ++){
			final String currentPath = path[i];

			final Integer idx = extractIndex(currentPath);

			validatePath(result, currentPath, idx, path);

			if(idx != null)
				result = extractPath(result, idx, defaultValue);
			else
				result = extractPath(result, currentPath, defaultValue);
		}
		return (T)result;
	}

	private static Integer extractIndex(final String currentPath){
		return (JavaHelper.isDecimalIntegerNumber(currentPath) && (currentPath.charAt(0) != '0' || currentPath.length() == 1)
			? Integer.valueOf(currentPath)
			: null
		);
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

	private static Object extractPath(final Object data, final int idx, final Object defaultValue){
		final IndexExtractor extractor = selectPathExtractor(data);
		return (extractor.isValidIndex(idx, data)
			? extractor.getValueAt(idx, data)
			: defaultValue
		);
	}

	private static IndexExtractor selectPathExtractor(final Object data){
		return (data instanceof List<?>
			? LIST_INDEX_EXTRACTOR
			: ARRAY_INDEX_EXTRACTOR
		);
	}

	private interface IndexExtractor{
		boolean isValidIndex(int idx, Object data);

		Object getValueAt(int idx, Object data);
	}

	private static class ListIndexExtractor implements IndexExtractor{
		@Override
		public final boolean isValidIndex(final int idx, final Object data){
			return (idx >= 0 && idx < ((Collection<?>)data).size());
		}

		@Override
		public final Object getValueAt(final int idx, final Object data){
			return ((List<?>)data).get(idx);
		}
	}

	private static class ArrayIndexExtractor implements IndexExtractor{
		@Override
		public final boolean isValidIndex(final int idx, final Object data){
			return (idx >= 0 && idx < Array.getLength(data));
		}

		@Override
		public final Object getValueAt(final int idx, final Object data){
			return Array.get(data, idx);
		}
	}

	private static Object extractPath(final Object data, final String currentPath, final Object defaultValue){
		Object nextData;
		if(data instanceof final Map<?, ?> m)
			nextData = (m.containsKey(currentPath)
				? m.get(currentPath)
				: defaultValue
			);
		else{
			final Class<?> cls = data.getClass();
			try{
				final Field currentField = cls.getDeclaredField(currentPath);
				FieldAccessor.makeAccessible(currentField);
				nextData = FieldMapper.getFieldValue(data, currentField);
			}
			catch(final NoSuchFieldException nsfe){
				nextData = defaultValue;
			}
		}
		return nextData;
	}

}
