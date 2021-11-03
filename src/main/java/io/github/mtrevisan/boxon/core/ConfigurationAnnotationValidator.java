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
package io.github.mtrevisan.boxon.core;

import io.github.mtrevisan.boxon.annotations.configurations.CompositeConfigurationField;
import io.github.mtrevisan.boxon.annotations.configurations.ConfigurationEnum;
import io.github.mtrevisan.boxon.annotations.configurations.ConfigurationField;
import io.github.mtrevisan.boxon.annotations.configurations.ConfigurationHeader;
import io.github.mtrevisan.boxon.annotations.configurations.ConfigurationSubField;
import io.github.mtrevisan.boxon.annotations.configurations.NullEnum;
import io.github.mtrevisan.boxon.exceptions.AnnotationException;
import io.github.mtrevisan.boxon.internal.JavaHelper;
import io.github.mtrevisan.boxon.internal.semanticversioning.Version;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;


enum ConfigurationAnnotationValidator{

	HEADER(ConfigurationHeader.class){
		@Override
		void validate(final Field field, final Annotation annotation) throws AnnotationException{
			final ConfigurationHeader binding = (ConfigurationHeader)annotation;

			if(JavaHelper.isBlank(binding.shortDescription()))
				throw AnnotationException.create("Short description must be present");

			//check minimum/maximum protocol
			final String minProtocol = binding.minProtocol();
			final String maxProtocol = binding.minProtocol();
			if(!minProtocol.isEmpty() && !maxProtocol.isEmpty()){
				final Version min = new Version(minProtocol);
				final Version max = new Version(maxProtocol);
				if(max.isLessThan(min))
					throw AnnotationException.create("Maximum protocol should be after minimum protocol in {}; min is {}, max is {}",
						ConfigurationField.class.getSimpleName(), minProtocol, maxProtocol);
			}
		}
	},

