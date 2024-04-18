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
import io.github.mtrevisan.boxon.exceptions.AnnotationException;
import io.github.mtrevisan.boxon.exceptions.ConfigurationException;
import io.github.mtrevisan.boxon.exceptions.EncodeException;
import io.github.mtrevisan.boxon.exceptions.FieldException;
import io.github.mtrevisan.boxon.exceptions.TemplateException;

import java.util.List;
import java.util.Map;


/**
 * Declarative describer for binary encoded data.
 */
@SuppressWarnings({"unused", "WeakerAccess"})
public final class Describer{

	private final TemplateDescriber templateDescriber;
	private final ConfigurationDescriber configurationDescriber;


	/**
	 * Create a describer.
	 *
	 * @param core	The parser core.
	 * @return	A describer.
	 */
	public static Describer create(final Core core){
		return new Describer(core);
	}


	private Describer(final Core core){
		final MessageDescriber messageDescriber = MessageDescriber.create(core);
		templateDescriber = TemplateDescriber.create(core, messageDescriber);
		configurationDescriber = ConfigurationDescriber.create(core, messageDescriber);
	}


	/**
	 * Description of all the loaded templates.
	 *
	 * @return	The list of descriptions.
	 * @throws TemplateException	If a template error occurs.
	 */
	public List<Map<String, Object>> describeParsing() throws FieldException{
		return templateDescriber.describeParsing();
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
		return templateDescriber.describeParsing(templateClass);
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
		return templateDescriber.describeParsing(templateClasses);
	}


	/**
	 * Description of all the loaded templates.
	 *
	 * @return	The list of descriptions.
	 * @throws TemplateException	If a template error occurs.
	 */
	public List<Map<String, Object>> describeTemplate() throws FieldException{
		return templateDescriber.describeTemplate();
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
		return templateDescriber.describeTemplate(templateClass);
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
		return templateDescriber.describeTemplate(templateClasses);
	}


	/**
	 * Description of all the loaded configuration.
	 *
	 * @return	The list of descriptions.
	 * @throws ConfigurationException	If a configuration error occurs.
	 */
	public List<Map<String, Object>> describeConfiguration() throws FieldException{
		return configurationDescriber.describeConfiguration();
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
		return configurationDescriber.describeConfiguration(configurationClass);
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
		return configurationDescriber.describeConfiguration(configurationClasses);
	}


	/**
	 * Description of a single annotated class.
	 *
	 * @param boundClass	Generic bound class to be described.
	 * @return	The description.
	 */
	public static Map<String, Object> describeRawMessage(final Class<?> boundClass){
		return FieldDescriber.describeRawMessage(boundClass);
	}

}
