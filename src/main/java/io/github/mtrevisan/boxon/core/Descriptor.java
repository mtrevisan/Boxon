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
package io.github.mtrevisan.boxon.core;

import io.github.mtrevisan.boxon.annotations.MessageHeader;
import io.github.mtrevisan.boxon.annotations.configurations.ConfigurationHeader;
import io.github.mtrevisan.boxon.core.helpers.configurations.ConfigField;
import io.github.mtrevisan.boxon.core.helpers.configurations.ConfigurationMessage;
import io.github.mtrevisan.boxon.core.helpers.descriptors.AnnotationDescriptor;
import io.github.mtrevisan.boxon.core.helpers.templates.BoundedField;
import io.github.mtrevisan.boxon.core.helpers.templates.EvaluatedField;
import io.github.mtrevisan.boxon.core.helpers.templates.PostProcessedField;
import io.github.mtrevisan.boxon.core.helpers.templates.Template;
import io.github.mtrevisan.boxon.core.keys.ConfigurationKey;
import io.github.mtrevisan.boxon.core.keys.DescriberKey;
import io.github.mtrevisan.boxon.core.parsers.ConfigurationParser;
import io.github.mtrevisan.boxon.core.parsers.LoaderConfiguration;
import io.github.mtrevisan.boxon.core.parsers.LoaderTemplate;
import io.github.mtrevisan.boxon.core.parsers.TemplateParser;
import io.github.mtrevisan.boxon.exceptions.AnnotationException;
import io.github.mtrevisan.boxon.exceptions.ConfigurationException;
import io.github.mtrevisan.boxon.exceptions.EncodeException;
import io.github.mtrevisan.boxon.exceptions.FieldException;
import io.github.mtrevisan.boxon.exceptions.TemplateException;
import io.github.mtrevisan.boxon.helpers.ContextHelper;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;


/**
 * Declarative descriptor for binary encoded data.
 */
@SuppressWarnings({"WeakerAccess", "unused"})
public final class Descriptor{

	private final Core core;

	private final LoaderTemplate loaderTemplate;
	private final LoaderConfiguration loaderConfiguration;


	/**
	 * Create a descriptor.
	 *
	 * @param core	The parser core.
	 * @return	A descriptor.
	 */
	public static Descriptor create(final Core core){
		return new Descriptor(core);
	}


	private Descriptor(final Core core){
		this.core = core;

		final TemplateParser templateParser = core.getTemplateParser();
		loaderTemplate = templateParser.getLoaderTemplate();
		final ConfigurationParser configurationParser = core.getConfigurationParser();
		loaderConfiguration = configurationParser.getLoaderConfiguration();
	}


	/**
	 * Description of all the loaded templates.
	 *
	 * @return	The list of descriptions.
	 * @throws TemplateException	If a template is not well formatted.
	 */
	public List<Map<String, Object>> describeParsing() throws FieldException{
		final Collection<Template<?>> templates = new HashSet<>(loaderTemplate.getTemplates());
		return describeEntities(templates, this::describeParsing);
	}

	/**
	 * Description of a single template annotated with {@link MessageHeader}.
	 *
	 * @param templateClass	Template class to be described.
	 * @return	The list of descriptions.
	 * @throws AnnotationException	If an annotation is not well formatted.
	 * @throws TemplateException	If a template is not well formatted.
	 */
	public Map<String, Object> describeParsing(final Class<?> templateClass) throws FieldException{
		return describeEntity(MessageHeader.class, templateClass, loaderTemplate::extractTemplate, this::describeTemplate);
	}

	/**
	 * Description of all the templates in the given package annotated with {@link MessageHeader}.
	 *
	 * @param templateClasses	Classes to be used ase starting point from which to load annotated classes.
	 * @return	The list of descriptions.
	 * @throws AnnotationException	If an annotation is not well formatted.
	 * @throws TemplateException	If a template is not well formatted.
	 */
	public List<Map<String, Object>> describeParsing(final Class<?>... templateClasses) throws FieldException{
		return describeEntities(MessageHeader.class, templateClasses, loaderTemplate::extractTemplate, this::describeParsing);
	}

