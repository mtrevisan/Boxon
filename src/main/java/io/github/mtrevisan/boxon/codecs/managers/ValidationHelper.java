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

import io.github.mtrevisan.boxon.annotations.configurations.ConfigurationEnum;
import io.github.mtrevisan.boxon.annotations.configurations.NullEnum;
import io.github.mtrevisan.boxon.codecs.managers.field.ConfigFieldData;
import io.github.mtrevisan.boxon.exceptions.AnnotationException;
import io.github.mtrevisan.boxon.exceptions.CodecException;
import io.github.mtrevisan.boxon.external.semanticversioning.Version;
import io.github.mtrevisan.boxon.internal.JavaHelper;
import io.github.mtrevisan.boxon.internal.ParserDataType;
import io.github.mtrevisan.boxon.internal.StringHelper;

import java.lang.annotation.Annotation;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.regex.Pattern;


final class ValidationHelper{

	private ValidationHelper(){}

	static void assertValidCharset(final String charsetName) throws AnnotationException{
		try{
			Charset.forName(charsetName);
		}
		catch(final IllegalArgumentException ignored){
			throw AnnotationException.create("Invalid charset: '{}'", charsetName);
		}
	}


	static <T extends Annotation> void validateProtocol(final ConfigFieldData<T> field, final Version minProtocolVersion,
			final Version maxProtocolVersion) throws AnnotationException{
		if(!StringHelper.isBlank(field.minProtocol) || !StringHelper.isBlank(field.maxProtocol)){
			//minProtocol/maxProtocol are valid
			final Version minimum = validateProtocol(field.minProtocol, field.annotation, "Invalid minimum protocol version in {}; found {}");
			final Version maximum = validateProtocol(field.maxProtocol, field.annotation, "Invalid maximum protocol version in {}; found {}");
			//maxProtocol after or equal to minProtocol
			if(minimum != null && maximum != null && maximum.isLessThan(minimum))
				throw AnnotationException.create("Minimum protocol version is greater than maximum protocol version in {}; found {}",
					field.annotation.getSimpleName(), field.maxProtocol);

			//minProtocol after or equal to minProtocolVersion
			if(minimum != null && !minProtocolVersion.isEmpty() && minimum.isLessThan(minProtocolVersion))
				throw AnnotationException.create("Minimum protocol version is less than whole message minimum protocol version in {}; found {}",
					field.annotation.getSimpleName(), maxProtocolVersion);
			//maxProtocol before or equal to maxProtocolVersion
			if(maximum != null && !maxProtocolVersion.isEmpty() && maxProtocolVersion.isLessThan(maximum))
				throw AnnotationException.create("Maximum protocol version is greater than whole message maximum protocol version in {}; found {}",
					field.annotation.getSimpleName(), maxProtocolVersion);
		}
	}

	private static Version validateProtocol(final String protocolVersion, final Class<? extends Annotation> binding,
			final String errorMessage) throws AnnotationException{
		Version protocol = null;
		if(!StringHelper.isBlank(protocolVersion)){
			try{
				protocol = Version.of(protocolVersion);
			}
			catch(final IllegalArgumentException iae){
				throw AnnotationException.create(iae, errorMessage, binding.getSimpleName(), protocolVersion);
			}
		}
		return protocol;
	}

	static <T extends Annotation> Object validateMinValue(final ConfigFieldData<T> field, final Object def) throws AnnotationException,
			CodecException{
		Object min = null;
		if(!StringHelper.isBlank(field.minValue)){
			min = JavaHelper.getValue(field.getFieldType(), field.minValue);
			//minValue compatible with variable type
			if(min == null)
				throw AnnotationException.create("Incompatible minimum value in {}; found {}, expected {}",
					field.annotation.getSimpleName(), field.minValue.getClass().getSimpleName(), field.getFieldType().toString());

			if(def != null && ((Number)def).doubleValue() < ((Number)min).doubleValue())
				//defaultValue compatible with minValue
				throw AnnotationException.create("Default value incompatible with minimum value in {}; found {}, expected greater than or equals to {}",
					field.annotation.getSimpleName(), field.defaultValue, field.minValue.getClass().getSimpleName());
		}
		return min;
	}

