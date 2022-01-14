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
package io.github.mtrevisan.boxon.codecs.managers;

import io.github.mtrevisan.boxon.codecs.managers.configuration.ConfigFieldData;
import io.github.mtrevisan.boxon.exceptions.AnnotationException;
import io.github.mtrevisan.boxon.exceptions.CodecException;
import io.github.mtrevisan.boxon.external.codecs.ParserDataType;
import io.github.mtrevisan.boxon.external.configurations.ConfigurationEnum;
import io.github.mtrevisan.boxon.external.semanticversioning.Version;
import io.github.mtrevisan.boxon.internal.JavaHelper;
import io.github.mtrevisan.boxon.internal.StringHelper;

import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.regex.Pattern;


final class ValidationHelper{

	private static final Pattern PATTERN_PIPE = Pattern.compile("\\|");


	private ValidationHelper(){}

	static void assertValidCharset(final String charsetName) throws AnnotationException{
		try{
			Charset.forName(charsetName);
		}
		catch(final IllegalArgumentException ignored){
			throw AnnotationException.create("Invalid charset: '{}'", charsetName);
		}
	}


	static void validateProtocol(final ConfigFieldData field, final Version minProtocolVersion,
			final Version maxProtocolVersion) throws AnnotationException{
		if(StringHelper.isBlank(field.minProtocol) && StringHelper.isBlank(field.maxProtocol))
			return;

		final Version minimum = validateProtocol(field.minProtocol, field.getAnnotationName(),
			"Invalid minimum protocol version in {}; found {}");
		final Version maximum = validateProtocol(field.maxProtocol, field.getAnnotationName(),
			"Invalid maximum protocol version in {}; found {}");

		//maxProtocol after or equal to minProtocol
		if(minimum != null && maximum != null && maximum.isLessThan(minimum))
			throw AnnotationException.create("Minimum protocol version is greater than maximum protocol version in {}; found {}",
				field.getAnnotationName(), field.maxProtocol);

		//minProtocol after or equal to minProtocolVersion
		if(minimum != null && !minProtocolVersion.isEmpty() && minimum.isLessThan(minProtocolVersion))
			throw AnnotationException.create("Minimum protocol version is less than whole message minimum protocol version in {}; found {}",
				field.getAnnotationName(), maxProtocolVersion);
		//maxProtocol before or equal to maxProtocolVersion
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
		if(!StringHelper.isBlank(field.minValue)){
			min = ParserDataType.getValue(field.getFieldType(), field.minValue);
			//minValue compatible with variable type
			if(min == null)
				throw AnnotationException.create("Incompatible minimum value in {}; found {}, expected {}",
					field.getAnnotationName(), field.minValue.getClass().getSimpleName(), field.getFieldType().toString());

			if(def != null && ((Number)def).doubleValue() < ((Number)min).doubleValue())
				//defaultValue compatible with minValue
				throw AnnotationException.create("Default value incompatible with minimum value in {}; found {}, expected greater than or equals to {}",
					field.getAnnotationName(), field.defaultValue, field.minValue.getClass().getSimpleName());
		}
		return min;
	}

	private static Object validateMaxValue(final ConfigFieldData field, final Object def) throws AnnotationException, CodecException{
		Object max = null;
		if(!StringHelper.isBlank(field.maxValue)){
			max = ParserDataType.getValue(field.getFieldType(), field.maxValue);
			//maxValue compatible with variable type
			if(max == null)
				throw AnnotationException.create("Incompatible maximum value in {}; found {}, expected {}",
					field.getAnnotationName(), field.maxValue.getClass().getSimpleName(), field.getFieldType().toString());

			if(def != null && ((Number)def).doubleValue() > ((Number)max).doubleValue())
				//defaultValue compatible with maxValue
				throw AnnotationException.create("Default value incompatible with maximum value in {}; found {}, expected less than or equals to {}",
					field.getAnnotationName(), field.defaultValue, field.maxValue.getClass().getSimpleName());
		}
		return max;
	}

	static void validateMinMaxValues(final ConfigFieldData field) throws AnnotationException, CodecException{
		if(!StringHelper.isBlank(field.minValue) || !StringHelper.isBlank(field.maxValue)){
			final Class<?> fieldType = field.getFieldType();
			if(fieldType.isArray())
				throw AnnotationException.create("Array field should not have `minValue` or `maxValue`");

			final Object def = (!StringHelper.isBlank(field.defaultValue)? ParserDataType.getValue(fieldType, field.defaultValue): null);
			final Object min = validateMinValue(field, def);
			final Object max = validateMaxValue(field, def);

			if(min != null && max != null && ((Number)min).doubleValue() > ((Number)max).doubleValue())
				//maxValue after or equal to minValue
				throw AnnotationException.create("Minimum value should be less than or equal to maximum value in {}; found {}, expected greater than or equals to {}",
					field.getAnnotationName(), field.defaultValue, field.minValue.getClass().getSimpleName());
		}
	}

