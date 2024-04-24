/*
 * Copyright (c) 2020-2024 Mauro Trevisan
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
package io.github.mtrevisan.boxon.core.helpers.configurations;

import io.github.mtrevisan.boxon.annotations.configurations.ConfigurationEnum;
import io.github.mtrevisan.boxon.annotations.configurations.NullEnum;
import io.github.mtrevisan.boxon.core.keys.ConfigurationKey;
import io.github.mtrevisan.boxon.exceptions.CodecException;
import io.github.mtrevisan.boxon.exceptions.ConfigurationException;
import io.github.mtrevisan.boxon.helpers.StringHelper;
import io.github.mtrevisan.boxon.io.ParserDataType;
import io.github.mtrevisan.boxon.semanticversioning.Version;
import io.github.mtrevisan.boxon.semanticversioning.VersionBuilder;

import java.lang.reflect.Array;
import java.util.Map;


/**
 * A collection of convenience methods for working with configurations.
 */
public final class ConfigurationHelper{

	private static final char PIPE = '|';


	private ConfigurationHelper(){}


	/**
	 * Put the pair key-value into the given map.
	 *
	 * @param key	The key.
	 * @param value	The value.
	 * @param map	The map in which to load the key-value pair.
	 * @throws ConfigurationException	If a duplicate is found.
	 */
	public static void putIfNotEmpty(final ConfigurationKey key, final Object value, final Map<String, Object> map)
			throws ConfigurationException{
		if(isValidValue(value) && map.put(key.toString(), value) != null)
			throw ConfigurationException.create("Duplicated short description: {}", key.toString());
	}

	private static boolean isValidValue(final Object value){
		return (value != null && (!(value instanceof final String v) || !StringHelper.isBlank(v)));
	}


	static Object convertValue(final String value, final Class<?> fieldType, final Class<? extends ConfigurationEnum> enumeration)
			throws CodecException{
		return (hasEnumeration(enumeration)
			? extractEnumerationValue(fieldType, value, enumeration)
			: ParserDataType.getValueOrSelf(fieldType, value)
		);
	}

	/**
	 * Whether the given class is a true enumeration.
	 *
	 * @param enumeration	The class to check.
	 * @return	Whether the given class is a true enumeration.
	 */
	static boolean hasEnumeration(final Class<? extends ConfigurationEnum> enumeration){
		return (enumeration != null && enumeration != NullEnum.class);
	}

	static Object extractEnumerationValue(final Class<?> fieldType, Object value, final Class<? extends ConfigurationEnum> enumerationClass){
		if(value instanceof final String v)
			value = (fieldType.isArray()
				? extractEnumerationArrayValue(v, enumerationClass)
				: extractEnumerationSingleValue(v, enumerationClass)
			);
		return value;
	}

	private static Object extractEnumerationArrayValue(final String value, final Class<? extends ConfigurationEnum> enumerationClass){
		final ConfigurationEnum[] enumConstants = enumerationClass.getEnumConstants();
		final String[] defaultValues = StringHelper.split(value, PIPE);
		final int length = defaultValues.length;
		final Object valEnum = Array.newInstance(enumerationClass, length);
		for(int i = 0; i < length; i ++)
			Array.set(valEnum, i, ConfigurationEnum.extractEnum(enumConstants, defaultValues[i]));
		return valEnum;
	}

	private static Object extractEnumerationSingleValue(final String value, final Class<? extends ConfigurationEnum> enumerationClass){
		final ConfigurationEnum[] enumConstants = enumerationClass.getEnumConstants();
		return ConfigurationEnum.extractEnum(enumConstants, value);
	}


	/**
	 * Whether the field should be read, that is, the given protocol is between minimum protocol and maximum protocol.
	 *
	 * @param protocol	The protocol to check.
	 * @param minProtocol	The minimum protocol to check against (should follow <a href="https://semver.org/">Semantic Versioning</a>).
	 * @param maxProtocol	The maximum protocol to check against (should follow <a href="https://semver.org/">Semantic Versioning</a>).
	 * @return	Whether the field should be read.
	 */
	public static boolean shouldBeExtracted(final Version protocol, final String minProtocol, final String maxProtocol){
		if(protocol.isEmpty())
			return true;

		final Version min = VersionBuilder.of(minProtocol);
		final Version max = VersionBuilder.of(maxProtocol);
		final boolean validMinimum = (min.isEmpty() || protocol.isGreaterThanOrEqualTo(min));
		final boolean validMaximum = (max.isEmpty() || protocol.isLessThanOrEqualTo(max));
		return (validMinimum && validMaximum);
	}


	static void extractEnumeration(final Class<?> fieldType, final Class<? extends ConfigurationEnum> enumeration,
			final Map<String, Object> map) throws ConfigurationException{
		if(enumeration != NullEnum.class){
			final ConfigurationEnum[] enumConstants = enumeration.getEnumConstants();
			final int length = enumConstants.length;
			final String[] enumValues = new String[length];
			for(int i = 0; i < length; i ++)
				enumValues[i] = enumConstants[i].name();
			putIfNotEmpty(ConfigurationKey.ENUMERATION, enumValues, map);
			if(!fieldType.isArray())
				putIfNotEmpty(ConfigurationKey.MUTUALLY_EXCLUSIVE, true, map);
		}
	}


	static void extractMinMaxProtocol(final String minProtocol, final String maxProtocol, final Map<String, Object> fieldMap)
			throws ConfigurationException{
		putIfNotEmpty(ConfigurationKey.MIN_PROTOCOL, minProtocol, fieldMap);
		putIfNotEmpty(ConfigurationKey.MAX_PROTOCOL, maxProtocol, fieldMap);
	}

}