	static <T extends Annotation> Object validateMaxValue(final ConfigFieldData<T> field, final Object def) throws AnnotationException,
			CodecException{
		Object max = null;
		if(!StringHelper.isBlank(field.maxValue)){
			max = JavaHelper.getValue(field.getFieldType(), field.maxValue);
			//maxValue compatible with variable type
			if(max == null)
				throw AnnotationException.create("Incompatible maximum value in {}; found {}, expected {}",
					field.annotation.getSimpleName(), field.maxValue.getClass().getSimpleName(), field.getFieldType().toString());

			if(def != null && ((Number)def).doubleValue() > ((Number)max).doubleValue())
				//defaultValue compatible with maxValue
				throw AnnotationException.create("Default value incompatible with maximum value in {}; found {}, expected less than or equals to {}",
					field.annotation.getSimpleName(), field.defaultValue, field.maxValue.getClass().getSimpleName());
		}
		return max;
	}

	static <T extends Annotation> void validateMinMaxValues(final ConfigFieldData<T> field) throws AnnotationException, CodecException{
		final Class<?> fieldType = field.getFieldType();

		if(!StringHelper.isBlank(field.minValue) || !StringHelper.isBlank(field.maxValue)){
			final Object def = (!StringHelper.isBlank(field.defaultValue)? JavaHelper.getValue(fieldType, field.defaultValue): null);
			final Object min = validateMinValue(field, def);
			final Object max = validateMaxValue(field, def);

			if(min != null && max != null && ((Number)min).doubleValue() > ((Number)max).doubleValue())
				//maxValue after or equal to minValue
				throw AnnotationException.create("Minimum value should be less than or equal to maximum value in {}; found {}, expected greater than or equals to {}",
					field.annotation.getSimpleName(), field.defaultValue, field.minValue.getClass().getSimpleName());
		}
	}

	static <T extends Annotation> void validateDefaultValue(final ConfigFieldData<T> field) throws AnnotationException, CodecException{
		final Class<?> fieldType = field.getFieldType();

		if(!StringHelper.isBlank(field.defaultValue)){
			//defaultValue compatible with variable type
			if(field.enumeration == NullEnum.class && JavaHelper.getValue(fieldType, field.defaultValue) == null)
				throw AnnotationException.create("Incompatible enum in {}, found {}, expected {}",
					field.annotation.getSimpleName(), field.defaultValue.getClass().getSimpleName(), fieldType.toString());
		}
		//if default value is not present, then field type must be an object
		else if(ParserDataType.isPrimitive(fieldType))
			throw AnnotationException.create("Default must be present for primitive type in {}, found {}, expected {}",
				field.annotation.getSimpleName(), fieldType.getSimpleName(),
				ParserDataType.toObjectiveTypeOrSelf(fieldType).getSimpleName());
	}

	static <T extends Annotation> void validateMinMaxDefaultValuesToPattern(final Pattern formatPattern, final ConfigFieldData<T> field)
			throws AnnotationException{
		//defaultValue compatible with pattern
		if(!StringHelper.isBlank(field.defaultValue) && !formatPattern.matcher(field.defaultValue).matches())
			throw AnnotationException.create("Default value not compatible with `pattern` in {}; found {}, expected {}",
				field.annotation.getSimpleName(), field.defaultValue, formatPattern.pattern());
		//minValue compatible with pattern
		if(!StringHelper.isBlank(field.minValue) && !formatPattern.matcher(field.minValue).matches())
			throw AnnotationException.create("Minimum value not compatible with `pattern` in {}; found {}, expected {}",
				field.annotation.getSimpleName(), field.minValue, formatPattern.pattern());
		//maxValue compatible with pattern
		if(!StringHelper.isBlank(field.maxValue) && !formatPattern.matcher(field.maxValue).matches())
			throw AnnotationException.create("Maximum value not compatible with `pattern` in {}; found {}, expected {}",
				field.annotation.getSimpleName(), field.maxValue, formatPattern.pattern());
	}

