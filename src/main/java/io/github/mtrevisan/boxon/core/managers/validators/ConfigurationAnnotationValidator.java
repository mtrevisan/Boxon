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
package io.github.mtrevisan.boxon.core.managers.validators;

import io.github.mtrevisan.boxon.annotations.configurations.AlternativeConfigurationField;
import io.github.mtrevisan.boxon.annotations.configurations.AlternativeSubField;
import io.github.mtrevisan.boxon.annotations.configurations.CompositeConfigurationField;
import io.github.mtrevisan.boxon.annotations.configurations.CompositeSubField;
import io.github.mtrevisan.boxon.annotations.configurations.ConfigurationField;
import io.github.mtrevisan.boxon.annotations.configurations.ConfigurationHeader;
import io.github.mtrevisan.boxon.core.managers.configuration.ConfigFieldData;
import io.github.mtrevisan.boxon.core.managers.configuration.ConfigFieldDataBuilder;
import io.github.mtrevisan.boxon.helpers.CharsetHelper;
import io.github.mtrevisan.boxon.exceptions.AnnotationException;
import io.github.mtrevisan.boxon.exceptions.CodecException;
import io.github.mtrevisan.boxon.semanticversioning.Version;
import io.github.mtrevisan.boxon.helpers.JavaHelper;
import io.github.mtrevisan.boxon.helpers.StringHelper;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;


/**
 * Container of all the validators of a configuration message.
 */
public enum ConfigurationAnnotationValidator{

	HEADER(ConfigurationHeader.class){
		@Override
		public void validate(final Field field, final Annotation annotation, final Version minProtocolVersion, final Version maxProtocolVersion)
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
		public void validate(final Field field, final Annotation annotation, final Version minProtocolVersion, final Version maxProtocolVersion)
				throws AnnotationException, CodecException{
			final ConfigFieldData configData = ConfigFieldDataBuilder.create(field, (ConfigurationField)annotation);

			final ConfigurationField binding = (ConfigurationField)annotation;

			if(StringHelper.isBlank(binding.shortDescription()))
				throw AnnotationException.create("Short description must be present");
			if(!configData.hasEnumeration() && field.getType().isEnum())
				throw AnnotationException.create("Unnecessary mutually exclusive field in a non-enumeration field");
			if(String.class.isAssignableFrom(field.getType()))
				validateCharset(configData.getCharset());
			ValidationHelper.validateRadix(configData.getRadix());

			validateMinimumParameters(configData);

			ValidationHelper.validatePattern(configData);

			ValidationHelper.validateDefaultValue(configData);

			ValidationHelper.validateEnumeration(configData);

			ValidationHelper.validateMinMaxValues(configData);

			ValidationHelper.validateProtocol(configData, minProtocolVersion, maxProtocolVersion);
		}

		private void validateMinimumParameters(final ConfigFieldData field) throws AnnotationException{
			//one only of `pattern`, `minValue`/`maxValue`, and `enumeration` should be set:
			final boolean hasPattern = !field.getPattern().isEmpty();
			final boolean hasMinMaxValues = (!field.getMinValue().isEmpty() || !field.getMaxValue().isEmpty());
			if(moreThanOneSet(hasPattern, hasMinMaxValues, field.hasEnumeration()))
				throw AnnotationException.create("Only one of `pattern`, `minValue`/`maxValue`, or `enumeration` should be used in {}",
					ConfigurationField.class.getSimpleName());

			final Class<?> fieldType = field.getFieldType();
			if(fieldType.isArray() && !field.hasEnumeration())
				throw AnnotationException.create("Array field should have `enumeration`");
		}

		private boolean moreThanOneSet(final boolean hasPattern, final boolean hasMinMaxValues, final boolean hasEnumeration){
			return (hasPattern && (hasMinMaxValues || hasEnumeration)
				|| hasMinMaxValues && hasEnumeration);
		}
	},

	COMPOSITE_FIELD(CompositeConfigurationField.class){
		@Override
		public void validate(final Field field, final Annotation annotation, final Version minProtocolVersion, final Version maxProtocolVersion)
				throws AnnotationException, CodecException{
			final ConfigFieldData configData = ConfigFieldDataBuilder.create(field, (CompositeConfigurationField)annotation);

			final CompositeConfigurationField binding = (CompositeConfigurationField)annotation;

			if(StringHelper.isBlank(binding.shortDescription()))
				throw AnnotationException.create("Short description must be present");
			if(!String.class.isAssignableFrom(field.getType()))
				throw AnnotationException.create("Composite fields must have a string variable to be bounded to");

			final CompositeSubField[] fields = binding.value();
			if(fields.length == 0)
				throw AnnotationException.create("Composite fields must have at least one sub-field");
			validateCharset(configData.getCharset());

			ValidationHelper.validatePattern(configData);

			ValidationHelper.validateProtocol(configData, minProtocolVersion, maxProtocolVersion);


			for(int i = 0; i < fields.length; i ++)
				SUB_FIELD.validate(field, fields[i], minProtocolVersion, maxProtocolVersion);
		}
	},

