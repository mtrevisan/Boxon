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

import io.github.mtrevisan.boxon.annotations.configurations.AlternativeConfigurationField;
import io.github.mtrevisan.boxon.annotations.configurations.AlternativeSubField;
import io.github.mtrevisan.boxon.annotations.configurations.CompositeConfigurationField;
import io.github.mtrevisan.boxon.annotations.configurations.CompositeSubField;
import io.github.mtrevisan.boxon.annotations.configurations.ConfigurationEnum;
import io.github.mtrevisan.boxon.annotations.configurations.ConfigurationField;
import io.github.mtrevisan.boxon.annotations.configurations.ConfigurationHeader;
import io.github.mtrevisan.boxon.annotations.configurations.NullEnum;
import io.github.mtrevisan.boxon.exceptions.AnnotationException;
import io.github.mtrevisan.boxon.external.semanticversioning.Version;
import io.github.mtrevisan.boxon.internal.JavaHelper;
import io.github.mtrevisan.boxon.internal.ParserDataType;
import io.github.mtrevisan.boxon.internal.StringHelper;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;


enum ConfigurationAnnotationValidator{

	HEADER(ConfigurationHeader.class){
		@Override
		void validate(final Field field, final Annotation annotation, final Version minMessageProtocol, final Version maxMessageProtocol)
				throws AnnotationException{
			final ConfigurationHeader binding = (ConfigurationHeader)annotation;

			if(StringHelper.isBlank(binding.shortDescription()))
				throw AnnotationException.create("Short description must be present");

			//check minimum/maximum protocol
			final String minProtocol = binding.minProtocol();
			final String maxProtocol = binding.minProtocol();
			if(!minProtocol.isEmpty() && !maxProtocol.isEmpty()){
				final Version min = Version.of(minProtocol);
				final Version max = Version.of(maxProtocol);
				if(max.isLessThan(min))
					throw AnnotationException.create("Maximum protocol should be after minimum protocol in {}; min is {}, max is {}",
						ConfigurationField.class.getSimpleName(), minProtocol, maxProtocol);
			}
		}
	},

