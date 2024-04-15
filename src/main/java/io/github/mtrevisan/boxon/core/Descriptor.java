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
import io.github.mtrevisan.boxon.annotations.bindings.ConverterChoices;
import io.github.mtrevisan.boxon.annotations.bindings.ObjectChoices;
import io.github.mtrevisan.boxon.annotations.bindings.ObjectChoicesList;
import io.github.mtrevisan.boxon.annotations.configurations.AlternativeSubField;
import io.github.mtrevisan.boxon.annotations.configurations.CompositeSubField;
import io.github.mtrevisan.boxon.annotations.configurations.ConfigurationHeader;
import io.github.mtrevisan.boxon.annotations.configurations.NullEnum;
import io.github.mtrevisan.boxon.annotations.converters.Converter;
import io.github.mtrevisan.boxon.annotations.converters.NullConverter;
import io.github.mtrevisan.boxon.annotations.validators.NullValidator;
import io.github.mtrevisan.boxon.annotations.validators.Validator;
import io.github.mtrevisan.boxon.core.helpers.configurations.ConfigurationMessage;
import io.github.mtrevisan.boxon.core.helpers.descriptors.AnnotationDescriptor;
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
import io.github.mtrevisan.boxon.core.helpers.extractors.SkipParams;
import io.github.mtrevisan.boxon.core.helpers.templates.Template;
import io.github.mtrevisan.boxon.core.keys.ConfigurationKey;
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
import io.github.mtrevisan.boxon.helpers.JavaHelper;
import io.github.mtrevisan.boxon.helpers.StringHelper;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
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


	private <M, F> Map<String, Object> describeMessage(final M message, final MessageExtractor<M, ? extends Annotation, F> messageExtractor,
			final FieldExtractor<F> fieldExtractor) throws FieldException{
		final Map<String, Object> description = new HashMap<>(6);
		describeRawMessage(message, messageExtractor, fieldExtractor, description);
		putIfNotEmpty(DescriberKey.HEADER, describeHeader(messageExtractor.getHeader(message)), description);
		describeContext(description);
		return Collections.unmodifiableMap(description);
	}

	/**
	 * Description of a single annotated class.
	 *
	 * @param boundClass	Generic bound class to be described.
	 * @return	The description.
	 * @throws AnnotationException	If an annotation error occurs and the template cannot be created.
	 * @throws FieldException	If the descriptor of a field is not found.
	 */
	public static Map<String, Object> describeRawMessage(final Class<?> boundClass) throws AnnotationException, FieldException{
		final Map<String, Object> description = new HashMap<>(6);
		final Template<?> entity = Template.create(boundClass);
		describeRawMessage(entity, MESSAGE_EXTRACTOR_BASIC_TEMPLATE, FIELD_EXTRACTOR_TEMPLATE, description);
		return Collections.unmodifiableMap(description);
	}

	private static <M, F> void describeRawMessage(final M message, final MessageExtractor<M, ?, F> messageExtractor,
			final FieldExtractor<F> fieldExtractor, final Map<String, Object> rootDescription) throws FieldException{
		final DescriberKey messageKey = (messageExtractor instanceof MessageExtractorBasicTemplate
			? DescriberKey.TEMPLATE
			: DescriberKey.CONFIGURATION);
		putIfNotEmpty(messageKey, messageExtractor.getTypeName(message), rootDescription);

		putIfNotEmpty(DescriberKey.FIELDS, describeFields(messageExtractor.getFields(message), fieldExtractor), rootDescription);
		putIfNotEmpty(DescriberKey.EVALUATED_FIELDS, describeFields(messageExtractor.getEvaluatedFields(message),
			FIELD_EXTRACTOR_EVALUATED_FIELD), rootDescription);
		putIfNotEmpty(DescriberKey.POST_PROCESSED_FIELDS, describeFields(messageExtractor.getPostProcessedFields(message),
			FIELD_EXTRACTOR_POST_PROCESSED_FIELD), rootDescription);
	}

	private static Map<String, Object> describeHeader(final Annotation header){
		final Map<String, Object> headerDescription = new HashMap<>(1);
		final AnnotationDescriptor annotationDescriptor = AnnotationDescriptor.fromAnnotation(header);
		annotationDescriptor.describe(header, headerDescription);
		return Collections.unmodifiableMap(headerDescription);
	}

	private static <T> List<Map<String, Object>> describeEntities(final Collection<T> entities,
			final ThrowingFunction<T, Map<String, Object>, FieldException> mapper) throws FieldException{
		final List<Map<String, Object>> descriptions = new ArrayList<>(entities.size());
		for(final T entity : entities)
			descriptions.add(mapper.apply(entity));
		return Collections.unmodifiableList(descriptions);
	}

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

	private static <T, E extends Exception> Map<String, Object> describeEntity(final Class<? extends Annotation> annotationClass,
			final Class<?> entityClass, final ThrowingFunction<Class<?>, T, E> extractor,
			final ThrowingFunction<T, Map<String, Object>, FieldException> mapper) throws FieldException, E{
		if(!entityClass.isAnnotationPresent(annotationClass))
			throw AnnotationException.create("Entity {} didn't have the `{}` annotation", entityClass.getSimpleName(),
				annotationClass.getSimpleName());

		final T entity = extractor.apply(entityClass);
		return Collections.unmodifiableMap(mapper.apply(entity));
	}

	private void describeContext(final Map<String, Object> description){
		final Map<String, Object> ctx = new HashMap<>(core.getContext());
		ctx.remove(ContextHelper.CONTEXT_SELF);
		ctx.remove(ContextHelper.CONTEXT_CHOICE_PREFIX);
		putIfNotEmpty(DescriberKey.CONTEXT, Collections.unmodifiableMap(ctx), description);
	}

	private static <F> Collection<Map<String, Object>> describeFields(final List<F> fields, final FieldExtractor<F> fieldExtractor)
			throws FieldException{
		final int length = JavaHelper.sizeOrZero(fields);
		final Collection<Map<String, Object>> fieldsDescription = new ArrayList<>(length);
//		final Collection<Map<String, Object>> fieldsDescription2 = new ArrayList<>(length);
		for(int i = 0; i < length; i ++){
			final F field = fields.get(i);

//FIXME
extractAnnotationParameters(field, fieldExtractor, fieldsDescription);

//			AnnotationDescriptor.describeSkips(field, fieldExtractor, fieldsDescription);
//
//			final Annotation binding = fieldExtractor.getBinding(field);
//			final String fieldName = fieldExtractor.getFieldName(field);
//			final Class<?> fieldType = fieldExtractor.getFieldType(field);
//			describeField(binding, fieldName, fieldType, fieldsDescription);
		}
		return Collections.unmodifiableCollection(fieldsDescription);
	}

	private static <F> void extractAnnotationParameters(final F field, final FieldExtractor<F> fieldExtractor,
			final Collection<Map<String, Object>> fieldsDescription){
		final SkipParams[] skips = fieldExtractor.getSkips(field);
		final Annotation binding = fieldExtractor.getBinding(field);
		final String fieldName = fieldExtractor.getFieldName(field);
		final Class<?> fieldType = fieldExtractor.getFieldType(field);

		final Map<String, Object> fieldDescription = new HashMap<>(3);
		putIfNotEmpty(DescriberKey.FIELD_NAME, fieldName, fieldDescription);
		putIfNotEmpty(DescriberKey.FIELD_TYPE, fieldType.getName(), fieldDescription);
		putIfNotEmpty(DescriberKey.ANNOTATION_TYPE, binding.annotationType().getName(), fieldDescription);

		for(int i = 0, length = JavaHelper.sizeOrZero(skips); i < length; i ++){
			final SkipParams skip = skips[i];

			final Map<String, Object> skipDescription = extractObjectParameters(skip, skip.getClass(), null);
			fieldsDescription.add(skipDescription);
		}

		extractObjectParameters(binding, binding.annotationType(), fieldDescription);

		fieldsDescription.add(Collections.unmodifiableMap(fieldDescription));
	}

	private static Map<String, Object> extractObjectParameters(final Object obj, final Class<?> objType,
			Map<String, Object> fieldDescription){
		final Method[] methods = objType.getDeclaredMethods();
		final int methodsLength = methods.length;
		if(fieldDescription == null)
			fieldDescription = new HashMap<>(methodsLength);
		for(int i = 0; i < methodsLength; i ++){
			final Method method = methods[i];
			if(method.getParameters().length > 0)
				continue;

			try{
				final Object value = method.invoke(obj);

				putIfNotEmpty2(method.getName(), value, fieldDescription);
			}
			catch(final Exception e){
				e.printStackTrace();
			}
		}

		return Collections.unmodifiableMap(fieldDescription);
	}

	/**
	 * Put the pair key-value into the given map if the value is not {@code null} or empty string.
	 *
	 * @param key	The key.
	 * @param value	The value.
	 * @param rootDescription	The map in which to load the key-value pair.
	 */
	public static void putIfNotEmpty2(final String key, final Object value, final Map<String, Object> rootDescription){
		if(value == null)
			return;

		if(value instanceof final Class<?> cls){
			if(Validator.class.isAssignableFrom(cls)){
				if(cls != NullValidator.class)
					rootDescription.put(key, cls);
			}
			else if(Converter.class.isAssignableFrom(cls)){
				if(cls != NullConverter.class)
					rootDescription.put(key, cls);
			}
			else if(!cls.isEnum() || cls != NullEnum.class)
				rootDescription.put(key, cls.getName());
		}
		else if(value.getClass().isArray()){
			final Class<?> componentType = value.getClass()
				.getComponentType();
			if(componentType == AlternativeSubField.class)
				AnnotationDescriptorHelper.describeAlternatives((AlternativeSubField[])value, rootDescription);
			else if(componentType == CompositeSubField.class)
				AnnotationDescriptorHelper.describeComposite((CompositeSubField[])value, rootDescription);
			else
				System.out.println();
		}
		else if(value instanceof final ObjectChoices choices)
			AnnotationDescriptorHelper.describeChoices(choices, rootDescription);
		else if(value instanceof final ObjectChoicesList choices)
			AnnotationDescriptorHelper.describeChoices(choices, rootDescription);
		else if(value instanceof final ConverterChoices converter)
			AnnotationDescriptorHelper.describeAlternatives(converter.alternatives(), rootDescription);
		else if(!(value instanceof final String v && StringHelper.isBlank(v))
			&& !(value instanceof final Collection<?> c && c.isEmpty())
		)
			rootDescription.put(key, value);
	}

	private static void describeField(final Annotation binding, final String fieldName, final Class<?> fieldType,
			final Collection<Map<String, Object>> fieldsDescription) throws FieldException{
		final Map<String, Object> fieldDescription = createFieldDescription(fieldName, fieldType.getName(),
			binding.annotationType());

		//extract binding descriptor
		final AnnotationDescriptor descriptor = AnnotationDescriptor.checkAndGetDescriptor(binding);
		descriptor.describe(binding, fieldDescription);

		fieldsDescription.add(Collections.unmodifiableMap(fieldDescription));
	}

	private static Map<String, Object> createFieldDescription(final String fieldName, final String name,
			final Class<? extends Annotation> annotationType){
		final Map<String, Object> fieldDescription = new HashMap<>(3);
		putIfNotEmpty(DescriberKey.FIELD_NAME, fieldName, fieldDescription);
		putIfNotEmpty(DescriberKey.FIELD_TYPE, name, fieldDescription);
		putIfNotEmpty(DescriberKey.ANNOTATION_TYPE, annotationType.getName(), fieldDescription);
		return fieldDescription;
	}


	@FunctionalInterface
	private interface ThrowingFunction<T, R, E extends Exception>{
		R apply(T t) throws E;
	}

}
