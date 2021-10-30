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

import io.github.mtrevisan.boxon.annotations.configurations.ConfigurationField;
import io.github.mtrevisan.boxon.annotations.configurations.NullEnum;
import io.github.mtrevisan.boxon.exceptions.AnnotationException;
import io.github.mtrevisan.boxon.internal.JavaHelper;
import io.github.mtrevisan.boxon.internal.ParserDataType;
import io.github.mtrevisan.boxon.internal.semanticversioning.Version;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;


enum ConfigurationAnnotationValidator{

	FIELD(ConfigurationField.class){
		@Override
		void validate(final Field field, final Annotation annotation) throws AnnotationException{
			final ConfigurationField binding = (ConfigurationField)annotation;

			validateMinimumParameters(field, binding);

			validateFormat(field, binding);

			validateDefaultValue(field, binding);

			verifyEnumeration(field, binding);

			verifyMinMaxValues(field, binding);

			validateProtocol(binding);
		}

		private void validateMinimumParameters(final Field field, final ConfigurationField binding) throws AnnotationException{
			final Class<?> fieldType = field.getType();
			final boolean isFieldArray = fieldType.isArray();
			final String format = binding.format();
			final String minValue = binding.minValue();
			final String maxValue = binding.maxValue();
			final Class<? extends Enum<?>> enumeration = binding.enumeration();

			//one only of `format`, `minValue`/`maxValue`, and `enumeration` should be set:
			int set = 0;
			if(!format.isEmpty())
				set ++;
			if(!minValue.isEmpty() || !maxValue.isEmpty())
				set ++;
			if(enumeration != NullEnum.class){
				set ++;

				if(!format.isEmpty() || !minValue.isEmpty() || !maxValue.isEmpty())
					throw AnnotationException.create("Enumeration cannot have `format` or `minValue`/`maxValue`");
			}
			if(isFieldArray && (set != 1 || enumeration == NullEnum.class))
				throw AnnotationException.create("Array field cannot have `format` or `minValue`/`maxValue`");
			if(binding.writable()){
				if(set == 0)
					throw AnnotationException.create("One of `format`, `minValue`/`maxValue`, or `enumeration` should be used in {}",
						ConfigurationField.class.getSimpleName());
				if(set != 1)
					throw AnnotationException.create("Only one of `format`, `minValue`/`maxValue`, or `enumeration` should be used in {}",
						ConfigurationField.class.getSimpleName());
			}
		}

		private void validateFormat(final Field field, final ConfigurationField binding) throws AnnotationException{
			final String format = binding.format();
			final String minValue = binding.minValue();
			final String maxValue = binding.maxValue();
			final String defaultValue = binding.defaultValue();

			//valid format
			if(!format.isEmpty()){
				try{
					final Pattern formatPattern = Pattern.compile(format);

					//defaultValue compatible with format
					if(!defaultValue.isEmpty() && !formatPattern.matcher(defaultValue).matches())
						throw AnnotationException.create("Default value not compatible with `format` in {}; found {}, expected {}",
							ConfigurationField.class.getSimpleName(), defaultValue, format);
					//minValue compatible with format
					if(!minValue.isEmpty() && !formatPattern.matcher(minValue).matches())
						throw AnnotationException.create("Minimum value not compatible with `format` in {}; found {}, expected {}",
							ConfigurationField.class.getSimpleName(), minValue, format);
					//maxValue compatible with format
					if(!maxValue.isEmpty() && !formatPattern.matcher(maxValue).matches())
						throw AnnotationException.create("Maximum value not compatible with `format` in {}; found {}, expected {}",
							ConfigurationField.class.getSimpleName(), maxValue, format);
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
				if(enumeration == NullEnum.class && getAssignableFrom(fieldType, defaultValue) == null)
					throw AnnotationException.create("Incompatible enum in {}, found {}, expected {}",
						ConfigurationField.class.getSimpleName(), defaultValue.getClass().getSimpleName(), fieldType.toString());
		}

		private void verifyMinMaxValues(final Field field, final ConfigurationField binding) throws AnnotationException{
			final Class<?> fieldType = field.getType();
			final boolean isFieldArray = fieldType.isArray();
			final String minValue = binding.minValue();
			final String maxValue = binding.maxValue();
			final Class<? extends Enum<?>> enumeration = binding.enumeration();
			final String defaultValue = binding.defaultValue();

			if(!minValue.isEmpty() || !maxValue.isEmpty()){
				Object min = null;
				Object max = null;
				Object def = (!defaultValue.isEmpty()? getAssignableFrom(fieldType, defaultValue): null);
				if(!minValue.isEmpty()){
					min = getAssignableFrom(fieldType, minValue);
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
					max = getAssignableFrom(fieldType, maxValue);
					//maxValue compatible with variable type
					if(max == null)
						throw AnnotationException.create("Incompatible maximum value in {}; found {}, expected {}",
							ConfigurationField.class.getSimpleName(), maxValue.getClass().getSimpleName(), fieldType.toString());

					if(JavaHelper.isNumeric(defaultValue) && def != null && ((Number)def).doubleValue() > ((Number)max).doubleValue())
						//defaultValue compatible with maxValue
						throw AnnotationException.create("Default value incompatible with maximum value in {}; found {}, expected less than or equals to {}",
							ConfigurationField.class.getSimpleName(), defaultValue, maxValue.getClass().getSimpleName());
				}

				if(JavaHelper.isNumeric(minValue) && def != null && ((Number)min).doubleValue() > ((Number)max).doubleValue())
					//maxValue after or equal to minValue
					throw AnnotationException.create("Minimum value should be less than or equal to maximum value in {}; found {}, expected greater than or equals to {}",
						ConfigurationField.class.getSimpleName(), defaultValue, minValue.getClass().getSimpleName());
			}
		}

		private void validateProtocol(final ConfigurationField binding) throws AnnotationException{
			final String minProtocol = binding.minProtocol();
			final String maxProtocol = binding.minProtocol();
			if(!minProtocol.isEmpty() || !maxProtocol.isEmpty()){
				//minProtocol/maxProtocol are valid
				Version minimum = null;
				if(!minProtocol.isEmpty()){
					try{
						minimum = new Version(minProtocol);
					}
					catch(final IllegalArgumentException iae){
						throw AnnotationException.create("Invalid minimum protocol version in {}; found {}",
							ConfigurationField.class.getSimpleName(), minProtocol);
					}
				}
				Version maximum = null;
				if(!maxProtocol.isEmpty()){
					try{
						maximum = new Version(maxProtocol);
					}
					catch(final IllegalArgumentException iae){
						throw AnnotationException.create("Invalid maximum protocol version in {}; found {}",
							ConfigurationField.class.getSimpleName(), maxProtocol);
					}
				}
				//maxProtocol after or equal to minProtocol
				if(minimum != null && maximum != null && maximum.isLessThan(maximum))
					throw AnnotationException.create("Minimum protocol version is greater than maximum protocol version in {}; found {}",
						ConfigurationField.class.getSimpleName(), maxProtocol);
			}
		}

		private void verifyEnumeration(final Field field, final ConfigurationField binding) throws AnnotationException{
			final Class<?> fieldType = field.getType();
			final boolean isFieldArray = fieldType.isArray();
			final Class<? extends Enum<?>> enumeration = binding.enumeration();
			final String defaultValue = binding.defaultValue();

			if(enumeration != NullEnum.class){
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
						for(int i = 0; i < JavaHelper.lengthOrZero(defaultValues); i ++){
							final String dv = defaultValues[i];
							if(!belongsToEnum(enumConstants, dv))
								throw AnnotationException.create("Default value not compatible with `enumeration` in {}; found {}, expected one of {}",
									ConfigurationField.class.getSimpleName(), dv, Arrays.toString(enumConstants));
						}
					}
				}
				else{
					if(!fieldType.isAssignableFrom(enumeration))
						throw AnnotationException.create("Incompatible enum in {}; found {}, expected {}",
							ConfigurationField.class.getSimpleName(), enumeration.getSimpleName(), fieldType.toString());

					if(!defaultValue.isEmpty() && !belongsToEnum(enumConstants, defaultValue))
						throw AnnotationException.create("Default value not compatible with `enumeration` in {}; found {}, expected one of {}",
							ConfigurationField.class.getSimpleName(), defaultValue, Arrays.toString(enumConstants));
				}
			}
		}

		private boolean belongsToEnum(final Enum<?>[] enumConstants, final String value){
			boolean found = false;
			for(int j = 0; ! found && j < enumConstants.length; j++)
				if(enumConstants[j].name().equals(value))
					found = true;
			return found;
		}

		private Object getAssignableFrom(final Class<?> fieldType, final String value){
			if(fieldType == String.class)
				return value;

			try{
				final Class<?> objectiveType = ParserDataType.toObjectiveTypeOrSelf(fieldType);
				final boolean hexadecimal = value.startsWith("0x");
				final boolean octal = (!hexadecimal && value.charAt(0) == '0');
				final Method method = (hexadecimal || octal
					? objectiveType.getDeclaredMethod("valueOf", String.class, int.class)
					: objectiveType.getDeclaredMethod("valueOf", String.class));
				final Object response;
				if(hexadecimal)
					response = method.invoke(null, value.substring(2), 16);
				else if(octal)
					response = method.invoke(null, value, 8);
				else
					response = method.invoke(null, value);
				return response;
			}
			catch(final NoSuchMethodException | IllegalAccessException | InvocationTargetException ignored){
				return null;
			}
		}
	};


	private static final Map<Class<? extends Annotation>, ConfigurationAnnotationValidator> VALIDATORS = new HashMap<>(1);
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

}
