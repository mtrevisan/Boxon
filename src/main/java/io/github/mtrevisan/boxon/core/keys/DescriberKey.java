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
	TEMPLATE("template"),
	CONFIGURATION("configuration"),

	CONTEXT("context"),

	HEADER("header"),
	HEADER_START("start"),
	HEADER_END("end"),
	HEADER_CHARSET("charset"),

	FIELDS("fields"),
	EVALUATED_FIELDS("evaluatedFields"),
	POST_PROCESSED_FIELDS("postProcessedFields"),
	FIELD_NAME("name"),
	FIELD_TYPE("fieldType"),
	ANNOTATION_TYPE("annotationType"),

	BIND_CONDITION("condition"),
	BIND_TYPE("type"),
	BIND_SELECT_DEFAULT("selectDefault"),
	BIND_CONVERTER("converter"),
	BIND_SELECT_CONVERTER_FROM("selectConverterFrom"),
	BIND_SIZE("size"),
	BIND_BIT_ORDER("bitOrder"),
	BIND_BYTE_ORDER("byteOrder"),
	BIND_CHARSET("charset"),
	BIND_TERMINATOR("terminator"),
	BIND_CONSUME_TERMINATOR("consumeTerminator"),
	BIND_PREFIX("prefix"),
	BIND_PREFIX_LENGTH("prefixLength"),
	BIND_SKIP_START("skipStart"),
	BIND_SKIP_END("skipEnd"),
	BIND_ALGORITHM("algorithm"),
	BIND_START_VALUE("startValue"),
	BIND_VALUE("value"),
	BIND_VALUE_DECODE("valueDecode"),
	BIND_VALUE_ENCODE("valueEncode"),
	BIND_VALIDATOR("validator"),

	BIND_MIN_PROTOCOL("minProtocol"),
	BIND_MAX_PROTOCOL("maxProtocol");


	private final String name;


	DescriberKey(final String name){
		this.name = name;
	}


	@Override
	public String toString(){
		return name;
	}

}
