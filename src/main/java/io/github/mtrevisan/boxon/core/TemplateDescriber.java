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
import io.github.mtrevisan.boxon.core.helpers.extractors.MessageExtractorBasicStrategy;
import io.github.mtrevisan.boxon.core.helpers.templates.Template;
import io.github.mtrevisan.boxon.core.parsers.TemplateParser;
import io.github.mtrevisan.boxon.exceptions.AnnotationException;
import io.github.mtrevisan.boxon.exceptions.FieldException;
import io.github.mtrevisan.boxon.exceptions.TemplateException;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;


/**
 * The TemplateDescriber class is responsible for describing templates.
 */
class TemplateDescriber{

	private final TemplateParser templateParser;

	private final MessageDescriber messageDescriber;


	/**
	 * Create a template describer.
	 *
	 * @param core	The parser core.
	 * @param messageDescriber	The message describer.
	 * @return	A describer.
	 */
	static TemplateDescriber create(final Core core, final MessageDescriber messageDescriber){
		return new TemplateDescriber(core, messageDescriber);
	}


	private TemplateDescriber(final Core core, final MessageDescriber messageDescriber){
		templateParser = core.getTemplateParser();

		this.messageDescriber = messageDescriber;
	}


	/**
	 * Description of all the loaded templates.
	 *
	 * @return	The list of descriptions.
	 * @throws TemplateException   If a template error occurs.
	 */
	List<Map<String, Object>> describeTemplate() throws FieldException{
		return describeAllTemplates(FieldDescriber.MESSAGE_EXTRACTOR_BASIC_STRATEGY);
	}

	/**
	 * Description of a single template annotated with {@link TemplateHeader}.
	 *
	 * @param templateClass	Template class to be described.
	 * @return	The description.
	 * @throws AnnotationException   If an annotation error occurs.
	 * @throws TemplateException	If a template error occurs.
	 */
	Map<String, Object> describeTemplate(final Class<?> templateClass) throws FieldException{
		return describeSingleTemplate(templateClass, FieldDescriber.MESSAGE_EXTRACTOR_BASIC_STRATEGY);
	}

	/**
	 * Description of all the templates in the given package annotated with {@link TemplateHeader}.
	 *
	 * @param templateClasses	Classes to be used ase starting point from which to load annotated classes.
	 * @return	The list of descriptions.
	 * @throws AnnotationException	If an annotation error occurs.
	 * @throws TemplateException	If a template error occurs.
	 */
	List<Map<String, Object>> describeTemplate(final Class<?>... templateClasses) throws FieldException{
		final MessageExtractorBasicStrategy messageExtractor = FieldDescriber.MESSAGE_EXTRACTOR_BASIC_STRATEGY;
		return describeTemplatesSet(templateClasses, messageExtractor);
	}


	/**
	 * Description of all the loaded templates.
	 *
	 * @return	The list of descriptions.
	 * @throws TemplateException   If a template error occurs.
	 */
	List<Map<String, Object>> describeParsing() throws FieldException{
		return describeAllTemplates(FieldDescriber.MESSAGE_EXTRACTOR_FULL_STRATEGY);
	}

	/**
	 * Description of a single template annotated with {@link TemplateHeader}.
	 *
	 * @param templateClass	Template class to be described.
	 * @return	The description.
	 * @throws AnnotationException   If an annotation error occurs.
	 * @throws TemplateException	If a template error occurs.
	 */
	Map<String, Object> describeParsing(final Class<?> templateClass) throws FieldException{
		return describeSingleTemplate(templateClass, FieldDescriber.MESSAGE_EXTRACTOR_FULL_STRATEGY);
	}

	/**
	 * Description of all the templates in the given package annotated with {@link TemplateHeader}.
	 *
	 * @param templateClasses	Classes to be used ase starting point from which to load annotated classes.
	 * @return	The list of descriptions.
	 * @throws AnnotationException	If an annotation error occurs.
	 * @throws TemplateException	If a template error occurs.
	 */
	List<Map<String, Object>> describeParsing(final Class<?>... templateClasses) throws FieldException{
		final MessageExtractorBasicStrategy messageExtractor = FieldDescriber.MESSAGE_EXTRACTOR_FULL_STRATEGY;
		return describeTemplatesSet(templateClasses, messageExtractor);
	}


	private List<Map<String, Object>> describeAllTemplates(final MessageExtractorBasicStrategy messageExtractor) throws FieldException{
		final Collection<Template<?>> templates = new HashSet<>(templateParser.getTemplates());
		return FieldDescriber.describeEntities(templates, template -> messageDescriber.describeMessage(template, messageExtractor,
			FieldDescriber.FIELD_EXTRACTOR_STRATEGY));
	}

	private Map<String, Object> describeSingleTemplate(final Class<?> templateClass, final MessageExtractorBasicStrategy messageExtractor)
			throws FieldException{
		return FieldDescriber.describeEntity(TemplateHeader.class, templateClass, templateParser::extractTemplate,
			template -> messageDescriber.describeMessage(template, messageExtractor, FieldDescriber.FIELD_EXTRACTOR_STRATEGY));
	}

	private List<Map<String, Object>> describeTemplatesSet(final Class<?>[] templateClasses,
			final MessageExtractorBasicStrategy messageExtractor) throws FieldException{
		return FieldDescriber.describeEntities(TemplateHeader.class, templateClasses, templateParser::extractTemplate,
			template -> messageDescriber.describeMessage(template, messageExtractor, FieldDescriber.FIELD_EXTRACTOR_STRATEGY));
	}

}
