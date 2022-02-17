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
package io.github.mtrevisan.boxon.internal.helpers;

import io.github.mtrevisan.boxon.internal.managers.configuration.ConfigFieldData;
import io.github.mtrevisan.boxon.exceptions.AnnotationException;
import io.github.mtrevisan.boxon.exceptions.CodecException;
import io.github.mtrevisan.boxon.configurations.ConfigurationEnum;
import io.github.mtrevisan.boxon.io.ParserDataType;
import io.github.mtrevisan.boxon.semanticversioning.Version;
import io.github.mtrevisan.boxon.internal.JavaHelper;
import io.github.mtrevisan.boxon.internal.StringHelper;

import java.util.Arrays;
import java.util.regex.Pattern;


/**
 * A collection of convenience methods for working with validations.
 */
public final class ValidationHelper{

	private static final Pattern PATTERN_PIPE = Pattern.compile("\\|");


	private ValidationHelper(){}


	public static void validateProtocol(final ConfigFieldData field, final Version minProtocolVersion, final Version maxProtocolVersion) throws AnnotationException{
		if(StringHelper.isBlank(field.getMinProtocol()) && StringHelper.isBlank(field.getMaxProtocol()))
			return;

		final Version minimum = validateProtocol(field.getMinProtocol(), field.getAnnotationName(),
			"Invalid minimum protocol version in {}; found {}");
		final Version maximum = validateProtocol(field.getMaxProtocol(), field.getAnnotationName(),
			"Invalid maximum protocol version in {}; found {}");

		//`maxProtocol` must be after or equal to `minProtocol`
		if(minimum != null && maximum != null && maximum.isLessThan(minimum))
			throw AnnotationException.create("Minimum protocol version is greater than maximum protocol version in {}; found {}",
				field.getAnnotationName(), field.getMaxProtocol());

		//`minProtocol` must be after or equal to `minProtocolVersion`
		if(minimum != null && !minProtocolVersion.isEmpty() && minimum.isLessThan(minProtocolVersion))
			throw AnnotationException.create("Minimum protocol version is less than whole message minimum protocol version in {}; found {}",
				field.getAnnotationName(), maxProtocolVersion);
		//`maxProtocol` must be before or equal to `maxProtocolVersion`
		if(maximum != null && !maxProtocolVersion.isEmpty() && maxProtocolVersion.isLessThan(maximum))
			throw AnnotationException.create("Maximum protocol version is greater than whole message maximum protocol version in {}; found {}",
				field.getAnnotationName(), maxProtocolVersion);
	}

	private static Version validateProtocol(final String protocolVersion, final String bindingName, final String errorMessage)
			throws AnnotationException{
		Version protocol = null;
		if(!StringHelper.isBlank(protocolVersion)){
			try{
				protocol = Version.of(protocolVersion);
			}
			catch(final IllegalArgumentException iae){
				throw AnnotationException.create(iae, errorMessage, bindingName, protocolVersion);
			}
		}
		return protocol;
	}

	private static Object validateMinValue(final ConfigFieldData field, final Object def) throws AnnotationException, CodecException{
		Object min = null;
		final String minValue = field.getMinValue();
		if(!StringHelper.isBlank(minValue)){
			min = ParserDataType.getValue(field.getFieldType(), minValue);
			//minValue compatible with variable type
			if(min == null)
				throw AnnotationException.create("Incompatible minimum value in {}; found {}, expected {}",
					field.getAnnotationName(), minValue.getClass().getSimpleName(), field.getFieldType().toString());

			if(def != null && ((Number)def).doubleValue() < ((Number)min).doubleValue())
				//defaultValue compatible with minValue
				throw AnnotationException.create("Default value incompatible with minimum value in {}; found {}, expected greater than or equals to {}",
					field.getAnnotationName(), field.getDefaultValue(), minValue.getClass().getSimpleName());
		}
		return min;
	}

