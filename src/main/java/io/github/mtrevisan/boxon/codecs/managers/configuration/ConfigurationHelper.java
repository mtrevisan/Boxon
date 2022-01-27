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
package io.github.mtrevisan.boxon.codecs.managers.configuration;

import io.github.mtrevisan.boxon.annotations.configurations.NullEnum;
import io.github.mtrevisan.boxon.exceptions.CodecException;
import io.github.mtrevisan.boxon.exceptions.ConfigurationException;
import io.github.mtrevisan.boxon.exceptions.EncodeException;
import io.github.mtrevisan.boxon.external.io.ParserDataType;
import io.github.mtrevisan.boxon.external.configurations.ConfigurationEnum;
import io.github.mtrevisan.boxon.external.configurations.ConfigurationKey;
import io.github.mtrevisan.boxon.external.semanticversioning.Version;
import io.github.mtrevisan.boxon.internal.StringHelper;

import java.lang.reflect.Array;
import java.util.Map;
import java.util.regex.Pattern;


public final class ConfigurationHelper{

	private static final Pattern PATTERN_PIPE = Pattern.compile("\\|");


	private ConfigurationHelper(){}


	static void validatePattern(final String dataKey, final Object dataValue, final String pattern) throws EncodeException{
		if(!pattern.isEmpty()){
			final Pattern formatPattern = Pattern.compile(pattern);

			//value compatible with data type and pattern
			if(!String.class.isInstance(dataValue) || !formatPattern.matcher((CharSequence)dataValue).matches())
				throw EncodeException.create("Data value not compatible with `pattern` for data key {}; found {}, expected {}",
					dataKey, dataValue, pattern);
		}
	}

	@SuppressWarnings("ConstantConditions")
	static void validateMinValue(final String dataKey, final Object dataValue, final Class<?> fieldType, final String minValue)
			throws EncodeException, CodecException{
		if(!minValue.isEmpty()){
			final Object min = ParserDataType.getValue(fieldType, minValue);
			if(Number.class.isInstance(dataValue) && ((Number)dataValue).doubleValue() < ((Number)min).doubleValue())
				throw EncodeException.create("Data value incompatible with minimum value for data key {}; found {}, expected greater than or equals to {}",
					dataKey, dataValue, minValue.getClass().getSimpleName());
		}
	}

	@SuppressWarnings("ConstantConditions")
	static void validateMaxValue(final String dataKey, final Object dataValue, final Class<?> fieldType, final String maxValue)
			throws EncodeException, CodecException{
		if(!maxValue.isEmpty()){
			final Object max = ParserDataType.getValue(fieldType, maxValue);
			if(Number.class.isInstance(dataValue) && ((Number)dataValue).doubleValue() > ((Number)max).doubleValue())
				throw EncodeException.create("Data value incompatible with maximum value for data key {}; found {}, expected greater than or equals to {}",
					dataKey, dataValue, maxValue.getClass().getSimpleName());
		}
	}


	public static void putIfNotEmpty(final ConfigurationKey key, final Object value,
			@SuppressWarnings("BoundedWildcard") final Map<String, Object> map) throws ConfigurationException{
		if(isValidValue(value) && map.put(key.toString(), value) != null)
			throw ConfigurationException.create("Duplicated short description: {}", key.toString());
	}

	private static boolean isValidValue(final Object value){
		return (value != null && (!CharSequence.class.isInstance(value) || !StringHelper.isBlank((CharSequence)value)));
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