	static <T extends Annotation> void validatePattern(final ConfigFieldData<T> field) throws AnnotationException{
		//valid pattern
		if(!StringHelper.isBlank(field.pattern)){
			final Pattern formatPattern;
			try{
				formatPattern = Pattern.compile(field.pattern);
			}
			catch(final Exception e){
				throw AnnotationException.create("Invalid pattern in {} in field {}", field.annotation.getSimpleName(),
					field.field.getName(), e);
			}

			//defaultValue compatible with field type
			if(!String.class.isAssignableFrom(field.getFieldType()))
				throw AnnotationException.create("Data type not compatible with `pattern` in {}; found {}.class, expected String.class",
					field.annotation.getSimpleName(), field.getFieldType());

			validateMinMaxDefaultValuesToPattern(formatPattern, field);
		}
	}


	static <T extends Annotation> void validateEnumeration(final ConfigFieldData<T> field) throws AnnotationException{
		final boolean isFieldArray = field.getFieldType().isArray();

		if(field.enumeration != NullEnum.class){
			//enumeration can be encoded
			if(!ConfigurationEnum.class.isAssignableFrom(field.enumeration))
				throw AnnotationException.create("Enum must implement ConfigurationEnum.class in {} in field {}",
					field.annotation.getSimpleName(), field.field.getName());

			//non-empty enumeration
			final Enum<?>[] enumConstants = field.enumeration.getEnumConstants();
			if(enumConstants.length == 0)
				throw AnnotationException.create("Empty enum in {} in field {}", field.annotation.getSimpleName(),
					field.field.getName());

			if(isFieldArray)
				validateEnumMultipleValues(field, enumConstants);
			else
				validateEnumerationMutuallyExclusive(field, enumConstants);
		}
	}

	private static <T extends Annotation> void validateEnumMultipleValues(final ConfigFieldData<T> field, final Enum<?>[] enumConstants)
			throws AnnotationException{
		//enumeration compatible with variable type
		if(!field.getFieldType().getComponentType().isAssignableFrom(field.enumeration))
			throw AnnotationException.create("Incompatible enum in {}; found {}, expected {}",
				field.annotation.getSimpleName(), field.enumeration.getSimpleName(), field.getFieldType().toString());

		if(!StringHelper.isBlank(field.defaultValue)){
			final String[] defaultValues = StringHelper.split(field.defaultValue, '|', -1);
			if(field.getFieldType().isEnum() && defaultValues.length != 1)
				throw AnnotationException.create("Default value for mutually exclusive enumeration field in {} should be a value; found {}, expected one of {}",
					field.annotation.getSimpleName(), field.defaultValue, Arrays.toString(enumConstants));

			for(int i = 0; i < JavaHelper.lengthOrZero(defaultValues); i ++)
				if(JavaHelper.extractEnum(enumConstants, defaultValues[i]) == null)
					throw AnnotationException.create("Default value not compatible with `enumeration` in {}; found {}, expected one of {}",
						field.annotation.getSimpleName(), defaultValues[i], Arrays.toString(enumConstants));
		}
	}

	private static <T extends Annotation> void validateEnumerationMutuallyExclusive(final ConfigFieldData<T> field,
			final Enum<?>[] enumConstants) throws AnnotationException{
		//enumeration compatible with variable type
		if(!field.getFieldType().isAssignableFrom(field.enumeration))
			throw AnnotationException.create("Incompatible enum in {}; found {}, expected {}",
				field.annotation.getSimpleName(), field.enumeration.getSimpleName(), field.getFieldType().toString());

		if(!StringHelper.isBlank(field.defaultValue) && JavaHelper.extractEnum(enumConstants, field.defaultValue) == null)
			throw AnnotationException.create("Default value not compatible with `enumeration` in {}; found {}, expected one of {}",
				field.annotation.getSimpleName(), field.defaultValue, Arrays.toString(enumConstants));
	}

	static void validateRadix(final int radix) throws AnnotationException{
		if(radix < Character.MIN_RADIX || radix > Character.MAX_RADIX)
			throw AnnotationException.create("Radix must be in [{}, {}]", Character.MIN_RADIX, Character.MAX_RADIX);
	}

}