	private static Object validateMaxValue(final ConfigFieldData field, final Object def) throws AnnotationException, CodecException{
		Object max = null;
		final String maxValue = field.getMaxValue();
		if(!StringHelper.isBlank(maxValue)){
			max = ParserDataType.getValue(field.getFieldType(), maxValue);
			//maxValue compatible with variable type
			if(max == null)
				throw AnnotationException.create("Incompatible maximum value in {}; found {}, expected {}",
					field.getAnnotationName(), maxValue.getClass().getSimpleName(), field.getFieldType().toString());

			if(def != null && ((Number)def).doubleValue() > ((Number)max).doubleValue())
				//defaultValue compatible with maxValue
				throw AnnotationException.create("Default value incompatible with maximum value in {}; found {}, expected less than or equals to {}",
					field.getAnnotationName(), field.getDefaultValue(), maxValue.getClass().getSimpleName());
		}
		return max;
	}

	public static void validateMinMaxValues(final ConfigFieldData field) throws AnnotationException, CodecException{
		if(!StringHelper.isBlank(field.getMinValue()) || !StringHelper.isBlank(field.getMaxValue())){
			final Class<?> fieldType = field.getFieldType();
			if(fieldType.isArray())
				throw AnnotationException.create("Array field should not have `minValue` or `maxValue`");

			final String defaultValue = field.getDefaultValue();
			final Object def = (!StringHelper.isBlank(defaultValue)
				? ParserDataType.getValue(fieldType, defaultValue)
				: null);
			final Object min = validateMinValue(field, def);
			final Object max = validateMaxValue(field, def);

			if(min != null && max != null && ((Number)min).doubleValue() > ((Number)max).doubleValue())
				//maxValue after or equal to minValue
				throw AnnotationException.create("Minimum value should be less than or equal to maximum value in {}; found {}, expected greater than or equals to {}",
					field.getAnnotationName(), defaultValue, field.getMinValue().getClass().getSimpleName());
		}
	}

	public static void validateDefaultValue(final ConfigFieldData field) throws AnnotationException, CodecException{
		final Class<?> fieldType = field.getFieldType();

		final String defaultValue = field.getDefaultValue();
		if(!StringHelper.isBlank(defaultValue)){
			//defaultValue compatible with variable type
			if(!field.hasEnumeration() && ParserDataType.getValue(fieldType, defaultValue) == null)
				throw AnnotationException.create("Incompatible enum in {}, found {}, expected {}",
					field.getAnnotationName(), defaultValue.getClass().getSimpleName(), fieldType.toString());
		}
		//if default value is not present, then field type must be an object
		else if(ParserDataType.isPrimitive(fieldType))
			throw AnnotationException.create("Default must be present for primitive type in {}, found {}, expected {}",
				field.getAnnotationName(), fieldType.getSimpleName(), fieldType.getSimpleName());
	}

	private static void validateMinMaxDefaultValuesToPattern(final Pattern formatPattern, final ConfigFieldData field)
			throws AnnotationException{
		//defaultValue compatible with pattern
		if(!matches(field.getDefaultValue(), formatPattern))
			throw AnnotationException.create("Default value not compatible with `pattern` in {}; found {}, expected {}",
				field.getAnnotationName(), field.getDefaultValue(), formatPattern.pattern());
		//minValue compatible with pattern
		if(!matches(field.getMinValue(), formatPattern))
			throw AnnotationException.create("Minimum value not compatible with `pattern` in {}; found {}, expected {}",
				field.getAnnotationName(), field.getMinValue(), formatPattern.pattern());
		//maxValue compatible with pattern
		if(!matches(field.getMaxValue(), formatPattern))
			throw AnnotationException.create("Maximum value not compatible with `pattern` in {}; found {}, expected {}",
				field.getAnnotationName(), field.getMaxValue(), formatPattern.pattern());
	}

	private static boolean matches(final CharSequence text, final Pattern pattern){
		return (StringHelper.isBlank(text) || pattern.matcher(text).matches());
	}

