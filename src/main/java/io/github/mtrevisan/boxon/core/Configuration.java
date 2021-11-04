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

import io.github.mtrevisan.boxon.annotations.configurations.ConfigurationHeader;
import io.github.mtrevisan.boxon.annotations.configurations.ConfigurationSkip;
import io.github.mtrevisan.boxon.exceptions.AnnotationException;
import io.github.mtrevisan.boxon.internal.ReflectionHelper;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.StringJoiner;


/**
 * The class containing the information that are used to construct configuration messages.
 *
 * @param <T> The type of object the configuration is able to construct configuration messages.
 */
final class Configuration<T>{

	private final Class<T> type;

	private final ConfigurationHeader header;
	private final List<ConfigField> configFields;


	Configuration(final Class<T> type) throws AnnotationException{
		this.type = type;

		header = type.getAnnotation(ConfigurationHeader.class);
		if(header == null)
			throw AnnotationException.create("No header present in this class: {}", type.getName());
		ConfigurationAnnotationValidator.fromAnnotation(header)
			.validate(null, header);

		CodecHelper.assertValidCharset(header.charset());

		final List<ConfigField> configFields = loadAnnotatedFields(type, ReflectionHelper.getAccessibleFields(type));
		this.configFields = Collections.unmodifiableList(configFields);

		if(configFields.isEmpty())
			throw AnnotationException.create("No data can be extracted from this class: {}", type.getName());
	}

	private List<ConfigField> loadAnnotatedFields(final Class<T> type, final List<Field> fields) throws AnnotationException{
		final List<ConfigField> configFields = new ArrayList<>(fields.size());
		for(int i = 0; i < fields.size(); i ++){
			final Field field = fields.get(i);
			final ConfigurationSkip[] skips = field.getDeclaredAnnotationsByType(ConfigurationSkip.class);

			final Annotation[] declaredAnnotations = field.getDeclaredAnnotations();

			try{
				final Annotation validAnnotation = validateField(field, declaredAnnotations);

				if(validAnnotation != null)
					configFields.add(new ConfigField(field, validAnnotation, (skips.length > 0? skips: null)));
			}
			catch(final AnnotationException e){
				e.setClassNameAndFieldName(type.getName(), field.getName());
				throw e;
			}
		}
		return configFields;
	}

	private Annotation validateField(final Field field, final Annotation[] annotations) throws AnnotationException{
		//filter out `@Skip` annotations
		Annotation foundAnnotation = null;
		for(int i = 0; i < annotations.length; i ++){
			final Class<? extends Annotation> annotationType = annotations[i].annotationType();
			if(!io.github.mtrevisan.boxon.annotations.configurations.ConfigurationSkip.class.isAssignableFrom(annotationType)
					&& !ConfigurationSkip.ConfigurationSkips.class.isAssignableFrom(annotationType)){
				if(foundAnnotation != null){
					final StringJoiner sj = new StringJoiner(", ", "[", "]");
					for(int j = 0; j < annotations.length; j ++)
						sj.add(annotations[j].annotationType().getSimpleName());
					throw AnnotationException.create("Cannot bind more that one annotation on {}: {}", type.getName(), sj.toString());
				}

				if(validateAnnotation(field, annotations[i]))
					foundAnnotation = annotations[i];
			}
		}
		return foundAnnotation;
	}

	private static boolean validateAnnotation(final Field field, final Annotation annotation) throws AnnotationException{
		final ConfigurationAnnotationValidator validator = ConfigurationAnnotationValidator.fromAnnotation(annotation);
		if(validator != null)
			validator.validate(field, annotation);
		return (validator != null);
	}

	Class<T> getType(){
		return type;
	}

	ConfigurationHeader getHeader(){
		return header;
	}

	List<ConfigField> getConfigurationFields(){
		return configFields;
	}

	boolean canBeCoded(){
		return (header != null);
	}

	@Override
	public String toString(){
		return type.getSimpleName();
	}

	@Override
	@SuppressWarnings("AccessingNonPublicFieldOfAnotherObject")
	public boolean equals(final Object obj){
		if(obj == this)
			return true;
		if(obj == null || obj.getClass() != getClass())
			return false;

		final Configuration<?> rhs = (Configuration<?>)obj;
		return (type == rhs.type);
	}

	@Override
	public int hashCode(){
		return type.getName().hashCode();
	}

}
