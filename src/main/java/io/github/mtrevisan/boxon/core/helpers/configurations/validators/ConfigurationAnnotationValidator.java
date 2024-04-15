/*
 * Copyright (c) 2020-2024 Mauro Trevisan
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
package io.github.mtrevisan.boxon.core.helpers.configurations.validators;

import io.github.mtrevisan.boxon.annotations.configurations.AlternativeConfigurationField;
import io.github.mtrevisan.boxon.annotations.configurations.AlternativeSubField;
import io.github.mtrevisan.boxon.annotations.configurations.CompositeConfigurationField;
import io.github.mtrevisan.boxon.annotations.configurations.CompositeSubField;
import io.github.mtrevisan.boxon.annotations.configurations.ConfigurationField;
import io.github.mtrevisan.boxon.annotations.configurations.ConfigurationHeader;
import io.github.mtrevisan.boxon.core.helpers.ValueOf;
import io.github.mtrevisan.boxon.exceptions.AnnotationException;
import io.github.mtrevisan.boxon.exceptions.CodecException;
import io.github.mtrevisan.boxon.helpers.CharsetHelper;
import io.github.mtrevisan.boxon.helpers.JavaHelper;
import io.github.mtrevisan.boxon.helpers.StringHelper;
import io.github.mtrevisan.boxon.semanticversioning.Version;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.nio.charset.UnsupportedCharsetException;


/**
 * Container of all the validators of a configuration message.
 */
public enum ConfigurationAnnotationValidator{