	public static void validatePattern(final ConfigFieldData field) throws AnnotationException{
		//valid pattern
		final String pattern = field.getPattern();
		if(!StringHelper.isBlank(pattern)){
			final Pattern formatPattern;
			try{
				formatPattern = Pattern.compile(pattern);
			}
			catch(final Exception e){
				throw AnnotationException.create("Invalid pattern in {} in field {}", field.getAnnotationName(), field.getFieldName(),
					e);
			}

			//defaultValue compatible with field type
			if(!String.class.isAssignableFrom(field.getFieldType()))
				throw AnnotationException.create("Data type not compatible with `pattern` in {}; found {}.class, expected String.class",
					field.getAnnotationName(), field.getFieldType());

			validateMinMaxDefaultValuesToPattern(formatPattern, field);
		}
	}


	public static void validateEnumeration(final ConfigFieldData field) throws AnnotationException{
		if(field.hasEnumeration()){
			//enumeration can be encoded
			final Class<? extends ConfigurationEnum> enumeration = field.getEnumeration();
			if(!ConfigurationEnum.class.isAssignableFrom(enumeration))
				throw AnnotationException.create("Enumeration must implement interface {} in {} in field {}",
					ConfigurationEnum.class.getSimpleName(), field.getAnnotationName(), field.getFieldName());

			//non-empty enumeration
			final ConfigurationEnum[] enumConstants = enumeration.getEnumConstants();
			if(enumConstants.length == 0)
				throw AnnotationException.create("Empty enum in {} in field {}", field.getAnnotationName(), field.getFieldName());

			final Class<?> fieldType = field.getFieldType();
			if(fieldType.isArray())
				validateEnumMultipleValues(field, enumConstants);
			else
				validateEnumerationMutuallyExclusive(field, enumConstants);
		}
	}

	private static void validateEnumMultipleValues(final ConfigFieldData field, final ConfigurationEnum[] enumConstants)
			throws AnnotationException{
		//enumeration compatible with variable type
		final Class<?> fieldType = field.getFieldType();
		if(!fieldType.getComponentType().isAssignableFrom(field.getEnumeration()))
			throw AnnotationException.create("Incompatible enum in {}; found {}, expected {}",
				field.getAnnotationName(), field.getEnumeration().getSimpleName(), fieldType.toString());

		final String defaultValue = field.getDefaultValue();
		if(!StringHelper.isBlank(defaultValue)){
			final String[] defaultValues = PATTERN_PIPE.split(defaultValue);
			if(fieldType.isEnum() && defaultValues.length != 1)
				throw AnnotationException.create("Default value for mutually exclusive enumeration field in {} should be a value; found {}, expected one of {}",
					field.getAnnotationName(), defaultValue, Arrays.toString(enumConstants));

			for(int i = 0; i < JavaHelper.lengthOrZero(defaultValues); i ++){
				final ConfigurationEnum enumValue = ConfigurationEnum.extractEnum(enumConstants, defaultValues[i]);
				if(enumValue == null)
					throw AnnotationException.create("Default value not compatible with `enumeration` in {}; found {}, expected one of {}",
						field.getAnnotationName(), defaultValues[i], Arrays.toString(enumConstants));
			}
		}
	}

	private static void validateEnumerationMutuallyExclusive(final ConfigFieldData field, final ConfigurationEnum[] enumConstants)
			throws AnnotationException{
		//enumeration compatible with variable type
		final Class<?> fieldType = field.getFieldType();
		if(!fieldType.isAssignableFrom(field.getEnumeration()))
			throw AnnotationException.create("Incompatible enum in {}; found {}, expected {}",
				field.getAnnotationName(), field.getEnumeration().getSimpleName(), fieldType.toString());

		final String defaultValue = field.getDefaultValue();
		if(!StringHelper.isBlank(defaultValue) && ConfigurationEnum.extractEnum(enumConstants, defaultValue) == null)
			throw AnnotationException.create("Default value not compatible with `enumeration` in {}; found {}, expected one of {}",
				field.getAnnotationName(), defaultValue, Arrays.toString(enumConstants));
	}

	public static void validateRadix(final int radix) throws AnnotationException{
		if(radix < Character.MIN_RADIX || radix > Character.MAX_RADIX)
			throw AnnotationException.create("Radix must be in [{}, {}]", Character.MIN_RADIX, Character.MAX_RADIX);
	}

}
