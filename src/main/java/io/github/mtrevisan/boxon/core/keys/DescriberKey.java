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


/**
 * Holds the constants used as a key in the {@link io.github.mtrevisan.boxon.core.Descriptor Descriptor}.
 */
public enum DescriberKey{
	/** Represents the template constant used as a key in the {@link io.github.mtrevisan.boxon.core.Descriptor Descriptor}. */
	TEMPLATE("template"),
	/** Represents the configuration constant used as a key in the {@link io.github.mtrevisan.boxon.core.Descriptor Descriptor}. */
	CONFIGURATION("configuration"),

	/** Represents the context constant used as a key in the {@link io.github.mtrevisan.boxon.core.Descriptor Descriptor}. */
	CONTEXT("context"),

	/** Represents the header constant used as a key in the {@link io.github.mtrevisan.boxon.core.Descriptor Descriptor}. */
	HEADER("header"),

	/** Represents the fields constant used as a key in the {@link io.github.mtrevisan.boxon.core.Descriptor Descriptor}. */
	FIELDS("fields"),
	/** Represents the evaluated fields constant used as a key in the {@link io.github.mtrevisan.boxon.core.Descriptor Descriptor}. */
	EVALUATED_FIELDS("evaluatedFields"),
	/** Represents the post-processed constant used as a key in the {@link io.github.mtrevisan.boxon.core.Descriptor Descriptor}. */
	POST_PROCESSED_FIELDS("postProcessedFields"),
	/** Represents the name constant used as a key in the {@link io.github.mtrevisan.boxon.core.Descriptor Descriptor}. */
	FIELD_NAME("name"),
	/** Represents the field type constant used as a key in the {@link io.github.mtrevisan.boxon.core.Descriptor Descriptor}. */
	FIELD_TYPE("fieldType"),
	/** Represents the annotation type constant used as a key in the {@link io.github.mtrevisan.boxon.core.Descriptor Descriptor}. */
	ANNOTATION_TYPE("annotationType"),

	/** Represents the bind condition constant used as a key in the {@link io.github.mtrevisan.boxon.core.Descriptor Descriptor}. */
	BIND_CONDITION("condition"),
	/** Represents the bind type constant used as a key in the {@link io.github.mtrevisan.boxon.core.Descriptor Descriptor}. */
	BIND_TYPE("type"),
	/** Represents the bind subtypes constant used as a key in the {@link io.github.mtrevisan.boxon.core.Descriptor Descriptor}. */
	BIND_SUBTYPES("subtypes"),
	/** Represents the bind converter constant used as a key in the {@link io.github.mtrevisan.boxon.core.Descriptor Descriptor}. */
	BIND_CONVERTER("converter"),
	/** Represents the bind select converter from constant used as a key in the {@link io.github.mtrevisan.boxon.core.Descriptor Descriptor}. */
	BIND_SELECT_CONVERTER_FROM("selectConverterFrom"),
	/** Represents the bind charset constant used as a key in the {@link io.github.mtrevisan.boxon.core.Descriptor Descriptor}. */
	BIND_CHARSET("charset"),
	/** Represents the bind terminator constant used as a key in the {@link io.github.mtrevisan.boxon.core.Descriptor Descriptor}. */
	BIND_TERMINATOR("terminator"),
	/** Represents the bind prefix constant used as a key in the {@link io.github.mtrevisan.boxon.core.Descriptor Descriptor}. */
	BIND_PREFIX("prefix"),
	/** Represents the bind prefix length constant used as a key in the {@link io.github.mtrevisan.boxon.core.Descriptor Descriptor}. */
	BIND_PREFIX_LENGTH("prefixLength"),
	/** Represents the bind validator constant used as a key in the {@link io.github.mtrevisan.boxon.core.Descriptor Descriptor}. */
	BIND_VALIDATOR("validator");


	private final String name;


	DescriberKey(final String name){
		this.name = name;
	}


	@Override
	public String toString(){
		return name;
	}

}
