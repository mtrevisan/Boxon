/**
 * Copyright (c) 2020-2021 Mauro Trevisan
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

import io.github.mtrevisan.boxon.external.ConfigurationEnum;
import io.github.mtrevisan.boxon.annotations.configurations.NullEnum;
import io.github.mtrevisan.boxon.codecs.LoaderConfiguration;
import io.github.mtrevisan.boxon.exceptions.CodecException;
import io.github.mtrevisan.boxon.exceptions.ConfigurationException;
import io.github.mtrevisan.boxon.exceptions.EncodeException;
import io.github.mtrevisan.boxon.external.semanticversioning.Version;
import io.github.mtrevisan.boxon.internal.JavaHelper;
import io.github.mtrevisan.boxon.internal.ParserDataType;
import io.github.mtrevisan.boxon.internal.StringHelper;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.Map;
import java.util.regex.Pattern;


public final class ConfigurationHelper{

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

	static void validateMinValue(final String dataKey, final Object dataValue, final Class<?> fieldType, final String minValue)
			throws EncodeException, CodecException{
		if(!minValue.isEmpty()){
			final Object min = JavaHelper.getValue(fieldType, minValue);
			if(Number.class.isInstance(dataValue) && ((Number)dataValue).doubleValue() < ((Number)min).doubleValue())
				throw EncodeException.create("Data value incompatible with minimum value for data key {}; found {}, expected greater than or equals to {}", dataKey, dataValue, minValue.getClass().getSimpleName());
		}
	}

	static void validateMaxValue(final String dataKey, final Object dataValue, final Class<?> fieldType, final String maxValue)
			throws EncodeException, CodecException{
		if(!maxValue.isEmpty()){
			final Object max = JavaHelper.getValue(fieldType, maxValue);
			if(Number.class.isInstance(dataValue) && ((Number)dataValue).doubleValue() > ((Number)max).doubleValue())
				throw EncodeException.create("Data value incompatible with maximum value for data key {}; found {}, expected greater than or equals to {}", dataKey, dataValue, maxValue.getClass().getSimpleName());
		}
	}


	public static void putIfNotEmpty(final String key, final Object value, @SuppressWarnings("BoundedWildcard") final Map<String, Object> map)
			throws ConfigurationException{
		if(value != null && (!String.class.isInstance(value) || !StringHelper.isBlank((CharSequence)value)))
			if(map.put(key, value) != null)
				throw ConfigurationException.create("Duplicated short description: {}", key);
	}

	static void putValueIfNotEmpty(final String key, final String value, final Class<?> fieldType,
			final Class<? extends ConfigurationEnum> enumeration, @SuppressWarnings("BoundedWildcard") final Map<String, Object> map)
			throws ConfigurationException, CodecException{
		if(!StringHelper.isBlank(value)){
			Object val = value;
			if(enumeration != NullEnum.class && fieldType.isArray())
				val = splitMultipleEnumerations(value);
			else if(Number.class.isAssignableFrom(ParserDataType.toObjectiveTypeOrSelf(fieldType)))
				val = JavaHelper.getValue(fieldType, value);
			if(map.put(key, val) != null)
				throw ConfigurationException.create("Duplicated short description: {}", key);
		}
	}

	static Object getDefaultValue(final Field field, final String value, final Class<? extends ConfigurationEnum> enumeration)
			throws CodecException{
		if(!StringHelper.isBlank(value)){
			if(enumeration != NullEnum.class)
				return extractEnumerationValue(field, value, enumeration);

			if(field.getType() != String.class)
				return JavaHelper.getValue(field.getType(), value);
		}
		return value;
	}

	static Object extractEnumerationValue(final Field field, final String value, final Class<? extends ConfigurationEnum> enumeration){
		final Object valEnum;
		final ConfigurationEnum[] enumConstants = enumeration.getEnumConstants();
		if(field.getType().isArray()){
			final String[] defaultValues = splitMultipleEnumerations(value);
			valEnum = Array.newInstance(enumeration, defaultValues.length);
			for(int i = 0; i < defaultValues.length; i ++)
				Array.set(valEnum, i, ConfigurationEnum.extractEnum(enumConstants, defaultValues[i]));
		}
		else
			valEnum = enumeration
				.cast(ConfigurationEnum.extractEnum(enumConstants, value));
		return valEnum;
	}

	private static String[] splitMultipleEnumerations(final String value){
		return StringHelper.split(value, '|', -1);
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
			putIfNotEmpty(LoaderConfiguration.KEY_ENUMERATION, enumValues, map);
			if(fieldType.isEnum())
				putIfNotEmpty(LoaderConfiguration.KEY_MUTUALLY_EXCLUSIVE, true, map);
		}
	}

	static void extractMinMaxProtocol(final String minProtocol, final String maxProtocol, final Map<String, Object> fieldMap)
			throws ConfigurationException{
		putIfNotEmpty(LoaderConfiguration.KEY_MIN_PROTOCOL, minProtocol, fieldMap);
		putIfNotEmpty(LoaderConfiguration.KEY_MAX_PROTOCOL, maxProtocol, fieldMap);
	}

}
