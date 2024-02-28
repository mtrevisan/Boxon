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
 * Holds the constants used as a key in the {@link io.github.mtrevisan.boxon.core.Configurator Configurator}.
 */
public enum ConfigurationKey{
	CONFIGURATION_HEADER("header"),
	CONFIGURATION_FIELDS("fields"),
	CONFIGURATION_PROTOCOL_VERSION_BOUNDARIES("protocolVersionBoundaries"),
	CONFIGURATION_COMPOSITE_FIELDS("fields"),

	ALTERNATIVES("alternatives"),
	FIELD_TYPE("fieldType"),
	SHORT_DESCRIPTION("shortDescription"),
	LONG_DESCRIPTION("longDescription"),
	UNIT_OF_MEASURE("unitOfMeasure"),
	MIN_PROTOCOL("minProtocol"),
	MAX_PROTOCOL("maxProtocol"),
	MIN_VALUE("minValue"),
	MAX_VALUE("maxValue"),
	PATTERN("pattern"),
	ENUMERATION("enumeration"),
	MUTUALLY_EXCLUSIVE("mutuallyExclusive"),
	DEFAULT_VALUE("defaultValue"),
	CHARSET("charset");


	private final String name;


	ConfigurationKey(final String name){
		this.name = name;
	}


	@Override
	public String toString(){
		return name;
	}

}
