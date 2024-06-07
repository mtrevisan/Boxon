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
package io.github.mtrevisan.boxon.core.helpers.configurations;

import io.github.mtrevisan.boxon.annotations.configurations.ConfigurationHeader;
import io.github.mtrevisan.boxon.annotations.configurations.ConfigurationSkip;
import io.github.mtrevisan.boxon.core.helpers.FieldAccessor;
import io.github.mtrevisan.boxon.core.helpers.templates.TemplateValidator;
import io.github.mtrevisan.boxon.core.helpers.validators.ConfigurationAnnotationValidator;
import io.github.mtrevisan.boxon.exceptions.AnnotationException;
import io.github.mtrevisan.boxon.exceptions.CodecException;
import io.github.mtrevisan.boxon.helpers.JavaHelper;
import io.github.mtrevisan.boxon.semanticversioning.Version;
import io.github.mtrevisan.boxon.semanticversioning.VersionBuilder;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;


/**
 * The class containing the information that are used to construct configuration messages.
 *
 * @param <T>	The type of object the configuration is able to construct configuration messages.
 */
public final class ConfigurationMessage<T>{

	private static final int ORDER_ALTERNATIVE_INDEX = 0;
	private static final int ORDER_COMPOSITE_INDEX = 1;
	private static final int ORDER_FIELD_INDEX = 2;

	private static final String CONFIGURATION_NAME_ALTERNATIVE = "AlternativeConfigurationField";
	private static final String CONFIGURATION_NAME_COMPOSITE = "CompositeConfigurationField";
	private static final String CONFIGURATION_NAME_FIELD = "ConfigurationField";
	private static final String CONFIGURATION_NAME_SKIP = "ConfigurationSkip";


	private final Class<T> type;

	private final ConfigurationHeader header;
	private final List<ConfigurationField> configurationFields;

	private final List<String> protocolVersionBoundaries;


	/**
	 * Create a configuration message for the given class.
	 * @param type	The class of the configuration message.
	 * @param <T>	The class type parameter.
	 * @return	An instance of configuration message.
	 * @throws AnnotationException	If a configuration annotation is invalid, or no annotation was found.
	 */
	public static <T> ConfigurationMessage<T> create(final Class<T> type) throws AnnotationException{
		return new ConfigurationMessage<>(type);
	}


	private ConfigurationMessage(final Class<T> type) throws AnnotationException{
		this.type = type;

		header = type.getAnnotation(ConfigurationHeader.class);
		if(header == null)
			throw AnnotationException.create("No header present in this class: {}", type.getName());

		final Version minProtocolVersion = VersionBuilder.of(header.minProtocol());
		final Version maxProtocolVersion = VersionBuilder.of(header.maxProtocol());
		try{
			final ConfigurationAnnotationValidator validator = ConfigurationAnnotationValidator.fromAnnotationType(header.annotationType());
			validator.validate(null, header, minProtocolVersion, maxProtocolVersion);

			configurationFields = loadAnnotatedFields(type, minProtocolVersion, maxProtocolVersion);

			protocolVersionBoundaries = extractProtocolVersionBoundaries(configurationFields);

			if(configurationFields.isEmpty())
				throw AnnotationException.create("No data can be extracted from this class: {}", type.getName());
		}
		catch(final CodecException ce){
			throw AnnotationException.create(ce);
		}
	}


	private List<ConfigurationField> loadAnnotatedFields(final Class<T> type, final Version minProtocolVersion,
			final Version maxProtocolVersion) throws AnnotationException, CodecException{
		final List<Field> fields = FieldAccessor.getAccessibleFields(type);
		final int size = fields.size();
		final Collection<String> uniqueShortDescription = new HashSet<>(size);
		final ArrayList<ConfigurationField> configurationFields = new ArrayList<>(size);
		for(int i = 0; i < size; i ++){
			final Field field = fields.get(i);

			final ConfigurationSkip[] skips = field.getDeclaredAnnotationsByType(ConfigurationSkip.class);

			final Annotation[] declaredAnnotations = field.getDeclaredAnnotations();
			validateAnnotationsOrder(declaredAnnotations);

			try{
				final Annotation validAnnotation = extractAndValidateConfigurationAnnotation(field, declaredAnnotations, minProtocolVersion,
					maxProtocolVersion);

				validateShortDescriptionUniqueness(validAnnotation, uniqueShortDescription, type);

				if(validAnnotation != null)
					configurationFields.add(ConfigurationField.create(field, validAnnotation, skips));
			}
			catch(final AnnotationException | CodecException e){
				e.withClassAndField(type, field);
				throw e;
			}
		}
		return JavaHelper.trimAndCreateUnmodifiable(configurationFields);
	}


