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
package io.github.mtrevisan.boxon.core.parsers;

import io.github.mtrevisan.boxon.annotations.TemplateHeader;
import io.github.mtrevisan.boxon.core.codecs.LoaderCodec;
import io.github.mtrevisan.boxon.core.codecs.TemplateParserInterface;
import io.github.mtrevisan.boxon.core.helpers.templates.Template;
import io.github.mtrevisan.boxon.exceptions.AnnotationException;
import io.github.mtrevisan.boxon.exceptions.BoxonException;
import io.github.mtrevisan.boxon.exceptions.TemplateException;
import io.github.mtrevisan.boxon.io.BitReaderInterface;
import io.github.mtrevisan.boxon.io.BitWriterInterface;
import io.github.mtrevisan.boxon.io.Evaluator;
import io.github.mtrevisan.boxon.logs.EventListener;

import java.util.Collection;


/**
 * Declarative data binding parser for message templates.
 */
public final class TemplateParser implements TemplateParserInterface{

	private final TemplateDecoder templateDecoder;
	private final TemplateEncoder templateEncoder;

	private final LoaderTemplate loaderTemplate;


	/**
	 * Create a template parser.
	 *
	 * @param loaderCodec	A codec loader.
	 * @param evaluator	An evaluator.
	 * @return	A template parser.
	 */
	public static TemplateParser create(final LoaderCodec loaderCodec, final Evaluator evaluator){
		return new TemplateParser(loaderCodec, evaluator);
	}


	private TemplateParser(final LoaderCodec loaderCodec, final Evaluator evaluator){
		templateDecoder = TemplateDecoder.create(loaderCodec, evaluator);
		templateEncoder = TemplateEncoder.create(loaderCodec, evaluator);

		loaderTemplate = LoaderTemplate.create(loaderCodec);

		withEventListener(null);
	}


	/**
	 * Assign an event listener.
	 *
	 * @param eventListener	The event listener.
	 * @return	This instance, used for chaining.
	 */
	public TemplateParser withEventListener(final EventListener eventListener){
		templateDecoder.withEventListener(eventListener);
		templateEncoder.withEventListener(eventListener);
		loaderTemplate.withEventListener(eventListener);

		return this;
	}

	/**
	 * Loads all the protocol classes annotated with {@link TemplateHeader}.
	 *
	 * @param basePackageClasses	Classes to be used ase starting point from which to load annotated classes.
	 * @return	This instance, used for chaining.
	 * @throws AnnotationException	If an annotation error occurs.
	 * @throws TemplateException	If a template error occurs.
	 */
	public TemplateParser withTemplatesFrom(final Class<?>... basePackageClasses) throws AnnotationException, TemplateException{
		loaderTemplate.loadTemplatesFrom(basePackageClasses);

		return this;
	}

	/**
	 * Load the specified protocol class annotated with {@link TemplateHeader}.
	 *
	 * @param templateClass	Template class.
	 * @return	This instance, used for chaining.
	 * @throws AnnotationException	If an annotation error occurs.
	 * @throws TemplateException	If the template error occurs.
	 */
	public TemplateParser withTemplate(final Class<?> templateClass) throws AnnotationException, TemplateException{
		loaderTemplate.loadTemplate(templateClass);

		return this;
	}


	@Override
	public <T> Template<T> createTemplate(final Class<T> type) throws AnnotationException{
		return loaderTemplate.createTemplate(type);
	}

	/**
	 * Retrieve the next template.
	 *
	 * @param reader	The reader from which to read the header from.
	 * @return	The template that is able to decode/encode the next message in the given reader.
	 */
	public Template<?> getTemplate(final BitReaderInterface reader) throws TemplateException{
		return loaderTemplate.getTemplate(reader);
	}

	/**
	 * Retrieve the template by class.
	 *
	 * @param type	The class to retrieve the template.
	 * @return	The template that is able to decode/encode the given class.
	 */
	public Template<?> getTemplate(final Class<?> type) throws TemplateException{
		return loaderTemplate.getTemplate(type);
	}

	/**
	 * Tries to infer the next message start by scanning all templates in header-start-length order.
	 *
	 * @param reader	The reader from which to read the data from.
	 * @return	The index of the next message.
	 */
	public int findNextMessageIndex(final BitReaderInterface reader){
		return loaderTemplate.findNextMessageIndex(reader);
	}

	/**
	 * Extract a template for the given class.
	 *
	 * @param type	The class type.
	 * @return	A template.
	 * @throws AnnotationException	If an annotation error occurs.
	 * @throws TemplateException	If a template error occurs.
	 */
	public Template<?> extractTemplate(final Class<?> type) throws AnnotationException, TemplateException{
		return loaderTemplate.extractTemplate(type);
	}

	/**
	 * Unmodifiable collection of templates.
	 *
	 * @return	Collection of templates.
	 */
	public Collection<Template<?>> getTemplates(){
		return loaderTemplate.getTemplates();
	}


	@Override
	public <T> T decode(final Template<T> template, final BitReaderInterface reader, final Object parentObject) throws BoxonException{
		return templateDecoder.decode(template, reader, parentObject);
	}


	@Override
	public <T> void encode(final Template<?> template, final BitWriterInterface writer, final Object parentObject, final T currentObject)
			throws BoxonException{
		templateEncoder.encode(template, writer, parentObject, currentObject);
	}

}
