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
import io.github.mtrevisan.boxon.internal.ParserDataType;
import io.github.mtrevisan.boxon.internal.semanticversioning.Version;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;


enum ConfigurationAnnotationValidator{

	FIELD(ConfigurationField.class){
		@Override
		void validate(final Field field, final Annotation annotation) throws AnnotationException{
			final ConfigurationField binding = (ConfigurationField)annotation;

			final ParserDataType fieldType = ParserDataType.fromType(field.getType());
			final String format = binding.format();
			final String minValue = binding.minValue();
			final String maxValue = binding.maxValue();
			final Class<? extends Enum<?>> enumeration = binding.enumeration();
			final String defaultValue = binding.defaultValue();

			if(!defaultValue.isEmpty()){
				//TODO
				//defaultValue compatible with variable type
				if(!field.getType().isAssignableFrom(value))
					throw AnnotationException.create("Incompatible enum in {} in field {}, found {}, expected {}",
						ConfigurationField.class.getSimpleName(), field.getName(), value.getClass().getSimpleName(), fieldType.toString());
			}

			//one only of `format`, `minValue`/`maxValue`, and `enumeration` should be set:
			int set = 0;
			if(!format.isEmpty())
				set ++;
			if(!minValue.isEmpty() || !maxValue.isEmpty())
				set ++;
			if(enumeration != NullEnum.class)
				set ++;
			if(set == 0)
				throw AnnotationException.create("One of `format`, `minValue`/`maxValue`, or `enumeration` should be used in {} in field {}",
					ConfigurationField.class.getSimpleName(), field.getName());
			if(set != 1)
				throw AnnotationException.create("Only one of `format`, `minValue`/`maxValue`, or `enumeration` should be used in {} in field {}",
					ConfigurationField.class.getSimpleName(), field.getName());

			//valid format
			if(!format.isEmpty()){
				try{
					final Pattern formatPattern = Pattern.compile(format);

					//defaultValue compatible with format
					if(!defaultValue.isEmpty() && !formatPattern.matcher(defaultValue).matches())
						throw AnnotationException.create("Default value not compatible with `format` in {} in field {}; found {}, expected {}",
							ConfigurationField.class.getSimpleName(), field.getName(), defaultValue, format);
				}
				catch(final Exception e){
					throw AnnotationException.create("Invalid pattern in {} in field {}", ConfigurationField.class.getSimpleName(),
						field.getName(), e);
				}
			}

			if(enumeration != NullEnum.class){
				//non-empty enumeration
				final Enum<?>[] enumConstants = enumeration.getEnumConstants();
				if(enumConstants.length == 0)
					throw AnnotationException.create("Empty enum in {} in field {}", ConfigurationField.class.getSimpleName(),
						field.getName());

				//enumeration compatible with variable type
				if(!field.getType().isAssignableFrom(enumeration))
					throw AnnotationException.create("Incompatible enum in {} in field {}; found {}, expected {}",
						ConfigurationField.class.getSimpleName(), field.getName(), enumeration.getSimpleName(), fieldType.toString());

				if(!defaultValue.isEmpty()){
					boolean found = false;
					for(int i = 0; !found && i < enumConstants.length; i ++)
						if(enumConstants[i].name().equals(defaultValue))
							found = true;
					if(!found)
						throw AnnotationException.create("Default value not compatible with `enumeration` in {} in field {}; found {}, expected on of {}",
							ConfigurationField.class.getSimpleName(), field.getName(), defaultValue, Arrays.toString(enumConstants));
				}
			}
			else if(!minValue.isEmpty() || !maxValue.isEmpty()){
				if(!minValue.isEmpty()){
					//TODO
					//minValue compatible with variable type
					if(!field.getType().isAssignableFrom(value))
						throw AnnotationException.create("Incompatible minimum value in {} in field {}; found {}, expected {}",
							ConfigurationField.class.getSimpleName(), field.getName(), value.getSimpleName(), fieldType.toString());
					//minValue compatible with format

					if(!defaultValue.isEmpty()){
						//TODO
						//defaultValue compatible with minValue
					}
				}
				if(!maxValue.isEmpty()){
					//TODO
					//maxValue compatible with variable type
					if(!field.getType().isAssignableFrom(value))
						throw AnnotationException.create("Incompatible maximum value in {} in field {}; found {}, expected {}",
							ConfigurationField.class.getSimpleName(), field.getName(), value.getSimpleName(), fieldType.toString());
					//maxValue compatible with format

					if(!defaultValue.isEmpty()){
						//TODO
						//defaultValue compatible with maxValue
					}
				}

				if(!minValue.isEmpty() && !maxValue.isEmpty()){
					//TODO
					//maxValue after or equal to minValue
				}
			}

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
						throw AnnotationException.create("Invalid minimum protocol version in {} in field {}; found {}",
							ConfigurationField.class.getSimpleName(), field.getName(), minProtocol);
					}
				}
				Version maximum = null;
				if(!maxProtocol.isEmpty()){
					try{
						maximum = new Version(maxProtocol);
					}
					catch(final IllegalArgumentException iae){
						throw AnnotationException.create("Invalid maximum protocol version in {} in field {}; found {}",
							ConfigurationField.class.getSimpleName(), field.getName(), maxProtocol);
					}
				}
				//maxProtocol after or equal to minProtocol
				if(minimum != null && maximum != null && maximum.isLessThan(maximum))
					throw AnnotationException.create("Minimum protocol version is greater than maximum protocol version in {} in field {}; found {}",
						ConfigurationField.class.getSimpleName(), field.getName(), maxProtocol);
			}
		}
	};


	private static final Map<Class<? extends Annotation>, ConfigurationAnnotationValidator> ANNOTATION_VALIDATORS = new HashMap<>(5);
	static{
		for(final ConfigurationAnnotationValidator validator : values())
			ANNOTATION_VALIDATORS.put(validator.annotationType, validator);
	}

	private final Class<? extends Annotation> annotationType;


	ConfigurationAnnotationValidator(final Class<? extends Annotation> type){
		annotationType = type;
	}

	static ConfigurationAnnotationValidator fromAnnotation(final Annotation annotation){
		return ANNOTATION_VALIDATORS.get(annotation.annotationType());
	}

	abstract void validate(final Field field, final Annotation annotation) throws AnnotationException;

}
