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

import io.github.mtrevisan.boxon.annotations.TemplateHeader;
import io.github.mtrevisan.boxon.annotations.configurations.ConfigurationHeader;
import io.github.mtrevisan.boxon.core.helpers.configurations.ConfigurationMessage;
import io.github.mtrevisan.boxon.core.helpers.descriptors.AnnotationDescriptorHelper;
import io.github.mtrevisan.boxon.core.helpers.extractors.FieldExtractor;
import io.github.mtrevisan.boxon.core.helpers.extractors.FieldExtractorConfiguration;
import io.github.mtrevisan.boxon.core.helpers.extractors.FieldExtractorEvaluatedField;
import io.github.mtrevisan.boxon.core.helpers.extractors.FieldExtractorPostProcessedField;
import io.github.mtrevisan.boxon.core.helpers.extractors.FieldExtractorTemplate;
import io.github.mtrevisan.boxon.core.helpers.extractors.MessageExtractor;
import io.github.mtrevisan.boxon.core.helpers.extractors.MessageExtractorBasicTemplate;
import io.github.mtrevisan.boxon.core.helpers.extractors.MessageExtractorConfiguration;
import io.github.mtrevisan.boxon.core.helpers.extractors.MessageExtractorFullTemplate;
import io.github.mtrevisan.boxon.core.helpers.templates.Template;
import io.github.mtrevisan.boxon.core.keys.DescriberKey;
import io.github.mtrevisan.boxon.core.parsers.ConfigurationParser;
import io.github.mtrevisan.boxon.core.parsers.LoaderConfiguration;
import io.github.mtrevisan.boxon.core.parsers.TemplateParser;
import io.github.mtrevisan.boxon.exceptions.AnnotationException;
import io.github.mtrevisan.boxon.exceptions.ConfigurationException;
import io.github.mtrevisan.boxon.exceptions.EncodeException;
import io.github.mtrevisan.boxon.exceptions.FieldException;
import io.github.mtrevisan.boxon.exceptions.TemplateException;
import io.github.mtrevisan.boxon.helpers.ContextHelper;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import static io.github.mtrevisan.boxon.core.helpers.descriptors.AnnotationDescriptorHelper.putIfNotEmpty;


/**
 * Declarative descriptor for binary encoded data.
 */
@SuppressWarnings({"unused", "WeakerAccess"})
public final class Descriptor{

	private static final MessageExtractorBasicTemplate MESSAGE_EXTRACTOR_BASIC_TEMPLATE = new MessageExtractorBasicTemplate();
	private static final MessageExtractorBasicTemplate MESSAGE_EXTRACTOR_FULL_TEMPLATE = new MessageExtractorFullTemplate();
	private static final MessageExtractorConfiguration MESSAGE_EXTRACTOR_CONFIGURATION = new MessageExtractorConfiguration();
	private static final FieldExtractorTemplate FIELD_EXTRACTOR_TEMPLATE = new FieldExtractorTemplate();
	private static final FieldExtractorEvaluatedField FIELD_EXTRACTOR_EVALUATED_FIELD = new FieldExtractorEvaluatedField();
	private static final FieldExtractorPostProcessedField FIELD_EXTRACTOR_POST_PROCESSED_FIELD = new FieldExtractorPostProcessedField();
	private static final FieldExtractorConfiguration FIELD_EXTRACTOR_CONFIGURATION = new FieldExtractorConfiguration();

	private final Core core;

	private final TemplateParser templateParser;
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

