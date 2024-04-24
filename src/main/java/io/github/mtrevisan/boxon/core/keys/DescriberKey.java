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
package io.github.mtrevisan.boxon.core.keys;

import io.github.mtrevisan.boxon.core.Describer;


/**
 * Holds the constants used as a key in the {@link Describer}.
 */
public enum DescriberKey{
	/** Represents the template constant used as a key in the {@link Describer}. */
	TEMPLATE("template"),
	/** Represents the configuration constant used as a key in the {@link Describer}. */
	CONFIGURATION("configuration"),

	/** Represents the context constant used as a key in the {@link Describer}. */
	CONTEXT("context"),

	/** Represents the header constant used as a key in the {@link Describer}. */
	HEADER("header"),

	/** Represents the fields constant used as a key in the {@link Describer}. */
	FIELDS("fields"),
	/** Represents the evaluated fields constant used as a key in the {@link Describer}. */
	EVALUATED_FIELDS("evaluatedFields"),
	/** Represents the post-processed constant used as a key in the {@link Describer}. */
	POST_PROCESSED_FIELDS("postProcessedFields"),
	/** Represents the name constant used as a key in the {@link Describer}. */
	FIELD_NAME("name"),
	/** Represents the field type constant used as a key in the {@link Describer}. */
	FIELD_TYPE("fieldType"),
	/** Represents the annotation type constant used as a key in the {@link Describer}. */
	ANNOTATION_TYPE("annotationType"),
	/** Represents the collection annotation type constant used as a key in the {@link Describer}. */
	COLLECTION_TYPE("collectionType"),
	/** Represents the array collection size constant used as a key in the {@link Describer}. */
	COLLECTION_ARRAY_SIZE("collectionArraySize"),

	/** Represents the bind subtypes constant used as a key in the {@link Describer}. */
	BIND_SUBTYPES("subtypes");


	private final String name;


	DescriberKey(final String name){
		this.name = name;
	}


	@Override
	public String toString(){
		return name;
	}

}
