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

import io.github.mtrevisan.boxon.annotations.configurations.ConfigurationEnum;
import io.github.mtrevisan.boxon.annotations.configurations.ConfigurationField;
import io.github.mtrevisan.boxon.annotations.configurations.NullEnum;
import io.github.mtrevisan.boxon.exceptions.AnnotationException;
import io.github.mtrevisan.boxon.exceptions.CodecException;
import io.github.mtrevisan.boxon.external.semanticversioning.Version;
import io.github.mtrevisan.boxon.internal.JavaHelper;
import io.github.mtrevisan.boxon.internal.ParserDataType;
import io.github.mtrevisan.boxon.internal.StringHelper;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
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


	static void validateProtocol(final String minProtocol, final String maxProtocol, final Version minMessageProtocol,
			final Version maxMessageProtocol, final Class<? extends Annotation> binding) throws AnnotationException{
		if(!StringHelper.isBlank(minProtocol) || !StringHelper.isBlank(maxProtocol)){
			//minProtocol/maxProtocol are valid
			final Version minimum = validateMinMaxProtocol(minProtocol, binding, "Invalid minimum protocol version in {}; found {}");
			final Version maximum = validateMinMaxProtocol(maxProtocol, binding, "Invalid maximum protocol version in {}; found {}");
			//maxProtocol after or equal to minProtocol
			if(minimum != null && maximum != null && maximum.isLessThan(minimum))
				throw AnnotationException.create("Minimum protocol version is greater than maximum protocol version in {}; found {}",
					binding.getSimpleName(), maxProtocol);

			//minProtocol after or equal to minMessageProtocol
			if(minimum != null && !minMessageProtocol.isEmpty() && minimum.isLessThan(minMessageProtocol))
				throw AnnotationException.create("Minimum protocol version is less than whole message minimum protocol version in {}; found {}",
					binding.getSimpleName(), maxMessageProtocol);
			//maxProtocol before or equal to maxMessageProtocol
			if(maximum != null && !maxMessageProtocol.isEmpty() && maxMessageProtocol.isLessThan(maximum))
				throw AnnotationException.create("Maximum protocol version is greater than whole message maximum protocol version in {}; found {}",
					binding.getSimpleName(), maxMessageProtocol);
		}
	}

	private static Version validateMinMaxProtocol(final String protocolVersion, final Class<? extends Annotation> binding,
			final String errorMessage) throws AnnotationException{
		Version protocol = null;
		if(!StringHelper.isBlank(protocolVersion)){
			try{
				protocol = Version.of(protocolVersion);
			}catch(final IllegalArgumentException iae){
				throw AnnotationException.create(iae, errorMessage, binding.getSimpleName(), protocolVersion);
			}
		}
		return protocol;
	}

	static Object validateMinValue(final Class<?> fieldType, final String minValue, final String defaultValue, final Object def,
			final Class<? extends Annotation> annotation) throws AnnotationException, CodecException{
		Object min = null;
		if(!StringHelper.isBlank(minValue)){
			min = JavaHelper.getValue(fieldType, minValue);
			//minValue compatible with variable type
			if(min == null)
				throw AnnotationException.create("Incompatible minimum value in {}; found {}, expected {}",
					annotation.getSimpleName(), minValue.getClass().getSimpleName(), fieldType.toString());

			if(def != null && ((Number)def).doubleValue() < ((Number)min).doubleValue())
				//defaultValue compatible with minValue
				throw AnnotationException.create("Default value incompatible with minimum value in {}; found {}, expected greater than or equals to {}",
					annotation.getSimpleName(), defaultValue, minValue.getClass().getSimpleName());
		}
		return min;
	}

	static Object validateMaxValue(final Class<?> fieldType, final String maxValue, final String defaultValue, final Object def,
			final Class<? extends Annotation> annotation) throws AnnotationException, CodecException{
		Object max = null;
		if(!StringHelper.isBlank(maxValue)){
			max = JavaHelper.getValue(fieldType, maxValue);
			//maxValue compatible with variable type
			if(max == null)
				throw AnnotationException.create("Incompatible maximum value in {}; found {}, expected {}",
					annotation.getSimpleName(), maxValue.getClass().getSimpleName(), fieldType.toString());

			if(def != null && ((Number)def).doubleValue() > ((Number)max).doubleValue())
				//defaultValue compatible with maxValue
				throw AnnotationException.create("Default value incompatible with maximum value in {}; found {}, expected less than or equals to {}",
					annotation.getSimpleName(), defaultValue, maxValue.getClass().getSimpleName());
		}
		return max;
	}

	static void validateMinMaxValues(final Field field, final String minValue, final String maxValue, final String defaultValue,
			final Class<? extends Annotation> annotation) throws AnnotationException, CodecException{
		final Class<?> fieldType = field.getType();

		if(!StringHelper.isBlank(minValue) || !StringHelper.isBlank(maxValue)){
			final Object def = (!StringHelper.isBlank(defaultValue)? JavaHelper.getValue(fieldType, defaultValue): null);
			final Object min = validateMinValue(fieldType, minValue, defaultValue, def, annotation);
			final Object max = validateMaxValue(fieldType, maxValue, defaultValue, def, annotation);

			if(min != null && max != null && ((Number)min).doubleValue() > ((Number)max).doubleValue())
				//maxValue after or equal to minValue
				throw AnnotationException.create("Minimum value should be less than or equal to maximum value in {}; found {}, expected greater than or equals to {}",
					annotation.getSimpleName(), defaultValue, minValue.getClass().getSimpleName());
		}
	}

	static void validateDefaultValue(final Field field, final String defaultValue, final Class<? extends Enum<?>> enumeration,
			final Class<? extends Annotation> annotation) throws AnnotationException, CodecException{
		final Class<?> fieldType = field.getType();

		if(!StringHelper.isBlank(defaultValue)){
			//defaultValue compatible with variable type
			if(enumeration == NullEnum.class && JavaHelper.getValue(fieldType, defaultValue) == null)
				throw AnnotationException.create("Incompatible enum in {}, found {}, expected {}",
					annotation.getSimpleName(), defaultValue.getClass().getSimpleName(), fieldType.toString());
		}
		//if default value is not present, then field type must be an object
		else if(ParserDataType.isPrimitive(fieldType))
			throw AnnotationException.create("Default must be present for primitive type in {}, found {}, expected {}",
				annotation.getSimpleName(), fieldType.getSimpleName(),
				ParserDataType.toObjectiveTypeOrSelf(fieldType).getSimpleName());
	}

	static void validateMinMaxDefaultValuesToPattern(final Pattern formatPattern, final String minValue, final String maxValue,
			final String defaultValue, final Class<? extends Annotation> annotation) throws AnnotationException{
		//defaultValue compatible with pattern
		if(!StringHelper.isBlank(defaultValue) && !formatPattern.matcher(defaultValue).matches())
			throw AnnotationException.create("Default value not compatible with `pattern` in {}; found {}, expected {}",
				annotation.getSimpleName(), defaultValue, formatPattern.pattern());
		//minValue compatible with pattern
		if(!StringHelper.isBlank(minValue) && !formatPattern.matcher(minValue).matches())
			throw AnnotationException.create("Minimum value not compatible with `pattern` in {}; found {}, expected {}",
				annotation.getSimpleName(), minValue, formatPattern.pattern());
		//maxValue compatible with pattern
		if(!StringHelper.isBlank(maxValue) && !formatPattern.matcher(maxValue).matches())
			throw AnnotationException.create("Maximum value not compatible with `pattern` in {}; found {}, expected {}",
				annotation.getSimpleName(), maxValue, formatPattern.pattern());
	}

	static void validatePattern(final Field field, final String pattern, final String minValue, final String maxValue,
			final String defaultValue, final Class<? extends Annotation> annotation) throws AnnotationException{
		//valid pattern
		if(!StringHelper.isBlank(pattern)){
			try{
				final Pattern formatPattern = Pattern.compile(pattern);

				//defaultValue compatible with field type
				if(!String.class.isAssignableFrom(field.getType()))
					throw AnnotationException.create("Data type not compatible with `pattern` in {}; found {}.class, expected String.class",
						annotation.getSimpleName(), field.getType());

				validateMinMaxDefaultValuesToPattern(formatPattern, minValue, maxValue, defaultValue, annotation);
			}
			catch(final AnnotationException ae){
				throw ae;
			}
			catch(final Exception e){
				throw AnnotationException.create("Invalid pattern in {} in field {}", annotation.getSimpleName(),
					field.getName(), e);
			}
		}
	}


	static void validateEnumeration(final Field field, final Class<? extends Enum<?>> enumeration, final String defaultValue,
			final Class<? extends Annotation> annotation) throws AnnotationException{
		final Class<?> fieldType = field.getType();
		final boolean isFieldArray = fieldType.isArray();

		if(enumeration != NullEnum.class){
			//enumeration can be encoded
			if(!ConfigurationEnum.class.isAssignableFrom(enumeration))
				throw AnnotationException.create("Enum must implement ConfigurationEnum.class in {} in field {}",
					annotation.getSimpleName(), field.getName());

			//non-empty enumeration
			final Enum<?>[] enumConstants = enumeration.getEnumConstants();
			if(enumConstants.length == 0)
				throw AnnotationException.create("Empty enum in {} in field {}", annotation.getSimpleName(),
					field.getName());

			//enumeration compatible with variable type
			if(isFieldArray)
				validateEnumMultipleValues(fieldType, enumeration, enumConstants, defaultValue, annotation);
			else
				validateEnumerationMutuallyExclusive(fieldType, enumeration, enumConstants, defaultValue, annotation);
		}
	}

	private static void validateEnumMultipleValues(final Class<?> fieldType, final Class<? extends Enum<?>> enumeration,
			final Enum<?>[] enumConstants, final String defaultValue, final Class<? extends Annotation> annotation) throws AnnotationException{
		if(!fieldType.getComponentType().isAssignableFrom(enumeration))
			throw AnnotationException.create("Incompatible enum in {}; found {}, expected {}",
				annotation.getSimpleName(), enumeration.getSimpleName(), fieldType.toString());

		if(!StringHelper.isBlank(defaultValue)){
			final String[] defaultValues = StringHelper.split(defaultValue, '|', -1);
			if(fieldType.isEnum() && defaultValues.length != 1)
				throw AnnotationException.create("Default value for mutually exclusive enumeration field in {} should be a value; found {}, expected one of {}",
					annotation.getSimpleName(), defaultValue, Arrays.toString(enumConstants));

			for(int i = 0; i < JavaHelper.lengthOrZero(defaultValues); i ++){
				final String dv = defaultValues[i];
				if(JavaHelper.extractEnum(enumConstants, dv) == null)
					throw AnnotationException.create("Default value not compatible with `enumeration` in {}; found {}, expected one of {}",
						ConfigurationField.class.getSimpleName(), dv, Arrays.toString(enumConstants));
			}
		}
	}

	private static void validateEnumerationMutuallyExclusive(final Class<?> fieldType, final Class<? extends Enum<?>> enumeration,
			final Enum<?>[] enumConstants, final String defaultValue, final Class<? extends Annotation> annotation) throws AnnotationException{
		if(!fieldType.isAssignableFrom(enumeration))
			throw AnnotationException.create("Incompatible enum in {}; found {}, expected {}",
				annotation.getSimpleName(), enumeration.getSimpleName(), fieldType.toString());

		if(!StringHelper.isBlank(defaultValue) && JavaHelper.extractEnum(enumConstants, defaultValue) == null)
			throw AnnotationException.create("Default value not compatible with `enumeration` in {}; found {}, expected one of {}",
				annotation.getSimpleName(), defaultValue, Arrays.toString(enumConstants));
	}

	static void validateRadix(final int radix) throws AnnotationException{
		if(radix < Character.MIN_RADIX || radix > Character.MAX_RADIX)
			throw AnnotationException.create("Radix must be in [{}, {}]", Character.MIN_RADIX, Character.MAX_RADIX);
	}

}
