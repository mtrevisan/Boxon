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
import io.github.mtrevisan.boxon.exceptions.AnnotationException;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;


enum ConfigurationAnnotationValidator{

	FIELD(ConfigurationField.class){
		@Override
		void validate(final Field field, final Annotation annotation) throws AnnotationException{
			final ConfigurationField binding = (ConfigurationField)annotation;

			//one only of `format`, `minValue`/`maxValue`, and `enumeration` should be set:
			final String format = binding.format();
			final String minValue = binding.minValue();
			final String maxValue = binding.maxValue();
			final Class<? extends Enum<?>> enumeration = binding.enumeration();
			int set = 0;
			if(!format.isEmpty())
				set ++;
			if(!minValue.isEmpty() || !maxValue.isEmpty())
				set ++;
			if(enumeration != null)
				set ++;
			if(set == 0)
				throw AnnotationException.create("One of `format`, `minValue`/`maxValue`, or `enumeration` should be used",
					ConfigurationField.class.getSimpleName());
			if(set != 1)
				throw AnnotationException.create("Only one of `enumeration`, `format`, or `minValue`/`maxValue` should be used",
					ConfigurationField.class.getSimpleName());

			//TODO
			final Class<?> fieldType = field.getType();
			if(format != null){
				//TODO
				//- defaultValue compatible with format
				//- minValue/maxValue compatible with format
			}

			//- defaultValue compatible with variable type
			//- format compatible with variable type
			//- minValue/maxValue compatible with variable type
			//- enumeration compatible with variable type
			//- maxValue after or equal to minValue
			//- maxProtocol after or equal to minProtocol
			//- enumeration not empty

//			final Class<?> type = binding.type();
//			if(ParserDataType.isPrimitive(type))
//				throw AnnotationException.create("Bad annotation used for {}, should have been used one of the primitive type's annotations",
//					ConfigurationField.class.getSimpleName());
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