	FIELD(ConfigurationField.class){
		@Override
		void validate(final Field field, final Annotation annotation, final Version minMessageProtocol, final Version maxMessageProtocol)
				throws AnnotationException{
			final ConfigurationField binding = (ConfigurationField)annotation;

			if(StringHelper.isBlank(binding.shortDescription()))
				throw AnnotationException.create("Short description must be present");
			if(binding.enumeration() == NullEnum.class && field.getType().isEnum())
				throw AnnotationException.create("Unnecessary mutually exclusive field in a non-enumeration field");
			if(String.class.isAssignableFrom(field.getType()))
				ValidationHelper.assertValidCharset(binding.charset());
			ValidationHelper.validateRadix(binding.radix());

			validateMinimumParameters(field, binding);

			final String pattern = binding.pattern();
			final String minValue = binding.minValue();
			final String maxValue = binding.maxValue();
			final String defaultValue = binding.defaultValue();
			ValidationHelper.validatePattern(field, pattern, minValue, maxValue, defaultValue);

			validateDefaultValue(field, binding);

			validateEnumeration(field, binding);

			ValidationHelper.validateMinMaxValues(field, minValue, maxValue, defaultValue);

			ValidationHelper.validateProtocol(binding.minProtocol(), binding.maxProtocol(), minMessageProtocol, maxMessageProtocol,
				ConfigurationField.class);
		}

		private void validateMinimumParameters(final Field field, final ConfigurationField binding) throws AnnotationException{
			final Class<?> fieldType = field.getType();
			final boolean isFieldArray = fieldType.isArray();
			final String pattern = binding.pattern();
			final String minValue = binding.minValue();
			final String maxValue = binding.maxValue();
			final Class<? extends Enum<?>> enumeration = binding.enumeration();

			//one only of `pattern`, `minValue`/`maxValue`, and `enumeration` should be set:
			final boolean hasPattern = !pattern.isEmpty();
			final boolean hasMinMaxValues = (!minValue.isEmpty() || !maxValue.isEmpty());
			final boolean hasEnumeration = (enumeration != NullEnum.class);
			if(moreThanOneSet(hasPattern, hasMinMaxValues, hasEnumeration))
				throw AnnotationException.create("Only one of `pattern`, `minValue`/`maxValue`, or `enumeration` should be used in {}",
					ConfigurationField.class.getSimpleName());
			if(isFieldArray && !hasEnumeration)
				throw AnnotationException.create("Array field should have `enumeration`");
		}

		private boolean moreThanOneSet(final boolean hasPattern, final boolean hasMinMaxValues, final boolean hasEnumeration){
			return (hasPattern && hasMinMaxValues || hasPattern && hasEnumeration || hasMinMaxValues && hasEnumeration);
		}

		private void validateDefaultValue(final Field field, final ConfigurationField binding) throws AnnotationException{
			final Class<?> fieldType = field.getType();
			final Class<? extends Enum<?>> enumeration = binding.enumeration();
			final String defaultValue = binding.defaultValue();

			if(!defaultValue.isEmpty()){
				//defaultValue compatible with variable type
				if(enumeration == NullEnum.class && JavaHelper.getValue(fieldType, defaultValue) == null)
					throw AnnotationException.create("Incompatible enum in {}, found {}, expected {}",
						ConfigurationField.class.getSimpleName(), defaultValue.getClass().getSimpleName(), fieldType.toString());
			}
			//if default value is not present, then field type must be an object
			else if(ParserDataType.isPrimitive(fieldType))
				throw AnnotationException.create("Default must be present for primitive type in {}, found {}, expected {}",
					ConfigurationField.class.getSimpleName(), fieldType.getSimpleName(),
					ParserDataType.toObjectiveTypeOrSelf(fieldType).getSimpleName());
		}

		private void validateEnumeration(final Field field, final ConfigurationField binding) throws AnnotationException{
			final Class<?> fieldType = field.getType();
			final boolean isFieldArray = fieldType.isArray();
			final Class<? extends Enum<?>> enumeration = binding.enumeration();
			final String defaultValue = binding.defaultValue();

			if(enumeration != NullEnum.class){
				//enumeration can be encoded
				if(!ConfigurationEnum.class.isAssignableFrom(enumeration))
					throw AnnotationException.create("Enum must implement ConfigurationEnum.class in {} in field {}",
						ConfigurationField.class.getSimpleName(), field.getName());

				//non-empty enumeration
				final Enum<?>[] enumConstants = enumeration.getEnumConstants();
				if(enumConstants.length == 0)
					throw AnnotationException.create("Empty enum in {} in field {}", ConfigurationField.class.getSimpleName(),
						field.getName());

				//enumeration compatible with variable type
				if(isFieldArray)
					validateEnumMultipleValues(field, fieldType, enumeration, enumConstants, defaultValue);
				else
					validateEnumerationMutuallyExclusive(fieldType, enumeration, enumConstants, defaultValue);
			}
		}

		private void validateEnumMultipleValues(final Field field, final Class<?> fieldType, final Class<? extends Enum<?>> enumeration,
				final Enum<?>[] enumConstants, final String defaultValue) throws AnnotationException{
			if(!fieldType.getComponentType().isAssignableFrom(enumeration))
				throw AnnotationException.create("Incompatible enum in {}; found {}, expected {}",
					ConfigurationField.class.getSimpleName(), enumeration.getSimpleName(), fieldType.toString());

			if(!defaultValue.isEmpty()){
				final String[] defaultValues = StringHelper.split(defaultValue, '|', -1);
				if(field.getType().isEnum() && defaultValues.length != 1)
					throw AnnotationException.create("Default value for mutually exclusive enumeration field in {} should be a value; found {}, expected one of {}",
						ConfigurationField.class.getSimpleName(), defaultValue, Arrays.toString(enumConstants));

				for(int i = 0; i < JavaHelper.lengthOrZero(defaultValues); i ++){
					final String dv = defaultValues[i];
					if(JavaHelper.extractEnum(enumConstants, dv) == null)
						throw AnnotationException.create("Default value not compatible with `enumeration` in {}; found {}, expected one of {}",
							ConfigurationField.class.getSimpleName(), dv, Arrays.toString(enumConstants));
				}
			}
		}

		private void validateEnumerationMutuallyExclusive(final Class<?> fieldType, final Class<? extends Enum<?>> enumeration,
				final Enum<?>[] enumConstants, final String defaultValue) throws AnnotationException{
			if(!fieldType.isAssignableFrom(enumeration))
				throw AnnotationException.create("Incompatible enum in {}; found {}, expected {}",
					ConfigurationField.class.getSimpleName(), enumeration.getSimpleName(), fieldType.toString());

			if(!defaultValue.isEmpty() && JavaHelper.extractEnum(enumConstants, defaultValue) == null)
				throw AnnotationException.create("Default value not compatible with `enumeration` in {}; found {}, expected one of {}",
					ConfigurationField.class.getSimpleName(), defaultValue, Arrays.toString(enumConstants));
		}
	},

