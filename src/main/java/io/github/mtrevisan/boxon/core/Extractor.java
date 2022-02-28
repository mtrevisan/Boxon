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
import io.github.mtrevisan.boxon.exceptions.JSONPathException;

import java.util.Objects;


/**
 * A class that allows extracting values from a POJO.
 */
@SuppressWarnings({"WeakerAccess", "unused"})
public final class Extractor{

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
	 * @param <T>	The class of the resulting value.
	 * @param path	The path used to extract the corresponding value (following
	 * 	<a href="https://tools.ietf.org/html/rfc6901">RFC6901 - JavaScript Object Notation (JSON) Pointer</a> notation).
	 * @return	The value.
	 * @throws JSONPathException	If the path has an error.
	 */
	public <T> T get(final String path) throws JSONPathException{
		return JSONPath.extract(path, data);
	}

}