	HEADER(ConfigurationHeader.class){
		@Override
		public void validate(final Field field, final Annotation annotation, final Version minProtocolVersion,
				final Version maxProtocolVersion) throws AnnotationException{
			final ConfigurationHeader binding = (ConfigurationHeader)annotation;

			ensureShortDescriptionIsPresent(binding.shortDescription());

			//check minimum/maximum protocol
			final String minProtocol = binding.minProtocol();
			final String maxProtocol = binding.maxProtocol();
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
		public void validate(final Field field, final Annotation annotation, final Version minProtocolVersion,
				final Version maxProtocolVersion) throws AnnotationException, CodecException{
			final ConfigFieldData configData = ConfigFieldDataBuilder.create(field, (ConfigurationField)annotation);

			final ConfigurationField binding = (ConfigurationField)annotation;

			ensureShortDescriptionIsPresent(binding.shortDescription());
			final Class<?> fieldType = field.getType();
			validateEnumWithEnumeration(fieldType, configData);
			if(String.class.isAssignableFrom(fieldType))
				validateCharset(binding.charset());
			ValidationHelper.validateRadix(binding.radix());

			validateMinimumParameters(configData);

			final String defaultValue = binding.defaultValue();
			ValidationHelper.validatePattern(defaultValue, configData);

			ValidationHelper.validateDefaultValue(configData);

			EnumerationValidator.validateEnumeration(configData);

			MinMaxDataValidator.validateMinMaxDataValues(defaultValue, configData);

			ProtocolValidator.validateProtocol(minProtocolVersion, maxProtocolVersion, configData);
		}

		private static void validateMinimumParameters(final ConfigFieldData configData) throws AnnotationException{
			//one only of `pattern`, `minValue`/`maxValue`, and `enumeration` should be set:
			if(configData.hasIncompatibleInputs())
				throw AnnotationException.create("Only one of `pattern`, `minValue`/`maxValue`, or `enumeration` should be used in {}",
					ConfigurationField.class.getSimpleName());

			final Class<?> fieldType = configData.getFieldType();
			validateArrayWithEnumeration(fieldType, configData);
		}
	},

	COMPOSITE_FIELD(CompositeConfigurationField.class){
		@Override
		public void validate(final Field field, final Annotation annotation, final Version minProtocolVersion,
				final Version maxProtocolVersion) throws AnnotationException, CodecException{
			final ConfigFieldData configData = ConfigFieldDataBuilder.create(field, (CompositeConfigurationField)annotation);

			final CompositeConfigurationField binding = (CompositeConfigurationField)annotation;

			ensureShortDescriptionIsPresent(binding.shortDescription());
			if(!String.class.isAssignableFrom(field.getType()))
				throw AnnotationException.create("Composite fields must have a string variable to be bounded to");

			validateCharset(binding.charset());

			ValidationHelper.validatePattern(null, configData);

			ProtocolValidator.validateProtocol(minProtocolVersion, maxProtocolVersion, configData);

			final CompositeSubField[] subfields = binding.value();
			final int length = JavaHelper.sizeOrZero(subfields);
			if(length == 0)
				throw AnnotationException.create("Composite fields must have at least one sub-field");

			for(int i = 0; i < length; i ++)
				SUB_FIELD.validate(field, subfields[i], minProtocolVersion, maxProtocolVersion);
		}
	},

	SUB_FIELD(CompositeSubField.class){
		@Override
		public void validate(final Field field, final Annotation annotation, final Version minProtocolVersion,
				final Version maxProtocolVersion) throws AnnotationException, CodecException{
			final ConfigFieldData configData = ConfigFieldDataBuilder.create(field, (CompositeSubField)annotation);

			final CompositeSubField binding = (CompositeSubField)annotation;

			ensureShortDescriptionIsPresent(binding.shortDescription());

			ValidationHelper.validatePattern(binding.defaultValue(), configData);

			ValidationHelper.validateDefaultValue(configData);
		}
	},

	ALTERNATIVE_FIELDS(AlternativeConfigurationField.class){
		@Override
		public void validate(final Field field, final Annotation annotation, final Version minProtocolVersion,
				final Version maxProtocolVersion) throws AnnotationException, CodecException{
			final ConfigFieldData configData = ConfigFieldDataBuilder.create(field, (AlternativeConfigurationField)annotation);

			final AlternativeConfigurationField binding = (AlternativeConfigurationField)annotation;

			ensureShortDescriptionIsPresent(binding.shortDescription());
			final Class<?> fieldType = field.getType();
			validateEnumWithEnumeration(fieldType, configData);

			validateArrayWithEnumeration(fieldType, configData);

			EnumerationValidator.validateEnumeration(configData);

			ProtocolValidator.validateProtocol(minProtocolVersion, maxProtocolVersion, configData);

			final AlternativeSubField[] alternatives = binding.value();
			final int length = JavaHelper.sizeOrZero(alternatives);
			if(length == 0)
				throw AnnotationException.create("Alternative fields must have at least one sub-field");

			for(int i = 0; i < length; i ++)
				ALTERNATIVE_FIELD.validate(field, alternatives[i], minProtocolVersion, maxProtocolVersion);
		}
	},

	ALTERNATIVE_FIELD(AlternativeSubField.class){
		@Override
		public void validate(final Field field, final Annotation annotation, final Version minProtocolVersion,
				final Version maxProtocolVersion) throws AnnotationException{
			final ConfigFieldData configData = ConfigFieldDataBuilder.create(field, (AlternativeSubField)annotation);

			final AlternativeSubField binding = (AlternativeSubField)annotation;

			if(String.class.isAssignableFrom(field.getType()))
				validateCharset(binding.charset());
			ValidationHelper.validateRadix(binding.radix());

			validateMinimumParameters(binding);

			final String defaultValue = binding.defaultValue();
			ValidationHelper.validatePattern(defaultValue, configData);

			MinMaxDataValidator.validateMinMaxDataValues(defaultValue, configData);

			ProtocolValidator.validateProtocol(minProtocolVersion, maxProtocolVersion, configData);
		}

		private static void validateMinimumParameters(final AlternativeSubField binding) throws AnnotationException{
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
	 * @param minProtocolVersion	The minimum protocol version (should follow <a href="https://semver.org/">Semantic Versioning</a>).
	 * @param maxProtocolVersion	The maximum protocol version (should follow <a href="https://semver.org/">Semantic Versioning</a>).
	 * @throws AnnotationException	If an annotation error occurs.
	 * @throws CodecException	If an error was raised reading of interpreting the field value.
	 */
	public abstract void validate(Field field, Annotation annotation, Version minProtocolVersion, Version maxProtocolVersion)
		throws AnnotationException, CodecException;

	private static void ensureShortDescriptionIsPresent(final String shortDescription) throws AnnotationException{
		if(StringHelper.isBlank(shortDescription))
			throw AnnotationException.create("Short description must be present");
	}

	private static void validateCharset(final String charsetName) throws AnnotationException{
		try{
			CharsetHelper.lookup(charsetName);
		}
		catch(final UnsupportedCharsetException ignored){
			throw AnnotationException.create("Invalid charset: '{}'", charsetName);
		}
	}

	private static void validateArrayWithEnumeration(final Class<?> fieldType, final ConfigFieldData configData) throws AnnotationException{
		if(fieldType.isArray() && !configData.hasEnumeration())
			throw AnnotationException.create("Array field should have `enumeration`");
	}

	private static void validateEnumWithEnumeration(final Class<?> fieldType, final ConfigFieldData configData) throws AnnotationException{
		if(fieldType.isEnum() && !configData.hasEnumeration())
			throw AnnotationException.create("Mutually exclusive field in a non-enumeration field");
	}

}
