/*
 * Copyright (c) 2020-2022 Mauro Trevisan
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
package io.github.mtrevisan.boxon.core.managers.configurations;

import io.github.mtrevisan.boxon.annotations.configurations.NullEnum;
import io.github.mtrevisan.boxon.configurations.ConfigurationEnum;
import io.github.mtrevisan.boxon.configurations.ConfigurationKey;
import io.github.mtrevisan.boxon.exceptions.CodecException;
import io.github.mtrevisan.boxon.exceptions.ConfigurationException;
import io.github.mtrevisan.boxon.helpers.StringHelper;
import io.github.mtrevisan.boxon.io.ParserDataType;
import io.github.mtrevisan.boxon.semanticversioning.Version;

import java.lang.reflect.Array;
import java.util.Map;
import java.util.regex.Pattern;


/**
 * A collection of convenience methods for working with configurations.
 */
public final class ConfigurationHelper{

	private static final Pattern PATTERN_PIPE = Pattern.compile("\\|");


	private ConfigurationHelper(){}


	/**
	 * Put the pair key-value into the given map.
	 *
	 * @param key	The key.
	 * @param value	The value.
	 * @param map	The map in which to load the key-value pair.
	 * @throws ConfigurationException	If a duplicate is found.
	 */
	public static void putIfNotEmpty(final ConfigurationKey key, final Object value,
			@SuppressWarnings("BoundedWildcard") final Map<String, Object> map) throws ConfigurationException{
		if(isValidValue(value) && map.put(key.toString(), value) != null)
			throw ConfigurationException.create("Duplicated short description: {}", key.toString());
	}

	private static boolean isValidValue(final Object value){
		return (value != null && (!(value instanceof CharSequence) || !StringHelper.isBlank((CharSequence)value)));
	}


	static Object convertValue(final String value, final Class<?> fieldType, final Class<? extends ConfigurationEnum> enumeration)
			throws CodecException{
		return (enumeration != NullEnum.class
			? extractEnumerationValue(fieldType, value, enumeration)
			: ParserDataType.getValue(fieldType, value));
	}


	static Object extractEnumerationValue(final Class<?> fieldType, final String value,
			final Class<? extends ConfigurationEnum> enumeration){
		return (fieldType.isArray()
			? extractEnumerationArrayValue(value, enumeration)
			: extractEnumerationSingleValue(value, enumeration));
	}

	@SuppressWarnings("unchecked")
	private static <T extends ConfigurationEnum> T[] extractEnumerationArrayValue(final CharSequence value, final Class<T> enumeration){
		final ConfigurationEnum[] enumConstants = enumeration.getEnumConstants();
		final String[] defaultValues = splitMultipleEnumerations(value);
		final T[] valEnum = (T[])Array.newInstance(enumeration, defaultValues.length);
		for(int i = 0; i < defaultValues.length; i ++)
			valEnum[i] = (T)ConfigurationEnum.extractEnum(enumConstants, defaultValues[i]);
		return valEnum;
	}

	@SuppressWarnings("unchecked")
	private static <T extends ConfigurationEnum> T extractEnumerationSingleValue(final String value, final Class<T> enumeration){
		final ConfigurationEnum[] enumConstants = enumeration.getEnumConstants();
		return (T)ConfigurationEnum.extractEnum(enumConstants, value);
	}

	private static String[] splitMultipleEnumerations(final CharSequence value){
		return PATTERN_PIPE.split(value);
	}


	/**
	 * Whether the field should be read, that is, the given protocol is between minimum protocol and maximum protocol.
	 *
	 * @param protocol	The protocol to check.
	 * @param minProtocol	The minimum protocol to check against.
	 * @param maxProtocol	The maximum protocol to check against.
	 * @return	Whether the field should be read.
	 */
	public static boolean shouldBeExtracted(final Version protocol, final String minProtocol, final String maxProtocol){
		if(protocol.isEmpty())
			return true;

		final Version min = Version.of(minProtocol);
		final Version max = Version.of(maxProtocol);
		final boolean validMinimum = (min.isEmpty() || protocol.isGreaterThanOrEqualTo(min));
		final boolean validMaximum = (max.isEmpty() || protocol.isLessThanOrEqualTo(max));
		return (validMinimum && validMaximum);
	}


	static void extractEnumeration(final Class<?> fieldType, final Class<? extends ConfigurationEnum> enumeration,
			final Map<String, Object> map) throws ConfigurationException{
		if(enumeration != NullEnum.class){
			final ConfigurationEnum[] enumConstants = enumeration.getEnumConstants();
			final String[] enumValues = new String[enumConstants.length];
			for(int j = 0; j < enumConstants.length; j ++)
				enumValues[j] = enumConstants[j].name();
			putIfNotEmpty(ConfigurationKey.ENUMERATION, enumValues, map);
			if(fieldType.isEnum())
				putIfNotEmpty(ConfigurationKey.MUTUALLY_EXCLUSIVE, true, map);
		}
	}


	static void extractMinMaxProtocol(final String minProtocol, final String maxProtocol, final Map<String, Object> fieldMap)
			throws ConfigurationException{
		putIfNotEmpty(ConfigurationKey.MIN_PROTOCOL, minProtocol, fieldMap);
		putIfNotEmpty(ConfigurationKey.MAX_PROTOCOL, maxProtocol, fieldMap);
	}

}
