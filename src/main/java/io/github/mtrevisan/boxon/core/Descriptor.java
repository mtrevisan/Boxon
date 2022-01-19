/*
 * Copyright (c) 2020-2022 Mauro Trevisan
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
import io.github.mtrevisan.boxon.core.codecs.internal.LoaderTemplate;
import io.github.mtrevisan.boxon.core.codecs.internal.TemplateParser;
import io.github.mtrevisan.boxon.exceptions.AnnotationException;
import io.github.mtrevisan.boxon.exceptions.TemplateException;
import io.github.mtrevisan.boxon.internal.AnnotationDescriptor;
import io.github.mtrevisan.boxon.internal.BoundedField;
import io.github.mtrevisan.boxon.internal.Template;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * Declarative descriptor for binary encoded data.
 */
@SuppressWarnings({"WeakerAccess", "unused"})
public final class Descriptor{

	private final LoaderTemplate loaderTemplate;
	private final Map<String, Object> backupContext;


	/**
	 * Create an empty descriptor.
	 *
	 * @param parserCore	The parser core.
	 * @return	A basic empty descriptor.
	 */
	public static Descriptor create(final ParserCore parserCore){
		return new Descriptor(parserCore);
	}


	private Descriptor(final ParserCore parserCore){
		final TemplateParser templateParser = parserCore.getTemplateParser();
		loaderTemplate = templateParser.getLoaderTemplate();
		backupContext = templateParser.getBackupContext();
	}


	/**
	 * Description of all the loaded templates.
	 *
	 * @return	The list of descriptions.
	 * @throws TemplateException	If a template is not well formatted.
	 */
	public List<Map<String, Object>> describeTemplates() throws TemplateException{
		final Collection<Template<?>> templates = loaderTemplate.getTemplates();
		final List<Map<String, Object>> description = new ArrayList<>(templates.size());
		for(final Template<?> template : templates)
			description.add(describeTemplate(template));
		return description;
	}

	/**
	 * Description of all the templates in the given package annotated with {@link MessageHeader}.
	 *
	 * @param templateClasses	Classes to be used ase starting point from which to load annotated classes.
	 * @return	The list of descriptions.
	 * @throws AnnotationException	If an annotation is not well formatted.
	 * @throws TemplateException	If a template is not well formatted.
	 */
	public List<Map<String, Object>> describeTemplates(final Class<?>... templateClasses) throws AnnotationException, TemplateException{
		final List<Map<String, Object>> description = new ArrayList<>(templateClasses.length);
		for(int i = 0; i < templateClasses.length; i ++){
			final Class<?> templateClass = templateClasses[i];
			if(templateClass.isAnnotationPresent(MessageHeader.class)){
				final Template<?> template = loaderTemplate.extractTemplate(templateClass);
				description.add(describeTemplate(template));
			}
		}
		return description;
	}

	private Map<String, Object> describeTemplate(final Template<?> template) throws TemplateException{
		final Map<String, Object> description = new HashMap<>(3);
		describeHeader(template.getHeader(), description);
		describeFields(template.getBoundedFields(), description);
		describeContext(description);
		return description;
	}

	private static void describeHeader(final MessageHeader header, final Map<String, Object> description){
		final Map<String, Object> headerDescription = new HashMap<>(3);
		AnnotationDescriptor.putIfNotEmpty(DescriberKey.HEADER_START, header.start(), headerDescription);
		AnnotationDescriptor.putIfNotEmpty(DescriberKey.HEADER_END, header.end(), headerDescription);
		AnnotationDescriptor.putIfNotEmpty(DescriberKey.HEADER_CHARSET, header.charset(), headerDescription);
		description.put(DescriberKey.HEADER.toString(), headerDescription);
	}

	private static void describeFields(final List<BoundedField> fields, final Map<String, Object> description) throws TemplateException{
		final Collection<Map<String, Object>> fieldsDescription = new ArrayList<>(0);
		for(int i = 0; i < fields.size(); i ++)
			describeField(fields.get(i), fieldsDescription);
		description.put(DescriberKey.FIELDS.toString(), fieldsDescription);
	}

	private static void describeField(final BoundedField field, final Collection<Map<String, Object>> fieldsDescription)
			throws TemplateException{
		AnnotationDescriptor.describeSkips(field.getSkips(), fieldsDescription);

		final Map<String, Object> fieldDescription = new HashMap<>(13);
		AnnotationDescriptor.putIfNotEmpty(DescriberKey.FIELD_NAME, field.getFieldName(), fieldDescription);
		AnnotationDescriptor.putIfNotEmpty(DescriberKey.FIELD_TYPE, field.getFieldType(), fieldDescription);
		final Annotation binding = field.getBinding();
		final Class<? extends Annotation> annotationType = binding.annotationType();
		AnnotationDescriptor.putIfNotEmpty(DescriberKey.ANNOTATION_TYPE, binding.annotationType().getSimpleName(), fieldDescription);

		//extract binding descriptor
		final AnnotationDescriptor descriptor = AnnotationDescriptor.fromAnnotation(binding);
		if(descriptor == null)
			throw TemplateException.create("Cannot extract descriptor for this annotation: {}", annotationType.getSimpleName());

		descriptor.describe(binding, fieldDescription);

		fieldsDescription.add(fieldDescription);
	}

	private void describeContext(final Map<String, Object> description){
		description.put(DescriberKey.CONTEXT.toString(), backupContext);
	}

}