	static void validateDefaultValue(final ConfigFieldData field) throws AnnotationException, CodecException{
		final Class<?> fieldType = field.getFieldType();

		if(!StringHelper.isBlank(field.defaultValue)){
			//defaultValue compatible with variable type
			if(!field.hasEnumeration() && ParserDataType.getValue(fieldType, field.defaultValue) == null)
				throw AnnotationException.create("Incompatible enum in {}, found {}, expected {}",
					field.getAnnotationName(), field.defaultValue.getClass().getSimpleName(), fieldType.toString());
		}
		//if default value is not present, then field type must be an object
		else if(ParserDataType.isPrimitive(fieldType))
			throw AnnotationException.create("Default must be present for primitive type in {}, found {}, expected {}",
				field.getAnnotationName(), fieldType.getSimpleName(), fieldType.getSimpleName());
	}

	private static void validateMinMaxDefaultValuesToPattern(final Pattern formatPattern, final ConfigFieldData field)
			throws AnnotationException{
		//defaultValue compatible with pattern
		if(!matches(field.defaultValue, formatPattern))
			throw AnnotationException.create("Default value not compatible with `pattern` in {}; found {}, expected {}",
				field.getAnnotationName(), field.defaultValue, formatPattern.pattern());
		//minValue compatible with pattern
		if(!matches(field.minValue, formatPattern))
			throw AnnotationException.create("Minimum value not compatible with `pattern` in {}; found {}, expected {}",
				field.getAnnotationName(), field.minValue, formatPattern.pattern());
		//maxValue compatible with pattern
		if(!matches(field.maxValue, formatPattern))
			throw AnnotationException.create("Maximum value not compatible with `pattern` in {}; found {}, expected {}",
				field.getAnnotationName(), field.maxValue, formatPattern.pattern());
	}

	private static boolean matches(final CharSequence text, final Pattern pattern){
		return (StringHelper.isBlank(text) || pattern.matcher(text).matches());
	}

	static void validatePattern(final ConfigFieldData field) throws AnnotationException{
		//valid pattern
		if(!StringHelper.isBlank(field.pattern)){
			final Pattern formatPattern;
			try{
				formatPattern = Pattern.compile(field.pattern);
			}
			catch(final Exception e){
				throw AnnotationException.create("Invalid pattern in {} in field {}", field.getAnnotationName(), field.field.getName(),
					e);
			}

			//defaultValue compatible with field type
			if(!String.class.isAssignableFrom(field.getFieldType()))
				throw AnnotationException.create("Data type not compatible with `pattern` in {}; found {}.class, expected String.class",
					field.getAnnotationName(), field.getFieldType());

			validateMinMaxDefaultValuesToPattern(formatPattern, field);
		}
	}


	static void validateEnumeration(final ConfigFieldData field) throws AnnotationException{
		if(field.hasEnumeration()){
			//enumeration can be encoded
			if(!ConfigurationEnum.class.isAssignableFrom(field.enumeration))
				throw AnnotationException.create("Enumeration must implement interface {} in {} in field {}",
					ConfigurationEnum.class.getSimpleName(), field.getAnnotationName(), field.field.getName());

			//non-empty enumeration
			final ConfigurationEnum[] enumConstants = field.enumeration.getEnumConstants();
			if(enumConstants.length == 0)
				throw AnnotationException.create("Empty enum in {} in field {}", field.getAnnotationName(), field.field.getName());

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
		if(!fieldType.getComponentType().isAssignableFrom(field.enumeration))
			throw AnnotationException.create("Incompatible enum in {}; found {}, expected {}",
				field.getAnnotationName(), field.enumeration.getSimpleName(), fieldType.toString());

		if(!StringHelper.isBlank(field.defaultValue)){
			final String[] defaultValues = PATTERN_PIPE.split(field.defaultValue);
			if(fieldType.isEnum() && defaultValues.length != 1)
				throw AnnotationException.create("Default value for mutually exclusive enumeration field in {} should be a value; found {}, expected one of {}",
					field.getAnnotationName(), field.defaultValue, Arrays.toString(enumConstants));

			for(int i = 0; i < JavaHelper.lengthOrZero(defaultValues); i ++)
				if(ConfigurationEnum.extractEnum(enumConstants, defaultValues[i]) == null)
					throw AnnotationException.create("Default value not compatible with `enumeration` in {}; found {}, expected one of {}",
						field.getAnnotationName(), defaultValues[i], Arrays.toString(enumConstants));
		}
	}

	private static void validateEnumerationMutuallyExclusive(final ConfigFieldData field, final ConfigurationEnum[] enumConstants)
			throws AnnotationException{
		//enumeration compatible with variable type
		final Class<?> fieldType = field.getFieldType();
		if(!fieldType.isAssignableFrom(field.enumeration))
			throw AnnotationException.create("Incompatible enum in {}; found {}, expected {}",
				field.getAnnotationName(), field.enumeration.getSimpleName(), fieldType.toString());

		if(!StringHelper.isBlank(field.defaultValue) && ConfigurationEnum.extractEnum(enumConstants, field.defaultValue) == null)
			throw AnnotationException.create("Default value not compatible with `enumeration` in {}; found {}, expected one of {}",
				field.getAnnotationName(), field.defaultValue, Arrays.toString(enumConstants));
	}

	static void validateRadix(final int radix) throws AnnotationException{
		if(radix < Character.MIN_RADIX || radix > Character.MAX_RADIX)
			throw AnnotationException.create("Radix must be in [{}, {}]", Character.MIN_RADIX, Character.MAX_RADIX);
	}

}