	COMPOSITE_FIELD(CompositeConfigurationField.class){
		@Override
		void validate(final Field field, final Annotation annotation, final Version minMessageProtocol, final Version maxMessageProtocol)
				throws AnnotationException{
			final CompositeConfigurationField binding = (CompositeConfigurationField)annotation;

			if(StringHelper.isBlank(binding.shortDescription()))
				throw AnnotationException.create("Short description must be present");
			if(!String.class.isAssignableFrom(field.getType()))
				throw AnnotationException.create("Composite fields must have a string variable to be bounded to");

			final CompositeSubField[] fields = binding.value();
			if(fields.length == 0)
				throw AnnotationException.create("Composite fields must have at least one sub-field");
			ValidationHelper.assertValidCharset(binding.charset());

			validatePattern(field, binding);

			ValidationHelper.validateProtocol(binding.minProtocol(), binding.maxProtocol(), minMessageProtocol, maxMessageProtocol,
				CompositeConfigurationField.class);


			for(int i = 0; i < fields.length; i ++)
				SUB_FIELD.validate(field, fields[i], minMessageProtocol, maxMessageProtocol);
		}

		private void validatePattern(final Field field, final CompositeConfigurationField binding) throws AnnotationException{
			final String pattern = binding.pattern();

			//valid pattern
			if(!pattern.isEmpty()){
				try{
					//defaultValue compatible with field type
					if(!String.class.isAssignableFrom(field.getType()))
						throw AnnotationException.create("Data type not compatible with `pattern` in {}; found {}.class, expected String.class",
							ConfigurationField.class.getSimpleName(), field.getType());
				}
				catch(final AnnotationException ae){
					throw ae;
				}
				catch(final Exception e){
					throw AnnotationException.create("Invalid pattern in {} in field {}", ConfigurationField.class.getSimpleName(),
						field.getName(), e);
				}
			}
		}
	},

	SUB_FIELD(CompositeSubField.class){
		@Override
		void validate(final Field field, final Annotation annotation, final Version minMessageProtocol, final Version maxMessageProtocol)
				throws AnnotationException{
			final CompositeSubField binding = (CompositeSubField)annotation;

			if(StringHelper.isBlank(binding.shortDescription()))
				throw AnnotationException.create("Short description must be present");

			validatePattern(field, binding);

			validateDefaultValue(field, binding);
		}

		private void validatePattern(final Field field, final CompositeSubField binding) throws AnnotationException{
			final String pattern = binding.pattern();
			final String defaultValue = binding.defaultValue();

			//valid pattern
			if(!pattern.isEmpty()){
				try{
					final Pattern formatPattern = Pattern.compile(pattern);

					//defaultValue compatible with field type
					if(!String.class.isAssignableFrom(field.getType()))
						throw AnnotationException.create("Data type not compatible with `pattern` in {}; found {}.class, expected String.class",
							CompositeSubField.class.getSimpleName(), field.getType());
					//defaultValue compatible with pattern
					if(!defaultValue.isEmpty() && !formatPattern.matcher(defaultValue).matches())
						throw AnnotationException.create("Default value not compatible with `pattern` in {}; found {}, expected {}",
							CompositeSubField.class.getSimpleName(), defaultValue, pattern);
				}
				catch(final AnnotationException ae){
					throw ae;
				}
				catch(final Exception e){
					throw AnnotationException.create("Invalid pattern in {} in field {}", CompositeSubField.class.getSimpleName(),
						field.getName(), e);
				}
			}
		}

		private void validateDefaultValue(final Field field, final CompositeSubField binding) throws AnnotationException{
			final Class<?> fieldType = field.getType();
			final String defaultValue = binding.defaultValue();

			if(!defaultValue.isEmpty()){
				//defaultValue compatible with variable type
				if(JavaHelper.getValue(fieldType, defaultValue) == null)
					throw AnnotationException.create("Incompatible enum in {}, found {}, expected {}",
						CompositeSubField.class.getSimpleName(), defaultValue.getClass().getSimpleName(), fieldType.toString());
			}
			//if default value is not present, then field type must be an object
			else if(ParserDataType.isPrimitive(fieldType))
				throw AnnotationException.create("Default must be present for primitive type in {}, found {}, expected {}",
					ConfigurationField.class.getSimpleName(), fieldType.getSimpleName(),
					ParserDataType.toObjectiveTypeOrSelf(fieldType).getSimpleName());
		}
	},

