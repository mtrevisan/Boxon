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

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;


enum ConfigurationAnnotationValidator{

	FIELD(ConfigurationField.class){
		@Override
		void validate(final Field field, final Annotation annotation) throws AnnotationException{
			final ConfigurationField binding = (ConfigurationField)annotation;

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

				//defaultValue compatible with format
				//defaultValue compatible with minValue/maxValue
				//defaultValue compatible with enumeration
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
			Pattern formatPattern = null;
			if(!format.isEmpty()){
				try{
					formatPattern = Pattern.compile(format);

					if(!defaultValue.isEmpty()){
						//TODO
						//defaultValue compatible with format
					}
				}
				catch(final Exception e){
					throw AnnotationException.create("Invalid pattern in {} in field {}", ConfigurationField.class.getSimpleName(),
						field.getName(), e);
				}
			}

			final ParserDataType fieldType = ParserDataType.fromType(field.getType());

			if(enumeration != NullEnum.class){
				//non-empty enumeration
				if(enumeration.getEnumConstants().length == 0)
					throw AnnotationException.create("Empty enum in {} in field {}", ConfigurationField.class.getSimpleName(),
						field.getName());

				//enumeration compatible with variable type
				if(!field.getType().isAssignableFrom(enumeration))
					throw AnnotationException.create("Incompatible enum in {} in field {}, found {}, expected {}",
						ConfigurationField.class.getSimpleName(), field.getName(), enumeration.getSimpleName(), fieldType.toString());

				if(!defaultValue.isEmpty()){
					//TODO
					//defaultValue compatible with enumeration
				}
			}
			else if(!minValue.isEmpty() || !maxValue.isEmpty()){
				if(!minValue.isEmpty()){
					//TODO
					//minValue compatible with variable type
					if(!field.getType().isAssignableFrom(value))
						throw AnnotationException.create("Incompatible minimum value in {} in field {}, found {}, expected {}",
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
						throw AnnotationException.create("Incompatible maximum value in {} in field {}, found {}, expected {}",
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

			//TODO
			//minProtocol/maxProtocol are valid
			//TODO
			//maxProtocol after or equal to minProtocol
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