	private Map<String, Object> describeParsing(final Template<?> template) throws FieldException{
		final Map<String, Object> description = new HashMap<>(3);
		description.put(DescriberKey.TEMPLATE.toString(), template.getType().getName());
		describeHeader(template.getHeader(), description);
		describeBoundedFields(template.getBoundedFields(), description);
		describeEvaluatedFields(template.getEvaluatedFields(), description);
		describePostProcessedFields(template.getPostProcessedFields(), description);
		describeContext(description);
		return Collections.unmodifiableMap(description);
	}


	/**
	 * Description of all the loaded templates.
	 *
	 * @return	The list of descriptions.
	 * @throws TemplateException	If a template is not well formatted.
	 */
	public List<Map<String, Object>> describeTemplate() throws FieldException{
		final Collection<Template<?>> configurations = new HashSet<>(loaderTemplate.getTemplates());
		return describeEntities(configurations, this::describeTemplate);
	}

	/**
	 * Description of a single template annotated with {@link MessageHeader}.
	 *
	 * @param templateClass	Template class to be described.
	 * @return	The list of descriptions.
	 * @throws AnnotationException	If an annotation is not well formatted.
	 * @throws TemplateException	If a template is not well formatted.
	 */
	public Map<String, Object> describeTemplate(final Class<?> templateClass) throws FieldException{
		return describeEntity(MessageHeader.class, templateClass, loaderTemplate::extractTemplate, this::describeTemplate);
	}

	/**
	 * Description of all the templates in the given package annotated with {@link MessageHeader}.
	 *
	 * @param templateClasses	Classes to be used ase starting point from which to load annotated classes.
	 * @return	The list of descriptions.
	 * @throws AnnotationException	If an annotation is not well formatted.
	 * @throws TemplateException	If a template is not well formatted.
	 */
	public List<Map<String, Object>> describeTemplate(final Class<?>... templateClasses) throws FieldException{
		return describeEntities(MessageHeader.class, templateClasses, loaderTemplate::extractTemplate, this::describeTemplate);
	}

	private Map<String, Object> describeTemplate(final Template<?> template) throws FieldException{
		final Map<String, Object> description = new HashMap<>(3);
		description.put(DescriberKey.TEMPLATE.toString(), template.getType().getName());
		describeHeader(template.getHeader(), description);
		describeBoundedFields(template.getBoundedFields(), description);
		describeContext(description);
		return Collections.unmodifiableMap(description);
	}


	/**
	 * Description of all the loaded configuration.
	 *
	 * @return	The list of descriptions.
	 * @throws ConfigurationException	If a configuration is not well formatted.
	 */
	public List<Map<String, Object>> describeConfiguration() throws FieldException{
		final Collection<ConfigurationMessage<?>> configurations = new HashSet<>(loaderConfiguration.getConfigurations());
		return describeEntities(configurations, this::describeConfiguration);
	}

	/**
	 * Description of a single configuration annotated with {@link ConfigurationHeader}.
	 *
	 * @param configurationClass	configuration class to be described.
	 * @return	The list of descriptions.
	 * @throws AnnotationException	If an annotation is not well formatted.
	 * @throws ConfigurationException	If a configuration is not well formatted.
	 * @throws EncodeException	If a configuration cannot be retrieved.
	 */
	public Map<String, Object> describeConfiguration(final Class<?> configurationClass) throws FieldException, EncodeException{
		final ThrowingFunction<Class<?>, ConfigurationMessage<?>, EncodeException> extractor = cls -> {
			final ConfigurationHeader header = configurationClass.getAnnotation(ConfigurationHeader.class);
			return loaderConfiguration.getConfiguration(header.shortDescription());
		};
		return describeEntity(ConfigurationHeader.class, configurationClass, extractor, this::describeConfiguration);
	}

	/**
	 * Description of all the configurations in the given package annotated with {@link ConfigurationHeader}.
	 *
	 * @param configurationClasses	Classes to be used ase starting point from which to load annotated classes.
	 * @return	The list of descriptions.
	 * @throws AnnotationException	If an annotation is not well formatted.
	 * @throws ConfigurationException	If a configuration is not well formatted.
	 */
	public List<Map<String, Object>> describeConfiguration(final Class<?>... configurationClasses) throws FieldException{
		return describeEntities(ConfigurationHeader.class, configurationClasses, loaderConfiguration::extractConfiguration,
			this::describeConfiguration);
	}