		templateParser = core.getTemplateParser();
		final ConfigurationParser configurationParser = core.getConfigurationParser();
		loaderConfiguration = configurationParser.getLoaderConfiguration();
	}


	/**
	 * Description of all the loaded templates.
	 *
	 * @return	The list of descriptions.
	 * @throws TemplateException	If a template error occurs.
	 */
	public List<Map<String, Object>> describeParsing() throws FieldException{
		final Collection<Template<?>> templates = new HashSet<>(templateParser.getTemplates());
		return describeEntities(templates, template -> describeMessage(template, MESSAGE_EXTRACTOR_FULL_TEMPLATE, FIELD_EXTRACTOR_TEMPLATE));
	}

	/**
	 * Description of a single template annotated with {@link TemplateHeader}.
	 *
	 * @param templateClass	Template class to be described.
	 * @return	The description.
	 * @throws AnnotationException	If an annotation error occurs.
	 * @throws TemplateException	If a template error occurs.
	 */
	public Map<String, Object> describeParsing(final Class<?> templateClass) throws FieldException{
		return describeEntity(TemplateHeader.class, templateClass, templateParser::extractTemplate,
			template -> describeMessage(template, MESSAGE_EXTRACTOR_FULL_TEMPLATE, FIELD_EXTRACTOR_TEMPLATE));
	}

	/**
	 * Description of all the templates in the given package annotated with {@link TemplateHeader}.
	 *
	 * @param templateClasses	Classes to be used ase starting point from which to load annotated classes.
	 * @return	The list of descriptions.
	 * @throws AnnotationException	If an annotation error occurs.
	 * @throws TemplateException	If a template error occurs.
	 */
	public List<Map<String, Object>> describeParsing(final Class<?>... templateClasses) throws FieldException{
		return describeEntities(TemplateHeader.class, templateClasses, templateParser::extractTemplate,
			template -> describeMessage(template, MESSAGE_EXTRACTOR_FULL_TEMPLATE, FIELD_EXTRACTOR_TEMPLATE));
	}


	/**
	 * Description of all the loaded templates.
	 *
	 * @return	The list of descriptions.
	 * @throws TemplateException	If a template error occurs.
	 */
	public List<Map<String, Object>> describeTemplate() throws FieldException{
		final Collection<Template<?>> configurations = new HashSet<>(templateParser.getTemplates());
		return describeEntities(configurations, template -> describeMessage(template, MESSAGE_EXTRACTOR_BASIC_TEMPLATE,
			FIELD_EXTRACTOR_TEMPLATE));
	}

	/**
	 * Description of a single template annotated with {@link TemplateHeader}.
	 *
	 * @param templateClass	Template class to be described.
	 * @return	The description.
	 * @throws AnnotationException	If an annotation error occurs.
	 * @throws TemplateException	If a template error occurs.
	 */
	public Map<String, Object> describeTemplate(final Class<?> templateClass) throws FieldException{
		return describeEntity(TemplateHeader.class, templateClass, templateParser::extractTemplate,
			template -> describeMessage(template, MESSAGE_EXTRACTOR_BASIC_TEMPLATE, FIELD_EXTRACTOR_TEMPLATE));
	}

	/**
	 * Description of all the templates in the given package annotated with {@link TemplateHeader}.
	 *
	 * @param templateClasses	Classes to be used ase starting point from which to load annotated classes.
	 * @return	The list of descriptions.
	 * @throws AnnotationException	If an annotation error occurs.
	 * @throws TemplateException	If a template error occurs.
	 */
	public List<Map<String, Object>> describeTemplate(final Class<?>... templateClasses) throws FieldException{
		return describeEntities(TemplateHeader.class, templateClasses, templateParser::extractTemplate,
			template -> describeMessage(template, MESSAGE_EXTRACTOR_BASIC_TEMPLATE, FIELD_EXTRACTOR_TEMPLATE));
	}


	/**
	 * Description of all the loaded configuration.
	 *
	 * @return	The list of descriptions.
	 * @throws ConfigurationException	If a configuration error occurs.
	 */
	public List<Map<String, Object>> describeConfiguration() throws FieldException{
		final Collection<ConfigurationMessage<?>> configurations = new HashSet<>(loaderConfiguration.getConfigurations());
		return describeEntities(configurations, configuration -> describeMessage(configuration, MESSAGE_EXTRACTOR_CONFIGURATION,
			FIELD_EXTRACTOR_CONFIGURATION));
	}

	/**
	 * Description of a single configuration annotated with {@link ConfigurationHeader}.
	 *
	 * @param configurationClass	Configuration class to be described.
	 * @return	The description.
	 * @throws AnnotationException	If an annotation error occurs.
	 * @throws ConfigurationException	If a configuration error occurs.
	 * @throws EncodeException	If a configuration cannot be retrieved.
	 */
	public Map<String, Object> describeConfiguration(final Class<?> configurationClass) throws FieldException, EncodeException{
		final ThrowingFunction<Class<?>, ConfigurationMessage<?>, EncodeException> extractor = cls -> {
			final ConfigurationHeader header = configurationClass.getAnnotation(ConfigurationHeader.class);
			return loaderConfiguration.getConfiguration(header.shortDescription());
		};
		return describeEntity(ConfigurationHeader.class, configurationClass, extractor,
			configuration -> describeMessage(configuration, MESSAGE_EXTRACTOR_CONFIGURATION, FIELD_EXTRACTOR_CONFIGURATION));
	}

	/**
	 * Description of all the configurations in the given package annotated with {@link ConfigurationHeader}.
	 *
	 * @param configurationClasses	Classes to be used ase starting point from which to load annotated classes.
	 * @return	The list of descriptions.
	 * @throws AnnotationException	If an annotation error occurs.
	 * @throws ConfigurationException	If a configuration error occurs.
	 */
	public List<Map<String, Object>> describeConfiguration(final Class<?>... configurationClasses) throws FieldException{
		return describeEntities(ConfigurationHeader.class, configurationClasses, loaderConfiguration::extractConfiguration,
			configuration -> describeMessage(configuration, MESSAGE_EXTRACTOR_CONFIGURATION, FIELD_EXTRACTOR_CONFIGURATION));
	}


	/**
	 * Returns a description of a message.
	 *
	 * @param message	The message to describe.
	 * @param messageExtractor	The message extractor to use.
	 * @param fieldExtractor	The field extractor to use.
	 * @return	A map containing the description of the message.
	 * @param <M>	The type of the message.
	 * @param <F>	The type of the message.
	 */
	private <M, F> Map<String, Object> describeMessage(final M message, final MessageExtractor<M, ? extends Annotation, F> messageExtractor,
			final FieldExtractor<F> fieldExtractor){
		final Map<String, Object> description = new HashMap<>(6);

		describeContext(description);

		final Annotation header = messageExtractor.getHeader(message);
		AnnotationDescriptorHelper.extractObjectParameters(header, header.annotationType(), description, DescriberKey.HEADER.toString());

		describeRawMessage(message, messageExtractor, fieldExtractor, description);
		return Collections.unmodifiableMap(description);
	}

	/**
	 * Description of a single annotated class.
	 *
	 * @param boundClass	Generic bound class to be described.
	 * @return	The description.
	 */
	public static Map<String, Object> describeRawMessage(final Class<?> boundClass){
		final Map<String, Object> description = new HashMap<>(6);
		try{
			final Template<?> entity = Template.create(boundClass);
			describeRawMessage(entity, MESSAGE_EXTRACTOR_BASIC_TEMPLATE, FIELD_EXTRACTOR_TEMPLATE, description);
		}
		catch(final AnnotationException ignored){
			description.put(DescriberKey.TEMPLATE.toString(), boundClass.getName());
		}
		return Collections.unmodifiableMap(description);
	}

	/**
	 * Describes a raw message by extracting various information from it and creating a description map.
	 * <p>The description includes the message type name, fields, evaluated fields, and post-processed fields.</p>
	 *
	 * @param message	The message to describe.
	 * @param messageExtractor	The message extractor to use.
	 * @param fieldExtractor	The field extractor to use.
	 * @param rootDescription	The map where the description will be populated.
	 * @param <M>	The type of the message.
	 * @param <F>	The type of the message.
	 */
	private static <M, F> void describeRawMessage(final M message, final MessageExtractor<M, ?, F> messageExtractor,
			final FieldExtractor<F> fieldExtractor, final Map<String, Object> rootDescription){
		final DescriberKey messageKey = (messageExtractor instanceof MessageExtractorBasicTemplate
			? DescriberKey.TEMPLATE
			: DescriberKey.CONFIGURATION);
		putIfNotEmpty(messageKey, messageExtractor.getTypeName(message), rootDescription);

		putIfNotEmpty(DescriberKey.FIELDS, AnnotationDescriptorHelper.describeFields(messageExtractor.getFields(message), fieldExtractor),
			rootDescription);
		putIfNotEmpty(DescriberKey.EVALUATED_FIELDS, AnnotationDescriptorHelper.describeFields(messageExtractor.getEvaluatedFields(message),
			FIELD_EXTRACTOR_EVALUATED_FIELD), rootDescription);
		putIfNotEmpty(DescriberKey.POST_PROCESSED_FIELDS, AnnotationDescriptorHelper.describeFields(
			messageExtractor.getPostProcessedFields(message), FIELD_EXTRACTOR_POST_PROCESSED_FIELD), rootDescription);
	}

	/**
	 * Describes the entities by mapping them to a list of maps using the provided mapper function.
	 * <p>Each entity is transformed into a map representation where the keys are the field names and the values are the corresponding field
	 * values.</p>
	 *
	 * @param entities	The collection of entities to be described.
	 * @param mapper	A function that takes an entity of type T and returns its corresponding map representation.
	 * @return	The list of descriptions for the entities.
	 * @throws FieldException	If a field exception occurs during the mapping process.
	 * @param <T>	The type of the entities.
	 */
	private static <T> List<Map<String, Object>> describeEntities(final Collection<T> entities,
			final ThrowingFunction<T, Map<String, Object>, FieldException> mapper) throws FieldException{
		final List<Map<String, Object>> descriptions = new ArrayList<>(entities.size());
		for(final T entity : entities)
			descriptions.add(mapper.apply(entity));
		return Collections.unmodifiableList(descriptions);
	}

	/**
	 * Describes the entities by mapping them to a list of maps using the provided mapper function.
	 *
	 * @param <T>	The type of the entities.
	 * @param <E>	The type of the exception.
	 * @param annotationClass	The annotation class to check for annotated entities.
	 * @param entitiesClass	The array of entity classes to be described.
	 * @param extractor	The function to extract the entity.
	 * @param mapper	The function that maps the entity to its corresponding map representation.
	 * @return	The list of descriptions for the entities.
	 * @throws FieldException	If a field exception occurs during the mapping process.
	 * @throws E	If any other exception occurs during the extraction or mapping process.
	 */
	private static <T, E extends Exception> List<Map<String, Object>> describeEntities(final Class<? extends Annotation> annotationClass,
			final Class<?>[] entitiesClass, final ThrowingFunction<Class<?>, T, E> extractor,
			final ThrowingFunction<T, Map<String, Object>, FieldException> mapper) throws FieldException, E{
		final List<Map<String, Object>> description = new ArrayList<>(entitiesClass.length);
		for(final Class<?> entityClass : entitiesClass)
			if(entityClass.isAnnotationPresent(annotationClass)){
				final T entity = extractor.apply(entityClass);
				description.add(mapper.apply(entity));
			}
		return Collections.unmodifiableList(description);
	}

	/**
	 * Describes an entity by mapping it to a map representation using the provided mapper function.
	 *
	 * @param annotationClass	The annotation class to check for the entity.
	 * @param entityClass	The entity class to be described.
	 * @param extractor	The function to extract the entity.
	 * @param mapper	The function that maps the entity to its corresponding map representation.
	 * @return	The map representation of the entity.
	 * @throws FieldException	If a field exception occurs during the mapping process.
	 * @throws E	If any other exception occurs during the extraction or mapping process.
	 * @param <T>	The type of the entity.
	 * @param <E>	The type of the exception.
	 */
	private static <T, E extends Exception> Map<String, Object> describeEntity(final Class<? extends Annotation> annotationClass,
			final Class<?> entityClass, final ThrowingFunction<Class<?>, T, E> extractor,
			final ThrowingFunction<T, Map<String, Object>, FieldException> mapper) throws FieldException, E{
		if(!entityClass.isAnnotationPresent(annotationClass))
			throw AnnotationException.create("Entity {} didn't have the `{}` annotation", entityClass.getSimpleName(),
				annotationClass.getSimpleName());

		final T entity = extractor.apply(entityClass);
		return Collections.unmodifiableMap(mapper.apply(entity));
	}

	/**
	 * Describes the context by populating the given map with the context information.
	 *
	 * @param description	The map where the context description will be populated.
	 */
	private void describeContext(final Map<String, Object> description){
		final Map<String, Object> ctx = new HashMap<>(core.getContext());
		ctx.remove(ContextHelper.CONTEXT_SELF);
		ctx.remove(ContextHelper.CONTEXT_CHOICE_PREFIX);
		putIfNotEmpty(DescriberKey.CONTEXT, Collections.unmodifiableMap(ctx), description);
	}


	@FunctionalInterface
	private interface ThrowingFunction<T, R, E extends Exception>{
		R apply(T t) throws E;
	}

}
