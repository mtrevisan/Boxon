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
package io.github.mtrevisan.boxon.core;

import io.github.mtrevisan.boxon.core.managers.extractors.JSONPath;
import io.github.mtrevisan.boxon.core.managers.extractors.JSONTypeInterface;
import io.github.mtrevisan.boxon.helpers.StringHelper;

import java.util.Objects;


/**
 * A class that allows extracting values from a POJO.
 */
@SuppressWarnings({"WeakerAccess", "unused"})
public final class Extractor{

	private static final char DOT = '.';


	private final Object data;


	/**
	 * Create an extractor.
	 *
	 * @param data	The data from which to extract values.
	 * @return	An extractor.
	 */
	public static Extractor create(final Object data){
		return new Extractor(data);
	}


	private Extractor(final Object data){
		Objects.requireNonNull(data, "Data cannot be null");

		this.data = data;
	}


	/**
	 * Extract the value corresponding to the given path.
	 *
	 * @param path	The path used to extract the corresponding value.
	 * @param type	The return type class.
	 * @return	The value.
	 * @param <T>	The class of the resulting value.
	 */
	public <T> T get(final String path, final Class<T> type){
		final JSONTypeInterface json = (JSONTypeInterface)data;
		return JSONPath.extract(cleanPath(path), json);
	}

	private static String cleanPath(final String path){
		String[] fields = StringHelper.split(path, DOT);
		if(fields.length > 0 && fields[0].isEmpty()){
			final String[] newFields = new String[fields.length - 1];
			System.arraycopy(fields, 1, newFields, 0, fields.length);
			fields = newFields;
		}
		return String.join(".", fields);
	}

}
