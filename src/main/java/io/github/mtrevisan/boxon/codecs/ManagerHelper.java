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
package io.github.mtrevisan.boxon.codecs;

import io.github.mtrevisan.boxon.annotations.configurations.NullEnum;
import io.github.mtrevisan.boxon.exceptions.ConfigurationException;
import io.github.mtrevisan.boxon.exceptions.EncodeException;
import io.github.mtrevisan.boxon.external.semanticversioning.Version;
import io.github.mtrevisan.boxon.internal.JavaHelper;
import io.github.mtrevisan.boxon.internal.ParserDataType;
import io.github.mtrevisan.boxon.internal.ReflectionHelper;
import io.github.mtrevisan.boxon.internal.StringHelper;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.Map;
import java.util.regex.Pattern;


final class ManagerHelper{

	private ManagerHelper(){}


	static void validateValue(final String dataKey, final Object dataValue, final Class<?> fieldType,
			final String pattern, final String minValue, final String maxValue) throws EncodeException{
		//check pattern
		validatePattern(dataKey, dataValue, pattern);
		//check minValue
		validateMinValue(dataKey, dataValue, fieldType, minValue);
		//check maxValue
		validateMaxValue(dataKey, dataValue, fieldType, maxValue);
	}

	private static void validatePattern(final String dataKey, final Object dataValue, final String pattern) throws EncodeException{
		if(!pattern.isEmpty()){
			final Pattern formatPattern = Pattern.compile(pattern);

			//value compatible with data type and pattern
			if(!String.class.isInstance(dataValue) || !formatPattern.matcher((CharSequence)dataValue).matches())
				throw EncodeException.create("Data value not compatible with `pattern` for data key {}; found {}, expected {}", dataKey, dataValue, pattern);
		}
	}

	private static void validateMinValue(final String dataKey, final Object dataValue, final Class<?> fieldType, final String minValue)
			throws EncodeException{
		if(!minValue.isEmpty()){
			final Object min = JavaHelper.getValue(fieldType, minValue);
			if(Number.class.isInstance(dataValue) && ((Number)dataValue).doubleValue() < ((Number)min).doubleValue())
				throw EncodeException.create("Data value incompatible with minimum value for data key {}; found {}, expected greater than or equals to {}", dataKey, dataValue, minValue.getClass().getSimpleName());
		}
	}

	private static void validateMaxValue(final String dataKey, final Object dataValue, final Class<?> fieldType, final String maxValue)
			throws EncodeException{
		if(!maxValue.isEmpty()){
			final Object max = JavaHelper.getValue(fieldType, maxValue);
			if(Number.class.isInstance(dataValue) && ((Number)dataValue).doubleValue() > ((Number)max).doubleValue())
				throw EncodeException.create("Data value incompatible with maximum value for data key {}; found {}, expected greater than or equals to {}", dataKey, dataValue, maxValue.getClass().getSimpleName());
		}
	}


	static void putIfNotEmpty(final String key, final Object value, @SuppressWarnings("BoundedWildcard") final Map<String, Object> map)
			throws ConfigurationException{
		if(value != null && (!String.class.isInstance(value) || !StringHelper.isBlank((CharSequence)value)))
			if(map.put(key, value) != null)
				throw ConfigurationException.create("Duplicated short description: {}", key);
	}

	static void putValueIfNotEmpty(final String key, final String value, final Class<?> fieldType,
			final Class<? extends Enum<?>> enumeration, @SuppressWarnings("BoundedWildcard") final Map<String, Object> map) throws ConfigurationException{
		if(!StringHelper.isBlank(value)){
			Object val = value;
			if(enumeration != NullEnum.class && fieldType.isArray())
				val = StringHelper.split(value, '|', -1);
			else if(Number.class.isAssignableFrom(ParserDataType.toObjectiveTypeOrSelf(fieldType)))
				val = JavaHelper.getValue(fieldType, value);
			if(map.put(key, val) != null)
				throw ConfigurationException.create("Duplicated short description: {}", key);
		}
	}

	static Object getDefaultValue(final Field field, final String value, final Class<? extends Enum<?>> enumeration){
		if(!StringHelper.isBlank(value)){
			if(enumeration != NullEnum.class)
				return extractEnumerationValue(field, value, enumeration);

			if(field.getType() != String.class)
				return JavaHelper.getValue(field.getType(), value);
		}
		return value;
	}

	static Object extractEnumerationValue(final Field field, final String value, final Class<? extends Enum<?>> enumeration){
		final Object valEnum;
		final Enum<?>[] enumConstants = enumeration.getEnumConstants();
		if(field.getType().isArray()){
			final String[] defaultValues = StringHelper.split(value, '|', -1);
			valEnum = Array.newInstance(enumeration, defaultValues.length);
			for(int i = 0; i < defaultValues.length; i ++)
				Array.set(valEnum, i, JavaHelper.extractEnum(enumConstants, defaultValues[i]));
		}
		else
			valEnum = enumeration
				.cast(JavaHelper.extractEnum(enumConstants, value));
		return valEnum;
	}

	static void setValue(final Field field, final Object configurationObject, final Object dataValue){
		ReflectionHelper.setFieldValue(field, configurationObject, dataValue);
	}

	static boolean shouldBeExtracted(final Version protocol, final String minProtocol, final String maxProtocol){
		if(protocol.isEmpty())
			return true;

		final Version min = Version.of(minProtocol);
		final Version max = Version.of(maxProtocol);
		final boolean validMinimum = (min.isEmpty() || protocol.isGreaterThanOrEqualTo(min));
		final boolean validMaximum = (max.isEmpty() || protocol.isLessThanOrEqualTo(max));
		return (validMinimum && validMaximum);
	}


	static void extractEnumeration(final Class<?> fieldType, final Class<? extends Enum> enumeration, final Map<String, Object> map)
			throws ConfigurationException{
		if(enumeration != NullEnum.class){
			final Enum<?>[] enumConstants = enumeration.getEnumConstants();
			final String[] enumValues = new String[enumConstants.length];
			for(int j = 0; j < enumConstants.length; j ++)
				enumValues[j] = enumConstants[j].name();
			putIfNotEmpty(LoaderConfiguration.KEY_ENUMERATION, enumValues, map);
			if(fieldType.isEnum())
				putIfNotEmpty(LoaderConfiguration.KEY_MUTUALLY_EXCLUSIVE, true, map);
		}
	}

}