	private Map<String, Object> describeConfiguration(final ConfigurationMessage<?> configuration) throws FieldException{
		final Map<String, Object> description = new HashMap<>(3);
		description.put(ConfigurationKey.CONFIGURATION.toString(), configuration.getType().getName());
		describeHeader(configuration.getHeader(), description);
		describeConfigFields(configuration.getConfigurationFields(), description);
		describeContext(description);
		return Collections.unmodifiableMap(description);
	}


	private <T> List<Map<String, Object>> describeEntities(final Collection<T> entities,
			final ThrowingFunction<T, Map<String, Object>, FieldException> mapper) throws FieldException{
		final List<Map<String, Object>> descriptions = new ArrayList<>(entities.size());
		for(final T entity : entities)
			descriptions.add(mapper.apply(entity));
		return Collections.unmodifiableList(descriptions);
	}

	private <T, E extends Exception> Map<String, Object> describeEntity(final Class<? extends Annotation> annotationClass,
			final Class<?> entityClass, final ThrowingFunction<Class<?>, T, E> extractor,
			final ThrowingFunction<T, Map<String, Object>, FieldException> mapper) throws FieldException, E{
		if(!entityClass.isAnnotationPresent(annotationClass))
			throw AnnotationException.create("Entity {} didn't have the `{}` annotation", entityClass.getSimpleName(),
				annotationClass.getSimpleName());

		final T entity = extractor.apply(entityClass);
		return Collections.unmodifiableMap(mapper.apply(entity));
	}

	private <T, E extends Exception> List<Map<String, Object>> describeEntities(final Class<? extends Annotation> annotationClass,
			final Class<?>[] entitiesClass, final ThrowingFunction<Class<?>, T, E> extractor,
			final ThrowingFunction<T, Map<String, Object>, FieldException> mapper) throws FieldException, E{
		final int length = entitiesClass.length;
		final List<Map<String, Object>> description = new ArrayList<>(length);
		for(int i = 0; i < length; i ++){
			final Class<?> entityClass = entitiesClass[i];

			if(entityClass.isAnnotationPresent(annotationClass)){
				final T entity = extractor.apply(entityClass);
				description.add(mapper.apply(entity));
			}
		}
		return Collections.unmodifiableList(description);
	}

	private static void describeHeader(final MessageHeader header, final Map<String, Object> description){
		final Map<String, Object> headerDescription = new HashMap<>(3);
		AnnotationDescriptor.putIfNotEmpty(DescriberKey.HEADER_START, Arrays.toString(header.start()), headerDescription);
		AnnotationDescriptor.putIfNotEmpty(DescriberKey.HEADER_END, header.end(), headerDescription);
		AnnotationDescriptor.putIfNotEmpty(DescriberKey.HEADER_CHARSET, header.charset(), headerDescription);
		description.put(DescriberKey.HEADER.toString(), headerDescription);
	}

	private static void describeHeader(final ConfigurationHeader header, final Map<String, Object> description){
		final Map<String, Object> headerDescription = new HashMap<>(3);
		AnnotationDescriptor.putIfNotEmpty(ConfigurationKey.SHORT_DESCRIPTION, header.shortDescription(), headerDescription);
		AnnotationDescriptor.putIfNotEmpty(ConfigurationKey.LONG_DESCRIPTION, header.longDescription(), headerDescription);
		AnnotationDescriptor.putIfNotEmpty(ConfigurationKey.MIN_PROTOCOL, header.minProtocol(), headerDescription);
		AnnotationDescriptor.putIfNotEmpty(ConfigurationKey.MAX_PROTOCOL, header.maxProtocol(), headerDescription);
		AnnotationDescriptor.putIfNotEmpty(ConfigurationKey.HEADER_START, header.start(), headerDescription);
		AnnotationDescriptor.putIfNotEmpty(ConfigurationKey.HEADER_END, header.end(), headerDescription);
		AnnotationDescriptor.putIfNotEmpty(ConfigurationKey.HEADER_CHARSET, header.charset(), headerDescription);
		description.put(ConfigurationKey.HEADER.toString(), headerDescription);
	}