	FIELD(ConfigurationField.class){
		@Override
		void validate(final Field field, final Annotation annotation) throws AnnotationException{
			final ConfigurationField binding = (ConfigurationField)annotation;

			if(JavaHelper.isBlank(binding.shortDescription()))
				throw AnnotationException.create("Short description must be present");
			if(binding.enumeration() == NullEnum.class && field.getType().isEnum())
				throw AnnotationException.create("Unnecessary mutually exclusive field in a non-enumeration field");
			if(String.class.isAssignableFrom(field.getType()))
				CodecHelper.assertValidCharset(binding.charset());
			if(binding.radix() < Character.MIN_RADIX || binding.radix() > Character.MAX_RADIX)
				throw AnnotationException.create("Radix must be in [" + Character.MIN_RADIX + ", " + Character.MAX_RADIX + "]");

			validateMinimumParameters(field, binding);

			validatePattern(field, binding);

			validateDefaultValue(field, binding);

			validateEnumeration(field, binding);

			validateMinMaxValues(field, binding);

			validateProtocol(binding.minProtocol(), binding.maxProtocol(), ConfigurationField.class);
		}

		private void validateMinimumParameters(final Field field, final ConfigurationField binding) throws AnnotationException{
			final Class<?> fieldType = field.getType();
			final boolean isFieldArray = fieldType.isArray();
			final String pattern = binding.pattern();
			final String minValue = binding.minValue();
			final String maxValue = binding.maxValue();
			final Class<? extends Enum<?>> enumeration = binding.enumeration();

			//one only of `pattern`, `minValue`/`maxValue`, and `enumeration` should be set:
			int set = 0;
			if(!pattern.isEmpty())
				set ++;
			if(!minValue.isEmpty() || !maxValue.isEmpty())
				set ++;
			if(enumeration != NullEnum.class){
				set ++;

				if(!pattern.isEmpty() || !minValue.isEmpty() || !maxValue.isEmpty())
					throw AnnotationException.create("Enumeration cannot have `pattern` or `minValue`/`maxValue`");
			}
			if(isFieldArray && (set != 1 || enumeration == NullEnum.class))
				throw AnnotationException.create("Array field cannot have `pattern` or `minValue`/`maxValue`");
			if(set > 1)
				throw AnnotationException.create("Only one of `pattern`, `minValue`/`maxValue`, or `enumeration` should be used in {}",
					ConfigurationField.class.getSimpleName());
		}

		private void validatePattern(final Field field, final ConfigurationField binding) throws AnnotationException{
			final String pattern = binding.pattern();
			final String minValue = binding.minValue();
			final String maxValue = binding.maxValue();
			final String defaultValue = binding.defaultValue();

			//valid pattern
			if(!pattern.isEmpty()){
				try{
					final Pattern formatPattern = Pattern.compile(pattern);

					//defaultValue compatible with field type
					if(!String.class.isAssignableFrom(field.getType()))
						throw AnnotationException.create("Data type not compatible with `pattern` in {}; found {}.class, expected String.class",
							ConfigurationField.class.getSimpleName(), field.getType());
					//defaultValue compatible with pattern
					if(!defaultValue.isEmpty() && !formatPattern.matcher(defaultValue).matches())
						throw AnnotationException.create("Default value not compatible with `pattern` in {}; found {}, expected {}",
							ConfigurationField.class.getSimpleName(), defaultValue, pattern);
					//minValue compatible with pattern
					if(!minValue.isEmpty() && !formatPattern.matcher(minValue).matches())
						throw AnnotationException.create("Minimum value not compatible with `pattern` in {}; found {}, expected {}",
							ConfigurationField.class.getSimpleName(), minValue, pattern);
					//maxValue compatible with pattern
					if(!maxValue.isEmpty() && !formatPattern.matcher(maxValue).matches())
						throw AnnotationException.create("Maximum value not compatible with `pattern` in {}; found {}, expected {}",
							ConfigurationField.class.getSimpleName(), maxValue, pattern);
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

		private void validateDefaultValue(final Field field, final ConfigurationField binding) throws AnnotationException{
			final Class<?> fieldType = field.getType();
			final Class<? extends Enum<?>> enumeration = binding.enumeration();
			final String defaultValue = binding.defaultValue();

			if(!defaultValue.isEmpty())
				//defaultValue compatible with variable type
				if(enumeration == NullEnum.class && JavaHelper.getValue(fieldType, defaultValue) == null)
					throw AnnotationException.create("Incompatible enum in {}, found {}, expected {}",
						ConfigurationField.class.getSimpleName(), defaultValue.getClass().getSimpleName(), fieldType.toString());
		}

		private void validateMinMaxValues(final Field field, final ConfigurationField binding) throws AnnotationException{
			final Class<?> fieldType = field.getType();
			final String minValue = binding.minValue();
			final String maxValue = binding.maxValue();
			final String defaultValue = binding.defaultValue();

			if(!minValue.isEmpty() || !maxValue.isEmpty()){
				Object min = null;
				Object max = null;
				final Object def = (!defaultValue.isEmpty()? JavaHelper.getValue(fieldType, defaultValue): null);
				if(!minValue.isEmpty()){
					min = JavaHelper.getValue(fieldType, minValue);
					//minValue compatible with variable type
					if(min == null)
						throw AnnotationException.create("Incompatible minimum value in {}; found {}, expected {}",
							ConfigurationField.class.getSimpleName(), minValue.getClass().getSimpleName(), fieldType.toString());

					if(JavaHelper.isNumeric(defaultValue) && def != null && ((Number)def).doubleValue() < ((Number)min).doubleValue())
						//defaultValue compatible with minValue
						throw AnnotationException.create("Default value incompatible with minimum value in {}; found {}, expected greater than or equals to {}",
							ConfigurationField.class.getSimpleName(), defaultValue, minValue.getClass().getSimpleName());
				}
				if(!maxValue.isEmpty()){
					max = JavaHelper.getValue(fieldType, maxValue);
					//maxValue compatible with variable type
					if(max == null)
						throw AnnotationException.create("Incompatible maximum value in {}; found {}, expected {}",
							ConfigurationField.class.getSimpleName(), maxValue.getClass().getSimpleName(), fieldType.toString());

					if(JavaHelper.isNumeric(defaultValue) && def != null && ((Number)def).doubleValue() > ((Number)max).doubleValue())
						//defaultValue compatible with maxValue
						throw AnnotationException.create("Default value incompatible with maximum value in {}; found {}, expected less than or equals to {}",
							ConfigurationField.class.getSimpleName(), defaultValue, maxValue.getClass().getSimpleName());
				}

				if(def != null && min != null && max != null && ((Number)min).doubleValue() > ((Number)max).doubleValue())
					//maxValue after or equal to minValue
					throw AnnotationException.create("Minimum value should be less than or equal to maximum value in {}; found {}, expected greater than or equals to {}",
						ConfigurationField.class.getSimpleName(), defaultValue, minValue.getClass().getSimpleName());
			}
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
				if(isFieldArray){
					if(!fieldType.getComponentType().isAssignableFrom(enumeration))
						throw AnnotationException.create("Incompatible enum in {}; found {}, expected {}",
							ConfigurationField.class.getSimpleName(), enumeration.getSimpleName(), fieldType.toString());

					if(!defaultValue.isEmpty()){
						final String[] defaultValues = JavaHelper.split(defaultValue, "|", -1);
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
				else{
					if(!fieldType.isAssignableFrom(enumeration))
						throw AnnotationException.create("Incompatible enum in {}; found {}, expected {}",
							ConfigurationField.class.getSimpleName(), enumeration.getSimpleName(), fieldType.toString());

					if(!defaultValue.isEmpty() && JavaHelper.extractEnum(enumConstants, defaultValue) == null)
						throw AnnotationException.create("Default value not compatible with `enumeration` in {}; found {}, expected one of {}",
							ConfigurationField.class.getSimpleName(), defaultValue, Arrays.toString(enumConstants));
				}
			}
		}
	},

	COMPOSITE_FIELD(CompositeConfigurationField.class){
		@Override
		void validate(final Field field, final Annotation annotation) throws AnnotationException{
			final CompositeConfigurationField binding = (CompositeConfigurationField)annotation;

			if(JavaHelper.isBlank(binding.shortDescription()))
				throw AnnotationException.create("Short description must be present");
			if(!String.class.isAssignableFrom(field.getType()))
				throw AnnotationException.create("Composite fields must have a string variable to be bounded to");

			final ConfigurationSubField[] fields = binding.value();
			if(fields.length == 0)
				throw AnnotationException.create("Composite fields must have at least one sub-field");
			CodecHelper.assertValidCharset(binding.charset());

			validatePattern(field, binding);

			validateProtocol(binding.minProtocol(), binding.maxProtocol(), CompositeConfigurationField.class);


			for(int i = 0; i < fields.length; i ++)
				SUB_FIELD.validate(field, fields[i]);
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

	SUB_FIELD(ConfigurationSubField.class){
		@Override
		void validate(final Field field, final Annotation annotation) throws AnnotationException{
			final ConfigurationSubField binding = (ConfigurationSubField)annotation;

			if(JavaHelper.isBlank(binding.shortDescription()))
				throw AnnotationException.create("Short description must be present");

			validatePattern(field, binding);

			validateDefaultValue(field, binding);
		}

		private void validatePattern(final Field field, final ConfigurationSubField binding) throws AnnotationException{
			final String pattern = binding.pattern();
			final String defaultValue = binding.defaultValue();

			//valid pattern
			if(!pattern.isEmpty()){
				try{
					final Pattern formatPattern = Pattern.compile(pattern);

					//defaultValue compatible with field type
					if(!String.class.isAssignableFrom(field.getType()))
						throw AnnotationException.create("Data type not compatible with `pattern` in {}; found {}.class, expected String.class",
							ConfigurationSubField.class.getSimpleName(), field.getType());
					//defaultValue compatible with pattern
					if(!defaultValue.isEmpty() && !formatPattern.matcher(defaultValue).matches())
						throw AnnotationException.create("Default value not compatible with `pattern` in {}; found {}, expected {}",
							ConfigurationSubField.class.getSimpleName(), defaultValue, pattern);
				}
				catch(final AnnotationException ae){
					throw ae;
				}
				catch(final Exception e){
					throw AnnotationException.create("Invalid pattern in {} in field {}", ConfigurationSubField.class.getSimpleName(),
						field.getName(), e);
				}
			}
		}

		private void validateDefaultValue(final Field field, final ConfigurationSubField binding) throws AnnotationException{
			final Class<?> fieldType = field.getType();
			final String defaultValue = binding.defaultValue();

			if(!defaultValue.isEmpty())
				//defaultValue compatible with variable type
				if(JavaHelper.getValue(fieldType, defaultValue) == null)
					throw AnnotationException.create("Incompatible enum in {}, found {}, expected {}",
						ConfigurationSubField.class.getSimpleName(), defaultValue.getClass().getSimpleName(), fieldType.toString());
		}
	};


	private static final Map<Class<? extends Annotation>, ConfigurationAnnotationValidator> VALIDATORS = new HashMap<>(4);
	static{
		for(final ConfigurationAnnotationValidator validator : values())
			VALIDATORS.put(validator.annotationType, validator);
	}

	private final Class<? extends Annotation> annotationType;


	ConfigurationAnnotationValidator(final Class<? extends Annotation> type){
		annotationType = type;
	}

	static ConfigurationAnnotationValidator fromAnnotation(final Annotation annotation){
		return VALIDATORS.get(annotation.annotationType());
	}

	abstract void validate(final Field field, final Annotation annotation) throws AnnotationException;

	private static void validateProtocol(final String minProtocol, final String maxProtocol, final Class<? extends Annotation> binding)
			throws AnnotationException{
		if(!minProtocol.isEmpty() || !maxProtocol.isEmpty()){
			//minProtocol/maxProtocol are valid
			Version minimum = null;
			if(!minProtocol.isEmpty()){
				try{
					minimum = new Version(minProtocol);
				}
				catch(final IllegalArgumentException iae){
					throw AnnotationException.create(iae, "Invalid minimum protocol version in {}; found {}",
						binding.getSimpleName(), minProtocol);
				}
			}
			Version maximum = null;
			if(!maxProtocol.isEmpty()){
				try{
					maximum = new Version(maxProtocol);
				}
				catch(final IllegalArgumentException iae){
					throw AnnotationException.create(iae, "Invalid maximum protocol version in {}; found {}",
						binding.getSimpleName(), maxProtocol);
				}
			}
			//maxProtocol after or equal to minProtocol
			if(minimum != null && maximum != null && maximum.isLessThan(maximum))
				throw AnnotationException.create("Minimum protocol version is greater than maximum protocol version in {}; found {}",
					binding.getSimpleName(), maxProtocol);
		}
	}

}
