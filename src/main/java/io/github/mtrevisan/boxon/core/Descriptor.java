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
	public List<Map<String, Object>> describeTemplate() throws TemplateException{
		final Collection<Template<?>> templates = new HashSet<>(loaderTemplate.getTemplates());

		final List<Map<String, Object>> description = new ArrayList<>(templates.size());
		for(final Template<?> template : templates)
			description.add(describeTemplate(template));
		return Collections.unmodifiableList(description);
	}

	/**
	 * Description of a single template annotated with {@link MessageHeader}.
	 *
	 * @param templateClass	Template class to be described.
	 * @return	The list of descriptions.
	 * @throws AnnotationException	If an annotation is not well formatted.
	 * @throws TemplateException	If a template is not well formatted.
	 */
	public Map<String, Object> describeTemplate(final Class<?> templateClass) throws AnnotationException, TemplateException{
		if(templateClass.isAnnotationPresent(MessageHeader.class)){
			final Template<?> template = loaderTemplate.extractTemplate(templateClass);
			return describeTemplate(template);
		}

		throw AnnotationException.create("Template {} didn't have the `MessageHeader` annotation",
			templateClass.getSimpleName());
	}

	/**
	 * Description of all the templates in the given package annotated with {@link MessageHeader}.
	 *
	 * @param templateClasses	Classes to be used ase starting point from which to load annotated classes.
	 * @return	The list of descriptions.
	 * @throws AnnotationException	If an annotation is not well formatted.
	 * @throws TemplateException	If a template is not well formatted.
	 */
	public List<Map<String, Object>> describeTemplate(final Class<?>... templateClasses) throws AnnotationException, TemplateException{
		final int length = templateClasses.length;
		for(int i = 0; i < length; i ++){
			final Class<?> templateClass = templateClasses[i];

			if(!templateClass.isAnnotationPresent(MessageHeader.class))
				throw AnnotationException.create("Template {} didn't have the `MessageHeader` annotation",
					templateClass.getSimpleName());
		}

		final List<Map<String, Object>> description = new ArrayList<>(length);
		for(int i = 0; i < length; i ++){
			final Class<?> templateClass = templateClasses[i];

			final Template<?> template = loaderTemplate.extractTemplate(templateClass);
			description.add(describeTemplate(template));
		}
		return Collections.unmodifiableList(description);
	}

	private Map<String, Object> describeTemplate(final Template<?> template) throws TemplateException{
		final Map<String, Object> description = new HashMap<>(3);
		description.put(DescriberKey.TEMPLATE.toString(), template.getType().getName());
		describeHeader(template.getHeader(), description);
		describeBoundedFields(template.getBoundedFields(), description);
		describeContext(description);
		return Collections.unmodifiableMap(description);
	}

	private static void describeHeader(final MessageHeader header, final Map<String, Object> description){
		final Map<String, Object> headerDescription = new HashMap<>(3);
		AnnotationDescriptor.putIfNotEmpty(DescriberKey.HEADER_START, Arrays.toString(header.start()), headerDescription);
		AnnotationDescriptor.putIfNotEmpty(DescriberKey.HEADER_END, header.end(), headerDescription);
		AnnotationDescriptor.putIfNotEmpty(DescriberKey.HEADER_CHARSET, header.charset(), headerDescription);
		description.put(DescriberKey.HEADER.toString(), headerDescription);
	}

	private static void describeBoundedFields(final List<BoundedField> fields, final Map<String, Object> description) throws TemplateException{
		final int length = fields.size();
		final Collection<Map<String, Object>> fieldsDescription = new ArrayList<>(length);
		for(int i = 0; i < length; i ++)
			describeField(fields.get(i), fieldsDescription);
		description.put(DescriberKey.FIELDS.toString(), fieldsDescription);
	}

	private static void describeField(final BoundedField field, final Collection<Map<String, Object>> fieldsDescription)
			throws TemplateException{
		AnnotationDescriptor.describeSkips(field.getSkips(), fieldsDescription);

		final Map<String, Object> fieldDescription = new HashMap<>(13);
		AnnotationDescriptor.putIfNotEmpty(DescriberKey.FIELD_NAME, field.getFieldName(), fieldDescription);
		AnnotationDescriptor.putIfNotEmpty(DescriberKey.FIELD_TYPE, field.getFieldType().getName(), fieldDescription);
		final Annotation binding = field.getBinding();
		final Class<? extends Annotation> annotationType = binding.annotationType();
		AnnotationDescriptor.putIfNotEmpty(DescriberKey.ANNOTATION_TYPE, binding.annotationType().getName(), fieldDescription);

		//extract binding descriptor
		final AnnotationDescriptor descriptor = AnnotationDescriptor.fromAnnotation(binding);
		if(descriptor == null)
			throw TemplateException.create("Cannot extract descriptor for this annotation: {}", annotationType.getSimpleName());

		descriptor.describe(binding, fieldDescription);

		fieldsDescription.add(fieldDescription);
	}

	private void describeContext(final Map<String, Object> description){
		final Map<String, Object> ctx = new HashMap<>(core.getContext());
		ctx.remove(ContextHelper.CONTEXT_SELF);
		ctx.remove(ContextHelper.CONTEXT_CHOICE_PREFIX);
		if(!ctx.isEmpty())
			description.put(DescriberKey.CONTEXT.toString(), ctx);
	}


	/**
	 * Description of all the loaded configuration.
	 *
	 * @return	The list of descriptions.
	 * @throws ConfigurationException	If a configuration is not well formatted.
	 */
	public List<Map<String, Object>> describeConfiguration() throws ConfigurationException{
		final Collection<ConfigurationMessage<?>> configurations = new HashSet<>(loaderConfiguration.getConfigurations());

		final List<Map<String, Object>> description = new ArrayList<>(configurations.size());
		for(final ConfigurationMessage<?> configuration : configurations)
			description.add(describeConfiguration(configuration));
		return Collections.unmodifiableList(description);
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
	public Map<String, Object> describeConfiguration(final Class<?> configurationClass) throws AnnotationException, ConfigurationException,
			EncodeException{
		final ConfigurationHeader header = configurationClass.getAnnotation(ConfigurationHeader.class);
		if(header != null){
			final ConfigurationMessage<?> configuration = loaderConfiguration.getConfiguration(header.shortDescription());
			return describeConfiguration(configuration);
		}

		throw AnnotationException.create("Configuration {} didn't have the `ConfigurationHeader` annotation",
			configurationClass.getSimpleName());
	}

	/**
	 * Description of all the configurations in the given package annotated with {@link ConfigurationHeader}.
	 *
	 * @param configurationClasses	Classes to be used ase starting point from which to load annotated classes.
	 * @return	The list of descriptions.
	 * @throws AnnotationException	If an annotation is not well formatted.
	 * @throws ConfigurationException	If a configuration is not well formatted.
	 * @throws EncodeException	If a configuration cannot be retrieved.
	 */
	public List<Map<String, Object>> describeConfiguration(final Class<?>... configurationClasses) throws AnnotationException,
			ConfigurationException, EncodeException{
		final int length = configurationClasses.length;
		for(int i = 0; i < length; i ++){
			final Class<?> configurationClass = configurationClasses[i];

			if(!configurationClass.isAnnotationPresent(ConfigurationHeader.class))
				throw AnnotationException.create("Configuration {} didn't have the `ConfigurationHeader` annotation",
					configurationClass.getSimpleName());
		}

		final List<Map<String, Object>> description = new ArrayList<>(length);
		for(int i = 0; i < length; i ++){
			final Class<?> configurationClass = configurationClasses[i];

			final ConfigurationHeader header = configurationClass.getAnnotation(ConfigurationHeader.class);

			final ConfigurationMessage<?> configuration = loaderConfiguration.getConfiguration(header.shortDescription());
			description.add(describeConfiguration(configuration));
		}
		return Collections.unmodifiableList(description);
	}

	private Map<String, Object> describeConfiguration(final ConfigurationMessage<?> configuration) throws ConfigurationException{
		final Map<String, Object> description = new HashMap<>(3);
		description.put(ConfigurationKey.CONFIGURATION.toString(), configuration.getType().getName());
		describeHeader(configuration.getHeader(), description);
		describeConfigFields(configuration.getConfigurationFields(), description);
		return Collections.unmodifiableMap(description);
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

	private static void describeConfigFields(final List<ConfigField> fields, final Map<String, Object> description) throws ConfigurationException{
		final int length = fields.size();
		final Collection<Map<String, Object>> fieldsDescription = new ArrayList<>(length);
		for(int i = 0; i < length; i ++)
			describeField(fields.get(i), fieldsDescription);
		description.put(DescriberKey.FIELDS.toString(), fieldsDescription);
	}

	private static void describeField(final ConfigField field, final Collection<Map<String, Object>> fieldsDescription)
			throws ConfigurationException{
		AnnotationDescriptor.describeSkips(field.getSkips(), fieldsDescription);

		final Map<String, Object> fieldDescription = new HashMap<>(13);
		AnnotationDescriptor.putIfNotEmpty(DescriberKey.FIELD_NAME, field.getFieldName(), fieldDescription);
		AnnotationDescriptor.putIfNotEmpty(DescriberKey.FIELD_TYPE, field.getFieldType().getName(), fieldDescription);
		final Annotation binding = field.getBinding();
		final Class<? extends Annotation> annotationType = binding.annotationType();
		AnnotationDescriptor.putIfNotEmpty(DescriberKey.ANNOTATION_TYPE, binding.annotationType().getName(), fieldDescription);

		//extract binding descriptor
		final AnnotationDescriptor descriptor = AnnotationDescriptor.fromAnnotation(binding);
		if(descriptor == null)
			throw ConfigurationException.create("Cannot extract descriptor for this annotation: {}", annotationType.getSimpleName());

		descriptor.describe(binding, fieldDescription);

		fieldsDescription.add(fieldDescription);
	}

}
