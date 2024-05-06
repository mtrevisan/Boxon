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
	/** Represents the header constant used as a key in the {@link io.github.mtrevisan.boxon.core.Configurator Configurator}. */
	HEADER("header"),
	/** Represents the fields constant used as a key in the {@link io.github.mtrevisan.boxon.core.Configurator Configurator}. */
	FIELDS("fields"),
	/** Represents the protocol version boundaries constant used as a key in the {@link io.github.mtrevisan.boxon.core.Configurator Configurator}. */
	PROTOCOL_VERSION_BOUNDARIES("protocolVersionBoundaries"),
	/** Represents the composite fields constant used as a key in the {@link io.github.mtrevisan.boxon.core.Configurator Configurator}. */
	COMPOSITE_FIELDS("fields"),

	/** Represents the alternatives constant used as a key in the {@link io.github.mtrevisan.boxon.core.Configurator Configurator}. */
	ALTERNATIVES("alternatives"),
	/** Represents the field type constant used as a key in the {@link io.github.mtrevisan.boxon.core.Configurator Configurator}. */
	FIELD_TYPE("fieldType"),
	/**
	 * Represents the short description constant used as a key in the {@link io.github.mtrevisan.boxon.core.Configurator Configurator}.
	 * NOTE: MUST match the name of the method in all the annotations that defines a short description!
	 */
	SHORT_DESCRIPTION("shortDescription"),
	/**
	 * Represents the long description constant used as a key in the {@link io.github.mtrevisan.boxon.core.Configurator Configurator}.
	 * NOTE: MUST match the name of the method in all the annotations that defines a long description!
	 */
	LONG_DESCRIPTION("longDescription"),
	/**
	 * Represents the unit of measure constant used as a key in the {@link io.github.mtrevisan.boxon.core.Configurator Configurator}.
	 * NOTE: MUST match the name of the method in all the annotations that defines a unit of measure!
	 */
	UNIT_OF_MEASURE("unitOfMeasure"),
	/**
	 * Represents the minimum protocol constant used as a key in the {@link io.github.mtrevisan.boxon.core.Configurator Configurator}.
	 * NOTE: MUST match the name of the method in all the annotations that defines a minimum protocol!
	 */
	MIN_PROTOCOL("minProtocol"),
	/**
	 * Represents the maximum protocol constant used as a key in the {@link io.github.mtrevisan.boxon.core.Configurator Configurator}.
	 * NOTE: MUST match the name of the method in all the annotations that defines a maximum protocol!
	 */
	MAX_PROTOCOL("maxProtocol"),
	/**
	 * Represents the minimum value constant used as a key in the {@link io.github.mtrevisan.boxon.core.Configurator Configurator}.
	 * NOTE: MUST match the name of the method in all the annotations that defines a minimum value!
	 */
	MIN_VALUE("minValue"),
	/**
	 * Represents the maximum value constant used as a key in the {@link io.github.mtrevisan.boxon.core.Configurator Configurator}.
	 * NOTE: MUST match the name of the method in all the annotations that defines a maximum value!
	 */
	MAX_VALUE("maxValue"),
	/**
	 * Represents the default value constant used as a key in the {@link io.github.mtrevisan.boxon.core.Configurator Configurator}.
	 * NOTE: MUST match the name of the method in all the annotations that defines a default value!
	 */
	DEFAULT_VALUE("defaultValue"),
	/**
	 * Represents the pattern constant used as a key in the {@link io.github.mtrevisan.boxon.core.Configurator Configurator}.
	 * NOTE: MUST match the name of the method in all the annotations that defines a pattern!
	 */
	PATTERN("pattern"),
	/**
	 * Represents the enumeration constant used as a key in the {@link io.github.mtrevisan.boxon.core.Configurator Configurator}.
	 * NOTE: MUST match the name of the method in all the annotations that defines an enumeration!
	 */
	ENUMERATION("enumeration"),
	/** Represents the mutually exclusive constant used as a key in the {@link io.github.mtrevisan.boxon.core.Configurator Configurator}. */
	MUTUALLY_EXCLUSIVE("mutuallyExclusive"),
	/**
	 * Represents the charset constant used as a key in the {@link io.github.mtrevisan.boxon.core.Configurator Configurator}.
	 * NOTE: MUST match the name of the method in all the annotations that defines a charset!
	 */
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