	private static void describeBoundedFields(final List<BoundedField> fields, final Map<String, Object> description) throws FieldException{
		final int length = fields.size();
		final Collection<Map<String, Object>> fieldsDescription = new ArrayList<>(length);
		for(int i = 0; i < length; i ++){
			final BoundedField field = fields.get(i);

			AnnotationDescriptor.describeSkips(field.getSkips(), fieldsDescription);

			describeField(field.getBinding(), field.getFieldName(), field.getFieldType(), fieldsDescription);
		}
		description.put(DescriberKey.FIELDS.toString(), fieldsDescription);
	}

	private static void describeEvaluatedFields(final List<EvaluatedField> fields, final Map<String, Object> description)
			throws FieldException{
		final int length = fields.size();
		final Collection<Map<String, Object>> fieldsDescription = new ArrayList<>(length);
		for(int i = 0; i < length; i ++){
			final EvaluatedField field = fields.get(i);

			describeField(field.getBinding(), field.getFieldName(), field.getFieldType(), fieldsDescription);
		}
		description.put(DescriberKey.EVALUATED_FIELDS.toString(), fieldsDescription);
	}

	private static void describePostProcessedFields(final List<PostProcessedField> fields, final Map<String, Object> description)
			throws FieldException{
		final int length = fields.size();
		final Collection<Map<String, Object>> fieldsDescription = new ArrayList<>(length);
		for(int i = 0; i < length; i ++){
			final PostProcessedField field = fields.get(i);

			describeField(field.getBinding(), field.getFieldName(), field.getFieldType(), fieldsDescription);
		}
		description.put(DescriberKey.POST_PROCESSED_FIELDS.toString(), fieldsDescription);
	}

	private void describeContext(final Map<String, Object> description){
		final Map<String, Object> ctx = new HashMap<>(core.getContext());
		ctx.remove(ContextHelper.CONTEXT_SELF);
		ctx.remove(ContextHelper.CONTEXT_CHOICE_PREFIX);
		if(!ctx.isEmpty())
			description.put(DescriberKey.CONTEXT.toString(), ctx);
	}

	private static void describeConfigFields(final List<ConfigField> fields, final Map<String, Object> description) throws FieldException{
		final int length = fields.size();
		final Collection<Map<String, Object>> fieldsDescription = new ArrayList<>(length);
		for(int i = 0; i < length; i ++){
			final ConfigField field = fields.get(i);

			AnnotationDescriptor.describeSkips(field.getSkips(), fieldsDescription);

			describeField(field.getBinding(), field.getFieldName(), field.getFieldType(), fieldsDescription);
		}
		description.put(DescriberKey.FIELDS.toString(), fieldsDescription);
	}

	private static void describeField(final Annotation binding, final String fieldName, final Class<?> fieldType,
			final Collection<Map<String, Object>> fieldsDescription) throws FieldException{
		final Map<String, Object> fieldDescription = createFieldDescription(fieldName, fieldType.getName(),
			binding.annotationType());

		//extract binding descriptor
		final AnnotationDescriptor descriptor = checkAndGetDescriptor(binding);
		descriptor.describe(binding, fieldDescription);

		fieldsDescription.add(fieldDescription);
	}

	private static Map<String, Object> createFieldDescription(final String fieldName, final String name,
			final Class<? extends Annotation> annotationType){
		final Map<String, Object> fieldDescription = new HashMap<>(13);
		AnnotationDescriptor.putIfNotEmpty(DescriberKey.FIELD_NAME, fieldName, fieldDescription);
		AnnotationDescriptor.putIfNotEmpty(DescriberKey.FIELD_TYPE, name, fieldDescription);
		AnnotationDescriptor.putIfNotEmpty(DescriberKey.ANNOTATION_TYPE, annotationType.getName(), fieldDescription);
		return fieldDescription;
	}

	private static AnnotationDescriptor checkAndGetDescriptor(final Annotation binding) throws FieldException{
		final AnnotationDescriptor descriptor = AnnotationDescriptor.fromAnnotation(binding);
		if(descriptor == null)
			throw FieldException.create("Cannot extract descriptor for this annotation: {}",
				binding.annotationType().getSimpleName());

		return descriptor;
	}


	@FunctionalInterface
	private interface ThrowingFunction<T, R, E extends Exception>{
		R apply(T t) throws E;
	}

}