	ALTERNATIVE_FIELDS(AlternativeConfigurationField.class){
		@Override
		void validate(final Field field, final Annotation annotation, final Version minMessageProtocol, final Version maxMessageProtocol)
			throws AnnotationException{
			final AlternativeConfigurationField binding = (AlternativeConfigurationField)annotation;

			if(StringHelper.isBlank(binding.shortDescription()))
				throw AnnotationException.create("Short description must be present");
			if(binding.enumeration() == NullEnum.class && field.getType().isEnum())
				throw AnnotationException.create("Unnecessary mutually exclusive field in a non-enumeration field");

			validateMinimumParameters(field, binding);

			validateEnumeration(field, binding);

			ValidationHelper.validateProtocol(binding.minProtocol(), binding.maxProtocol(), minMessageProtocol, maxMessageProtocol,
				AlternativeConfigurationField.class);

			final AlternativeSubField[] alternatives = binding.value();
			for(int i = 0; i < JavaHelper.lengthOrZero(alternatives); i ++)
				ValidationHelper.validateProtocol(alternatives[i].minProtocol(), alternatives[i].maxProtocol(), minMessageProtocol,
					maxMessageProtocol, AlternativeSubField.class);
		}

		private void validateMinimumParameters(final Field field, final AlternativeConfigurationField binding) throws AnnotationException{
			final Class<?> fieldType = field.getType();
			final boolean isFieldArray = fieldType.isArray();
			final Class<? extends Enum<?>> enumeration = binding.enumeration();

			if(isFieldArray && enumeration == NullEnum.class)
				throw AnnotationException.create("Array field should have `enumeration`");
		}

		private void validateEnumeration(final Field field, final AlternativeConfigurationField binding) throws AnnotationException{
			final Class<? extends Enum<?>> enumeration = binding.enumeration();

			if(enumeration != NullEnum.class){
				//enumeration can be encoded
				if(!ConfigurationEnum.class.isAssignableFrom(enumeration))
					throw AnnotationException.create("Enum must implement ConfigurationEnum.class in {} in field {}",
						ConfigurationField.class.getSimpleName(), field.getName());

				//non-empty enumeration
				final Enum<?>[] enumConstants = enumeration.getEnumConstants();
				if(enumConstants.length == 0)
					throw AnnotationException.create("Empty enum in {} in field {}", ConfigurationField.class.getSimpleName(),
						field.getName());
			}
		}
	},

	ALTERNATIVE_FIELD(AlternativeSubField.class){
		@Override
		void validate(final Field field, final Annotation annotation, final Version minMessageProtocol, final Version maxMessageProtocol)
			throws AnnotationException{
			final AlternativeSubField binding = (AlternativeSubField)annotation;

			if(String.class.isAssignableFrom(field.getType()))
				ValidationHelper.assertValidCharset(binding.charset());
			ValidationHelper.validateRadix(binding.radix());

			validateMinimumParameters(binding);

			final String pattern = binding.pattern();
			final String minValue = binding.minValue();
			final String maxValue = binding.maxValue();
			final String defaultValue = binding.defaultValue();
			ValidationHelper.validatePattern(field, pattern, minValue, maxValue, defaultValue);

			ValidationHelper.validateMinMaxValues(field, minValue, maxValue, defaultValue);

			ValidationHelper.validateProtocol(binding.minProtocol(), binding.maxProtocol(), minMessageProtocol, maxMessageProtocol,
				AlternativeSubField.class);
		}

		private void validateMinimumParameters(final AlternativeSubField binding) throws AnnotationException{
			final String pattern = binding.pattern();
			final String minValue = binding.minValue();
			final String maxValue = binding.maxValue();

			//one only of `pattern`, `minValue`/`maxValue`, and `enumeration` should be set:
			int set = 0;
			if(!pattern.isEmpty())
				set ++;
			if(!minValue.isEmpty() || !maxValue.isEmpty())
				set ++;
			if(set > 1)
				throw AnnotationException.create("Only one of `pattern`, `minValue`/`maxValue`, or `enumeration` should be used in {}",
					ConfigurationField.class.getSimpleName());
		}
	};


	private static final Map<Class<? extends Annotation>, ConfigurationAnnotationValidator> VALIDATORS;
	static{
		final ConfigurationAnnotationValidator[] values = values();
		final Map<Class<? extends Annotation>, ConfigurationAnnotationValidator> validators = new HashMap<>(values.length);
		for(final ConfigurationAnnotationValidator validator : values)
			validators.put(validator.annotationType, validator);
		VALIDATORS = Collections.unmodifiableMap(validators);
	}

	private final Class<? extends Annotation> annotationType;


	ConfigurationAnnotationValidator(final Class<? extends Annotation> type){
		annotationType = type;
	}

	static ConfigurationAnnotationValidator fromAnnotation(final Annotation annotation){
		return VALIDATORS.get(annotation.annotationType());
	}

	abstract void validate(final Field field, final Annotation annotation, final Version minMessageProtocol,
		final Version maxMessageProtocol) throws AnnotationException;

}