	private static void validateAnnotationsOrder(final Annotation[] annotations) throws AnnotationException{
		final int length = annotations.length;
		if(length <= 1)
			return;

		final boolean[] annotationFound = new boolean[ORDER_FIELD_INDEX + 1];
		for(final Annotation annotation : annotations){
			final String annotationName = annotation.annotationType()
				.getSimpleName();

			if(annotationName.startsWith(CONFIGURATION_NAME_ALTERNATIVE)){
				validateAlternativeAnnotationOrder(annotationFound);

				annotationFound[ORDER_ALTERNATIVE_INDEX] = true;
			}
			else if(annotationName.equals(CONFIGURATION_NAME_COMPOSITE)){
				validateCompositeAnnotationOrder(annotationFound);

				annotationFound[ORDER_COMPOSITE_INDEX] = true;
			}
			else if(annotationName.equals(CONFIGURATION_NAME_FIELD)){
				validateFieldAnnotationOrder(annotationFound);

				annotationFound[ORDER_FIELD_INDEX] = true;
			}
			else if(annotationName.startsWith(CONFIGURATION_NAME_SKIP))
				validateSkipAnnotationOrder(annotationFound);
		}
	}

	private static void validateAlternativeAnnotationOrder(final boolean[] annotationFound) throws AnnotationException{
		if(annotationFound[ORDER_ALTERNATIVE_INDEX])
			throw AnnotationException.create(TemplateValidator.ANNOTATION_ORDER_ERROR_WRONG_NUMBER,
				CONFIGURATION_NAME_ALTERNATIVE);
		if(annotationFound[ORDER_COMPOSITE_INDEX])
			throw AnnotationException.create(TemplateValidator.ANNOTATION_ORDER_ERROR_INCOMPATIBLE,
				CONFIGURATION_NAME_ALTERNATIVE, CONFIGURATION_NAME_COMPOSITE);
		if(annotationFound[ORDER_FIELD_INDEX])
			throw AnnotationException.create(TemplateValidator.ANNOTATION_ORDER_ERROR_INCOMPATIBLE,
				CONFIGURATION_NAME_ALTERNATIVE, CONFIGURATION_NAME_FIELD);
	}

	private static void validateCompositeAnnotationOrder(final boolean[] annotationFound) throws AnnotationException{
		if(annotationFound[ORDER_ALTERNATIVE_INDEX])
			throw AnnotationException.create(TemplateValidator.ANNOTATION_ORDER_ERROR_INCOMPATIBLE,
				CONFIGURATION_NAME_COMPOSITE, CONFIGURATION_NAME_ALTERNATIVE);
		if(annotationFound[ORDER_COMPOSITE_INDEX])
			throw AnnotationException.create(TemplateValidator.ANNOTATION_ORDER_ERROR_WRONG_NUMBER,
				CONFIGURATION_NAME_COMPOSITE);
		if(annotationFound[ORDER_FIELD_INDEX])
			throw AnnotationException.create(TemplateValidator.ANNOTATION_ORDER_ERROR_INCOMPATIBLE,
				CONFIGURATION_NAME_COMPOSITE, CONFIGURATION_NAME_FIELD);
	}

	private static void validateFieldAnnotationOrder(final boolean[] annotationFound) throws AnnotationException{
		if(annotationFound[ORDER_ALTERNATIVE_INDEX])
			throw AnnotationException.create(TemplateValidator.ANNOTATION_ORDER_ERROR_INCOMPATIBLE,
				CONFIGURATION_NAME_FIELD, CONFIGURATION_NAME_ALTERNATIVE);
		if(annotationFound[ORDER_COMPOSITE_INDEX])
			throw AnnotationException.create(TemplateValidator.ANNOTATION_ORDER_ERROR_INCOMPATIBLE,
				CONFIGURATION_NAME_FIELD, CONFIGURATION_NAME_COMPOSITE);
		if(annotationFound[ORDER_FIELD_INDEX])
			throw AnnotationException.create(TemplateValidator.ANNOTATION_ORDER_ERROR_WRONG_NUMBER,
				CONFIGURATION_NAME_FIELD);
	}

	private static void validateSkipAnnotationOrder(final boolean[] annotationFound) throws AnnotationException{
		if(annotationFound[ORDER_ALTERNATIVE_INDEX])
			throw AnnotationException.create(TemplateValidator.ANNOTATION_ORDER_ERROR_WRONG_ORDER,
				CONFIGURATION_NAME_SKIP, CONFIGURATION_NAME_ALTERNATIVE);
		if(annotationFound[ORDER_COMPOSITE_INDEX])
			throw AnnotationException.create(TemplateValidator.ANNOTATION_ORDER_ERROR_WRONG_ORDER,
				CONFIGURATION_NAME_SKIP, CONFIGURATION_NAME_COMPOSITE);
		if(annotationFound[ORDER_FIELD_INDEX])
			throw AnnotationException.create(TemplateValidator.ANNOTATION_ORDER_ERROR_WRONG_ORDER,
				CONFIGURATION_NAME_SKIP, CONFIGURATION_NAME_FIELD);
	}