	SUB_FIELD(CompositeSubField.class){
		@Override
		public void validate(final Field field, final Annotation annotation, final Version minProtocolVersion, final Version maxProtocolVersion)
				throws AnnotationException, CodecException{
			final ConfigFieldData configData = ConfigFieldDataBuilder.create(field, (CompositeSubField)annotation);

			final CompositeSubField binding = (CompositeSubField)annotation;

			if(StringHelper.isBlank(binding.shortDescription()))
				throw AnnotationException.create("Short description must be present");

			ValidationHelper.validatePattern(configData);

			ValidationHelper.validateDefaultValue(configData);
		}
	},

	ALTERNATIVE_FIELDS(AlternativeConfigurationField.class){
		@Override
		public void validate(final Field field, final Annotation annotation, final Version minProtocolVersion, final Version maxProtocolVersion)
				throws AnnotationException{
			final ConfigFieldData configData = ConfigFieldDataBuilder.create(field, (AlternativeConfigurationField)annotation);

			final AlternativeConfigurationField binding = (AlternativeConfigurationField)annotation;

			if(StringHelper.isBlank(binding.shortDescription()))
				throw AnnotationException.create("Short description must be present");
			if(!configData.hasEnumeration() && field.getType().isEnum())
				throw AnnotationException.create("Unnecessary mutually exclusive field in a non-enumeration field");

			validateMinimumParameters(field, configData);

			ValidationHelper.validateEnumeration(configData);

			ValidationHelper.validateProtocol(configData, minProtocolVersion, maxProtocolVersion);

			final AlternativeSubField[] alternatives = binding.value();
			for(int i = 0; i < JavaHelper.lengthOrZero(alternatives); i ++){
				final ConfigFieldData alternativeConfigData = ConfigFieldDataBuilder.create(field, alternatives[i]);
				ValidationHelper.validateProtocol(alternativeConfigData, minProtocolVersion, maxProtocolVersion);
			}
		}

		private void validateMinimumParameters(final Field field, final ConfigFieldData configData) throws AnnotationException{
			final Class<?> fieldType = field.getType();
			if(fieldType.isArray() && !configData.hasEnumeration())
				throw AnnotationException.create("Array field should have `enumeration`");
		}
	},

	ALTERNATIVE_FIELD(AlternativeSubField.class){
		@Override
		public void validate(final Field field, final Annotation annotation, final Version minProtocolVersion, final Version maxProtocolVersion)
				throws AnnotationException, CodecException{
			final ConfigFieldData configData = ConfigFieldDataBuilder.create(field, (AlternativeSubField)annotation);

			final AlternativeSubField binding = (AlternativeSubField)annotation;

			if(String.class.isAssignableFrom(field.getType()))
				validateCharset(configData.getCharset());
			ValidationHelper.validateRadix(configData.getRadix());

			validateMinimumParameters(binding);

			ValidationHelper.validatePattern(configData);

			ValidationHelper.validateMinMaxValues(configData);

			ValidationHelper.validateProtocol(configData, minProtocolVersion, maxProtocolVersion);
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


	private static final ValueOf<ConfigurationAnnotationValidator, Class<? extends Annotation>> VALIDATORS
		= ValueOf.create(ConfigurationAnnotationValidator.class, validator -> validator.annotationType);


	private final Class<? extends Annotation> annotationType;


	ConfigurationAnnotationValidator(final Class<? extends Annotation> type){
		annotationType = type;
	}

	/**
	 * Get the validator for the given annotation.
	 *
	 * @param annotationType	The annotation class type.
	 * @return	The validator for the given annotation.
	 */
	public static ConfigurationAnnotationValidator fromAnnotationType(final Class<? extends Annotation> annotationType){
		return VALIDATORS.get(annotationType);
	}

	/**
	 * Validate field and annotation.
	 *
	 * @param field	The field associated to the annotation.
	 * @param annotation	The annotation.
	 * @param minProtocolVersion	The minimum protocol version.
	 * @param maxProtocolVersion	The maximum protocol version.
	 * @throws AnnotationException	If an error is detected.
	 * @throws CodecException	If an error was raised reading of interpreting the field value.
	 */
	public abstract void validate(final Field field, final Annotation annotation, final Version minProtocolVersion, final Version maxProtocolVersion) throws AnnotationException, CodecException;

	private static void validateCharset(final String charsetName) throws AnnotationException{
		try{
			CharsetHelper.assertValidCharset(charsetName);
		}
		catch(final IllegalArgumentException ignored){
			throw AnnotationException.create("Invalid charset: '{}'", charsetName);
		}
	}

}
