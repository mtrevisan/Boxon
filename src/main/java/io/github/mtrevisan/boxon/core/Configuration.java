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

import io.github.mtrevisan.boxon.annotations.configurations.AlternativeConfigurationField;
import io.github.mtrevisan.boxon.annotations.configurations.AlternativeConfigurationFields;
import io.github.mtrevisan.boxon.annotations.configurations.CompositeConfigurationField;
import io.github.mtrevisan.boxon.annotations.configurations.ConfigurationField;
import io.github.mtrevisan.boxon.annotations.configurations.ConfigurationHeader;
import io.github.mtrevisan.boxon.annotations.configurations.ConfigurationSkip;
import io.github.mtrevisan.boxon.exceptions.AnnotationException;
import io.github.mtrevisan.boxon.internal.JavaHelper;
import io.github.mtrevisan.boxon.internal.ReflectionHelper;
import io.github.mtrevisan.boxon.internal.semanticversioning.Version;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.StringJoiner;


/**
 * The class containing the information that are used to construct configuration messages.
 *
 * @param <T> The type of object the configuration is able to construct configuration messages.
 */
final class Configuration<T>{

	private static final String EMPTY_STRING = "";


	private final Class<T> type;

	private final ConfigurationHeader header;
	private final List<ConfigField> configFields;

	private final List<String> protocolVersionBoundaries;


	Configuration(final Class<T> type) throws AnnotationException{
		this.type = type;

		header = type.getAnnotation(ConfigurationHeader.class);
		if(header == null)
			throw AnnotationException.create("No header present in this class: {}", type.getName());
		final Version minMessageProtocol = Version.of(header.minProtocol());
		final Version maxMessageProtocol = Version.of(header.maxProtocol());
		ConfigurationAnnotationValidator.fromAnnotation(header)
			.validate(null, header, minMessageProtocol, maxMessageProtocol);

		final List<ConfigField> configFields = loadAnnotatedFields(type, ReflectionHelper.getAccessibleFields(type), minMessageProtocol,
			maxMessageProtocol);
		this.configFields = Collections.unmodifiableList(configFields);

		final List<String> protocolVersionBoundaries = extractProtocolVersionBoundaries(configFields);
		this.protocolVersionBoundaries = Collections.unmodifiableList(protocolVersionBoundaries);

		if(configFields.isEmpty())
			throw AnnotationException.create("No data can be extracted from this class: {}", type.getName());
	}

	private static void removeDuplicates(final Iterable<String> protocolVersions){
		String previous = null;
		final Iterator<String> itr = protocolVersions.iterator();
		while(itr.hasNext()){
			if(previous == null)
				previous = itr.next();
			else{
				final String current = itr.next();
				if(current.equals(previous))
					itr.remove();
				previous = current;
			}
		}
	}

	private List<ConfigField> loadAnnotatedFields(final Class<T> type, final List<Field> fields, final Version minMessageProtocol,
			final Version maxMessageProtocol) throws AnnotationException{
		final List<ConfigField> configFields = new ArrayList<>(fields.size());
		for(int i = 0; i < fields.size(); i ++){
			final Field field = fields.get(i);
			final ConfigurationSkip[] skips = field.getDeclaredAnnotationsByType(ConfigurationSkip.class);

			final Annotation[] declaredAnnotations = field.getDeclaredAnnotations();

			try{
				final Annotation validAnnotation = validateField(field, declaredAnnotations, minMessageProtocol, maxMessageProtocol);

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

	private Annotation validateField(final Field field, final Annotation[] annotations, final Version minMessageProtocol,
			final Version maxMessageProtocol) throws AnnotationException{
		//filter out `@ConfigurationSkip` annotations
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

				if(validateAnnotation(field, annotations[i], minMessageProtocol, maxMessageProtocol))
					foundAnnotation = annotations[i];
			}
		}
		return foundAnnotation;
	}

	private static boolean validateAnnotation(final Field field, final Annotation annotation, final Version minMessageProtocol,
			final Version maxMessageProtocol) throws AnnotationException{
		final ConfigurationAnnotationValidator validator = ConfigurationAnnotationValidator.fromAnnotation(annotation);
		if(validator != null)
			validator.validate(field, annotation, minMessageProtocol, maxMessageProtocol);
		return (validator != null);
	}

	private List<String> extractProtocolVersionBoundaries(final List<ConfigField> configFields){
		final List<String> protocolVersionBoundaries = new ArrayList<>(configFields.size() * 2 + 2);
		protocolVersionBoundaries.add(header.minProtocol());
		protocolVersionBoundaries.add(header.maxProtocol());

		for(int i = 0; i < configFields.size(); i ++){
			final ConfigField cf = configFields.get(i);

			final Annotation annotation = cf.getBinding();
			if(ConfigurationField.class.isInstance(annotation)){
				final ConfigurationField binding = (ConfigurationField)annotation;
				protocolVersionBoundaries.add(binding.minProtocol());
				protocolVersionBoundaries.add(binding.maxProtocol());
			}
			else if(CompositeConfigurationField.class.isInstance(annotation)){
				final CompositeConfigurationField binding = (CompositeConfigurationField)annotation;
				protocolVersionBoundaries.add(binding.minProtocol());
				protocolVersionBoundaries.add(binding.maxProtocol());
			}
			else if(AlternativeConfigurationFields.class.isInstance(annotation)){
				final AlternativeConfigurationFields binding = (AlternativeConfigurationFields)annotation;
				protocolVersionBoundaries.add(binding.minProtocol());
				protocolVersionBoundaries.add(binding.maxProtocol());

				final AlternativeConfigurationField[] alternativeFields = binding.value();
				for(int j = 0; j < alternativeFields.length; j ++){
					final AlternativeConfigurationField fieldBinding = alternativeFields[j];
					protocolVersionBoundaries.add(fieldBinding.minProtocol());
					protocolVersionBoundaries.add(fieldBinding.maxProtocol());
				}
			}

			final ConfigurationSkip[] skips = cf.getSkips();
			for(int j = 0; j < JavaHelper.lengthOrZero(skips); j ++){
				protocolVersionBoundaries.add(skips[j].minProtocol());
				protocolVersionBoundaries.add(skips[j].maxProtocol());
			}
		}

		protocolVersionBoundaries.sort(Comparator.comparing(Version::of));
		removeDuplicates(protocolVersionBoundaries);
		protocolVersionBoundaries.remove(EMPTY_STRING);
		return protocolVersionBoundaries;
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

	List<String> getProtocolVersionBoundaries(){
		return protocolVersionBoundaries;
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
