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

import io.github.mtrevisan.boxon.annotations.configurations.ConfigurationHeader;
import io.github.mtrevisan.boxon.annotations.configurations.ConfigurationSkip;
import io.github.mtrevisan.boxon.codecs.managers.configuration.ConfigurationManagerFactory;
import io.github.mtrevisan.boxon.codecs.managers.configuration.ConfigurationManagerInterface;
import io.github.mtrevisan.boxon.exceptions.AnnotationException;
import io.github.mtrevisan.boxon.exceptions.CodecException;
import io.github.mtrevisan.boxon.exceptions.ConfigurationException;
import io.github.mtrevisan.boxon.external.semanticversioning.Version;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;


/**
 * The class containing the information that are used to construct configuration messages.
 *
 * @param <T> The type of object the configuration is able to construct configuration messages.
 */
public final class ConfigurationMessage<T>{

	private static final String EMPTY_STRING = "";


	private final Class<T> type;

	private final ConfigurationHeader header;
	private final List<ConfigField> configFields;

	private final List<String> protocolVersionBoundaries;


	public static <T> ConfigurationMessage<T> create(final Class<T> type) throws AnnotationException{
		return new ConfigurationMessage<>(type);
	}

	private ConfigurationMessage(final Class<T> type) throws AnnotationException{
		this.type = type;

		header = type.getAnnotation(ConfigurationHeader.class);
		if(header == null)
			throw AnnotationException.create("No header present in this class: {}", type.getName());

		final Version minProtocolVersion = Version.of(header.minProtocol());
		final Version maxProtocolVersion = Version.of(header.maxProtocol());
		try{
			final ConfigurationAnnotationValidator validator = ConfigurationAnnotationValidator.fromAnnotationType(header.annotationType());
			validator.validate(null, header, minProtocolVersion, maxProtocolVersion);

			final List<ConfigField> configFields = loadAnnotatedFields(type, ReflectionHelper.getAccessibleFields(type), minProtocolVersion,
				maxProtocolVersion);
			this.configFields = Collections.unmodifiableList(configFields);

			final List<String> protocolVersionBoundaries = extractProtocolVersionBoundaries(configFields);
			this.protocolVersionBoundaries = Collections.unmodifiableList(protocolVersionBoundaries);

			if(configFields.isEmpty())
				throw AnnotationException.create("No data can be extracted from this class: {}", type.getName());
		}
		catch(final ConfigurationException | CodecException ce){
			throw AnnotationException.create(ce);
		}
	}

	private static void removeDuplicates(final Iterable<String> protocolVersions){
		String previous = null;
		final Iterator<String> itr = protocolVersions.iterator();
		while(itr.hasNext()){
			final String current = itr.next();
			if(current.equals(previous))
				itr.remove();

			previous = current;
		}
	}

	@SuppressWarnings("ObjectAllocationInLoop")
	private List<ConfigField> loadAnnotatedFields(final Class<T> type, final List<Field> fields, final Version minProtocolVersion,
			final Version maxProtocolVersion) throws AnnotationException, ConfigurationException, CodecException{
		final Collection<String> uniqueShortDescription = new HashSet<>(fields.size());
		final List<ConfigField> configFields = new ArrayList<>(fields.size());
		for(int i = 0; i < fields.size(); i ++){
			final Field field = fields.get(i);
			final ConfigurationSkip[] skips = field.getDeclaredAnnotationsByType(ConfigurationSkip.class);

			final Annotation[] declaredAnnotations = field.getDeclaredAnnotations();

			try{
				final Annotation validAnnotation = validateField(field, declaredAnnotations, minProtocolVersion, maxProtocolVersion);

				validateShortDescriptionUniqueness(validAnnotation, uniqueShortDescription, type);

				if(validAnnotation != null)
					configFields.add(new ConfigField(field, validAnnotation, (skips.length > 0? skips: null)));
			}
			catch(final AnnotationException | CodecException e){
				e.withClassNameAndFieldName(type.getName(), field.getName());
				throw e;
			}
		}
		return configFields;
	}

	private void validateShortDescriptionUniqueness(final Annotation annotation, final Collection<String> uniqueShortDescription,
			final Class<T> type) throws AnnotationException{
		final ConfigurationManagerInterface manager = ConfigurationManagerFactory.buildManager(annotation);
		final String shortDescription = manager.getShortDescription();
		if(!uniqueShortDescription.add(shortDescription))
			throw AnnotationException.create("Duplicated short description in {}: {}", type.getName(), shortDescription);
	}

	private static Annotation validateField(final Field field, final Annotation[] annotations, final Version minProtocolVersion,
			final Version maxProtocolVersion) throws AnnotationException, CodecException{
		/** filter out {@link ConfigurationSkip} annotations */
		Annotation foundAnnotation = null;
		for(int i = 0; foundAnnotation == null && i < annotations.length; i ++){
			final Class<? extends Annotation> annotationType = annotations[i].annotationType();
			if(ConfigurationSkip.class.isAssignableFrom(annotationType)
					|| ConfigurationSkip.ConfigurationSkips.class.isAssignableFrom(annotationType))
				continue;

			validateAnnotation(field, annotations[i], minProtocolVersion, maxProtocolVersion);

			foundAnnotation = annotations[i];
		}
		return foundAnnotation;
	}

	private static void validateAnnotation(final Field field, final Annotation annotation, final Version minProtocolVersion,
			final Version maxProtocolVersion) throws AnnotationException, CodecException{
		final ConfigurationAnnotationValidator validator = ConfigurationAnnotationValidator.fromAnnotationType(annotation.annotationType());
		validator.validate(field, annotation, minProtocolVersion, maxProtocolVersion);
	}

	private List<String> extractProtocolVersionBoundaries(final List<ConfigField> configFields){
		final List<String> protocolVersionBoundaries = new ArrayList<>(configFields.size() * 2 + 2);
		protocolVersionBoundaries.add(header.minProtocol());
		protocolVersionBoundaries.add(header.maxProtocol());

		for(int i = 0; i < configFields.size(); i ++){
			final ConfigField cf = configFields.get(i);

			final Annotation annotation = cf.getBinding();
			final ConfigurationManagerInterface manager = ConfigurationManagerFactory.buildManager(annotation);
			manager.addProtocolVersionBoundaries(protocolVersionBoundaries);

			final ConfigurationSkip[] skips = cf.getSkips();
			for(int j = 0; j < skips.length; j ++){
				protocolVersionBoundaries.add(skips[j].minProtocol());
				protocolVersionBoundaries.add(skips[j].maxProtocol());
			}
		}

		protocolVersionBoundaries.sort(Comparator.comparing(Version::of));
		removeDuplicates(protocolVersionBoundaries);
		protocolVersionBoundaries.remove(EMPTY_STRING);
		return protocolVersionBoundaries;
	}

	public Class<T> getType(){
		return type;
	}

	public ConfigurationHeader getHeader(){
		return header;
	}

	public List<ConfigField> getConfigurationFields(){
		return configFields;
	}

	public List<String> getProtocolVersionBoundaries(){
		return protocolVersionBoundaries;
	}

	public boolean canBeCoded(){
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

		final ConfigurationMessage<?> rhs = (ConfigurationMessage<?>)obj;
		return (type == rhs.type);
	}

	@Override
	public int hashCode(){
		return type.getName().hashCode();
	}

}
