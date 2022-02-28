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


/**
 * An implementation of <a href="https://tools.ietf.org/html/rfc6901">RFC6901 - JavaScript Object Notation (JSON) Pointer</a>.
 */
public final class JSONPath{

	private static final char SLASH = '/';
	private static final char TILDE = '~';
	/** Encoding of a tilde. */
	private static final String TILDE_ZERO = "~0";
	/** Encoding of a slash. */
	private static final String TILDE_ONE = "~1";


	private JSONPath(){}


	/**
	 * Extract the value from the given object pointed by the given path.
	 *
	 * @param path	The path used to reference the value in the data object.
	 * @param data	The data from which to extract the valule.
	 * @param <T>	The value class type.
	 * @return	The value.
	 * @throws JSONPathException	If the path is not well formatted.
	 */
	@SuppressWarnings("unchecked")
	public static <T> T extract(final String path, final Object data) throws JSONPathException{
		return (path == null || !path.isEmpty()? extract(parse(path), data): (T)data);
	}

	private static String[] parse(final String path) throws JSONPathException{
		if(path == null || path.charAt(0) != SLASH)
			throw JSONPathException.create("invalid path ['{}']", path);

		final String[] components = StringHelper.split(path, SLASH);
		for(int i = 0; i < components.length; i ++)
			if(!StringHelper.isBlank(components[i])){
				//NOTE: the order here is important!
				components[i] = replace(components[i], TILDE_ONE, SLASH);
				components[i] = replace(components[i], TILDE_ZERO, TILDE);
				components[i] = URLDecoder.decode(components[i], StandardCharsets.UTF_8);
			}
		return components;
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

	@SuppressWarnings({"unchecked", "ThrowInsideCatchBlockWhichIgnoresCaughtException"})
	private static <T> T extract(final String[] path, Object data) throws JSONPathException{
		try{
			 for(int i = 0; i < path.length; i ++){
				final String currentPath = path[i];
				final Integer idx = (ParserDataType.isDecimalNumber(currentPath)? Integer.valueOf(currentPath): null);
				final Class<?> dataClass = data.getClass();
				if(idx != null ^ dataClass.isArray())
					throw JSONPathException.create("No array field '{}' found on path '{}'", currentPath, toJSONPath(path));

				if(idx == null){
					final Field currentField = dataClass.getDeclaredField(currentPath);
					currentField.setAccessible(true);
					data = ReflectionHelper.getValue(data, currentField);
				}
				else
					data = Array.get(data, idx);
			}

			return (T)data;
		}
		catch(final NoSuchFieldException nsfe){
			throw JSONPathException.create("No field '{}' found on path '{}'", nsfe.getMessage(), toJSONPath(path));
		}
	}

	private static String toJSONPath(final String[] path){
		return "/" + String.join("/", path);
	}

}
