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

import io.github.mtrevisan.boxon.annotations.configurations.AlternativeConfigurationField;
import io.github.mtrevisan.boxon.annotations.configurations.AlternativeSubField;
import io.github.mtrevisan.boxon.annotations.configurations.CompositeConfigurationField;
import io.github.mtrevisan.boxon.annotations.configurations.CompositeSubField;
import io.github.mtrevisan.boxon.annotations.configurations.ConfigurationField;
import io.github.mtrevisan.boxon.annotations.configurations.ConfigurationHeader;
import io.github.mtrevisan.boxon.codecs.managers.configuration.ConfigFieldData;
import io.github.mtrevisan.boxon.codecs.managers.configuration.ConfigFieldDataFactory;
import io.github.mtrevisan.boxon.exceptions.AnnotationException;
import io.github.mtrevisan.boxon.exceptions.CodecException;
import io.github.mtrevisan.boxon.external.semanticversioning.Version;
import io.github.mtrevisan.boxon.internal.JavaHelper;
import io.github.mtrevisan.boxon.internal.StringHelper;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;


enum ConfigurationAnnotationValidator{

	HEADER(ConfigurationHeader.class){
		@Override
		void validate(final Field field, final Annotation annotation, final Version minProtocolVersion, final Version maxProtocolVersion)
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
		void validate(final Field field, final Annotation annotation, final Version minProtocolVersion, final Version maxProtocolVersion)
				throws AnnotationException, CodecException{
			final ConfigFieldData<ConfigurationField> configData = ConfigFieldDataFactory.buildData(field, (ConfigurationField)annotation);

			final ConfigurationField binding = (ConfigurationField)annotation;

			if(StringHelper.isBlank(binding.shortDescription()))
				throw AnnotationException.create("Short description must be present");
			if(!configData.hasEnumeration() && field.getType().isEnum())
				throw AnnotationException.create("Unnecessary mutually exclusive field in a non-enumeration field");
			if(String.class.isAssignableFrom(field.getType()))
				ValidationHelper.assertValidCharset(configData.charset);
			ValidationHelper.validateRadix(configData.radix);

			validateMinimumParameters(configData);

			ValidationHelper.validatePattern(configData);

			ValidationHelper.validateDefaultValue(configData);

			ValidationHelper.validateEnumeration(configData);

			ValidationHelper.validateMinMaxValues(configData);

			ValidationHelper.validateProtocol(configData, minProtocolVersion, maxProtocolVersion);
		}

		private void validateMinimumParameters(final ConfigFieldData<ConfigurationField> field) throws AnnotationException{
			//one only of `pattern`, `minValue`/`maxValue`, and `enumeration` should be set:
			final boolean hasPattern = !field.pattern.isEmpty();
			final boolean hasMinMaxValues = (!field.minValue.isEmpty() || !field.maxValue.isEmpty());
			if(moreThanOneSet(hasPattern, hasMinMaxValues, field.hasEnumeration()))
				throw AnnotationException.create("Only one of `pattern`, `minValue`/`maxValue`, or `enumeration` should be used in {}",
					ConfigurationField.class.getSimpleName());

			final Class<?> fieldType = field.getFieldType();
			if(fieldType.isArray() && !field.hasEnumeration())
				throw AnnotationException.create("Array field should have `enumeration`");
		}

		private boolean moreThanOneSet(final boolean hasPattern, final boolean hasMinMaxValues, final boolean hasEnumeration){
			return (hasPattern && hasMinMaxValues
				|| hasPattern && hasEnumeration
				|| hasMinMaxValues && hasEnumeration);
		}
	},

	COMPOSITE_FIELD(CompositeConfigurationField.class){
		@Override
		void validate(final Field field, final Annotation annotation, final Version minProtocolVersion, final Version maxProtocolVersion)
				throws AnnotationException, CodecException{
			final ConfigFieldData<CompositeConfigurationField> configData = ConfigFieldDataFactory.buildData(field,
				(CompositeConfigurationField)annotation);

			final CompositeConfigurationField binding = (CompositeConfigurationField)annotation;

			if(StringHelper.isBlank(binding.shortDescription()))
				throw AnnotationException.create("Short description must be present");
			if(!String.class.isAssignableFrom(field.getType()))
				throw AnnotationException.create("Composite fields must have a string variable to be bounded to");

			final CompositeSubField[] fields = binding.value();
			if(fields.length == 0)
				throw AnnotationException.create("Composite fields must have at least one sub-field");
			ValidationHelper.assertValidCharset(configData.charset);

			ValidationHelper.validatePattern(configData);

			ValidationHelper.validateProtocol(configData, minProtocolVersion, maxProtocolVersion);


			for(int i = 0; i < fields.length; i ++)
				SUB_FIELD.validate(field, fields[i], minProtocolVersion, maxProtocolVersion);
		}
	},

	SUB_FIELD(CompositeSubField.class){
		@Override
		void validate(final Field field, final Annotation annotation, final Version minProtocolVersion, final Version maxProtocolVersion)
				throws AnnotationException, CodecException{
			final ConfigFieldData<CompositeSubField> configData = ConfigFieldDataFactory.buildData(field, (CompositeSubField)annotation);

			final CompositeSubField binding = (CompositeSubField)annotation;

			if(StringHelper.isBlank(binding.shortDescription()))
				throw AnnotationException.create("Short description must be present");

			ValidationHelper.validatePattern(configData);

			ValidationHelper.validateDefaultValue(configData);
		}
	},

	ALTERNATIVE_FIELDS(AlternativeConfigurationField.class){
		@Override
		void validate(final Field field, final Annotation annotation, final Version minProtocolVersion, final Version maxProtocolVersion)
				throws AnnotationException{
			final ConfigFieldData<AlternativeConfigurationField> configData = ConfigFieldDataFactory.buildData(field,
				(AlternativeConfigurationField)annotation);

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
				final ConfigFieldData<AlternativeSubField> alternativeConfigData = ConfigFieldDataFactory.buildData(field, alternatives[i]);
				ValidationHelper.validateProtocol(alternativeConfigData, minProtocolVersion, maxProtocolVersion);
			}
		}

		private void validateMinimumParameters(final Field field, final ConfigFieldData<AlternativeConfigurationField> configData)
				throws AnnotationException{
			final Class<?> fieldType = field.getType();
			if(fieldType.isArray() && !configData.hasEnumeration())
				throw AnnotationException.create("Array field should have `enumeration`");
		}
	},

	ALTERNATIVE_FIELD(AlternativeSubField.class){
		@Override
		void validate(final Field field, final Annotation annotation, final Version minProtocolVersion, final Version maxProtocolVersion)
				throws AnnotationException, CodecException{
			final ConfigFieldData<AlternativeSubField> configData = ConfigFieldDataFactory.buildData(field, (AlternativeSubField)annotation);

			final AlternativeSubField binding = (AlternativeSubField)annotation;

			if(String.class.isAssignableFrom(field.getType()))
				ValidationHelper.assertValidCharset(configData.charset);
			ValidationHelper.validateRadix(configData.radix);

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

	static ConfigurationAnnotationValidator fromAnnotationType(final Class<? extends Annotation> annotationType){
		return VALIDATORS.get(annotationType);
	}

	abstract void validate(final Field field, final Annotation annotation, final Version minProtocolVersion,
		final Version maxProtocolVersion) throws AnnotationException, CodecException;

}