	private void validateShortDescriptionUniqueness(final Annotation annotation, final Collection<String> uniqueShortDescription,
			final Class<T> type) throws AnnotationException{
		final ConfigurationManager manager = ConfigurationManagerFactory.buildManager(annotation);
		final String shortDescription = manager.getShortDescription();
		if(!uniqueShortDescription.add(shortDescription))
			throw AnnotationException.create("Duplicated short description in {}: {}", type.getName(), shortDescription);
	}

	/**
	 * Validates a configuration field and return the first valid configuration annotation.
	 *
	 * @param field	The configuration field to validate.
	 * @param annotations	The list of annotations on the configuration field.
	 * @param minProtocolVersion	The minimum protocol version (should follow <a href="https://semver.org/">Semantic Versioning</a>).
	 * @param maxProtocolVersion	The maximum protocol version (should follow <a href="https://semver.org/">Semantic Versioning</a>).
	 * @return	The first valid configuration annotation, or {@code null} if none are found.
	 * @throws AnnotationException	If an annotation error occurs.
	 */
	private static Annotation extractAndValidateConfigurationAnnotation(final Field field, final Annotation[] annotations,
			final Version minProtocolVersion, final Version maxProtocolVersion) throws AnnotationException, CodecException{
		Annotation foundAnnotation = null;
		for(int i = 0, length = annotations.length; foundAnnotation == null && i < length; i ++){
			final Annotation annotation = annotations[i];

			final Class<? extends Annotation> annotationType = annotation.annotationType();
			if(ConfigurationSkip.class.isAssignableFrom(annotationType)
					|| ConfigurationSkip.ConfigurationSkips.class.isAssignableFrom(annotationType))
				continue;

			validateAnnotation(field, annotation, minProtocolVersion, maxProtocolVersion);

			foundAnnotation = annotation;
		}
		return foundAnnotation;
	}

	private static void validateAnnotation(final Field field, final Annotation annotation, final Version minProtocolVersion,
			final Version maxProtocolVersion) throws AnnotationException, CodecException{
		final ConfigurationAnnotationValidator validator = ConfigurationAnnotationValidator.fromAnnotationType(annotation.annotationType());
		validator.validate(field, annotation, minProtocolVersion, maxProtocolVersion);
	}

	private List<String> extractProtocolVersionBoundaries(final List<ConfigurationField> fields){
		final int length = fields.size();
		final ArrayList<String> boundaries = new ArrayList<>((length << 1) + 2);
		boundaries.add(header.minProtocol());
		boundaries.add(header.maxProtocol());

		for(int i = 0; i < length; i ++){
			final ConfigurationField field = fields.get(i);

			final Annotation binding = field.getBinding();
			final ConfigurationManager manager = ConfigurationManagerFactory.buildManager(binding);
			manager.addProtocolVersionBoundaries(boundaries);

			final ConfigurationSkip[] skips = field.getSkips();
			extractProtocolVersionBoundaries(skips, boundaries);
		}

		boundaries.sort(Comparator.comparing(VersionBuilder::of));
		removeDuplicates(boundaries);
		boundaries.remove(JavaHelper.EMPTY_STRING);
		return JavaHelper.trimAndCreateUnmodifiable(boundaries);
	}

	private static void extractProtocolVersionBoundaries(final ConfigurationSkip[] skips, final Collection<String> boundaries){
		for(int i = 0, length = skips.length; i < length; i ++){
			final ConfigurationSkip skip = skips[i];

			boundaries.add(skip.minProtocol());
			boundaries.add(skip.maxProtocol());
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

	/**
	 * Class type of the configuration.
	 *
	 * @return	Class type of the configuration.
	 */
	public Class<T> getType(){
		return type;
	}

	/**
	 * Configuration header annotation.
	 *
	 * @return	Configuration header annotation.
	 */
	public ConfigurationHeader getHeader(){
		return header;
	}

	/**
	 * List of configuration fields data.
	 *
	 * @return	List of configuration fields data.
	 */
	public List<ConfigurationField> getConfigurationFields(){
		return configurationFields;
	}

	/**
	 * List of protocol version boundaries.
	 *
	 * @return	List of protocol version boundaries.
	 */
	public List<String> getProtocolVersionBoundaries(){
		return protocolVersionBoundaries;
	}

	/**
	 * Whether the message can be coded, that is, is annotated with {@link ConfigurationHeader}.
	 *
	 * @return	Whether the message can be coded.
	 */
	public boolean canBeCoded(){
		return (header != null);
	}

	@Override
	public String toString(){
		return type.getSimpleName();
	}

	@Override
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
		return type.getName()
			.hashCode();
	}

}
