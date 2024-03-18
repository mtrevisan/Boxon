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

import io.github.mtrevisan.boxon.annotations.MessageHeader;
import io.github.mtrevisan.boxon.core.codecs.LoaderCodecInterface;
import io.github.mtrevisan.boxon.core.helpers.templates.Template;
import io.github.mtrevisan.boxon.exceptions.AnnotationException;
import io.github.mtrevisan.boxon.exceptions.TemplateException;
import io.github.mtrevisan.boxon.helpers.Evaluator;
import io.github.mtrevisan.boxon.helpers.JavaHelper;
import io.github.mtrevisan.boxon.io.BitReaderInterface;
import io.github.mtrevisan.boxon.logs.EventListener;


@SuppressWarnings("unused")
final class TemplateParserCore{

	private final LoaderCodecInterface loaderCodec;
	private final LoaderTemplate loaderTemplate;

	private final Evaluator evaluator;

	private EventListener eventListener;


	/**
	 * Create a template parser core.
	 *
	 * @param loaderCodec	A codec loader.
	 * @param evaluator	An evaluator.
	 * @return	A template parser core.
	 */
	static TemplateParserCore create(final LoaderCodecInterface loaderCodec, final Evaluator evaluator){
		return new TemplateParserCore(loaderCodec, evaluator);
	}


	private TemplateParserCore(final LoaderCodecInterface loaderCodec, final Evaluator evaluator){
		this.loaderCodec = loaderCodec;
		loaderTemplate = LoaderTemplate.create(loaderCodec);
		this.evaluator = evaluator;

		eventListener = EventListener.getNoOpInstance();
	}


	/**
	 * Assign an event listener.
	 *
	 * @param eventListener	The event listener.
	 * @return	This instance, used for chaining.
	 */
	TemplateParserCore withEventListener(final EventListener eventListener){
		this.eventListener = JavaHelper.nonNullOrDefault(eventListener, EventListener.getNoOpInstance());

		loaderTemplate.withEventListener(eventListener);

		return this;
	}


	/**
	 * Loads all the protocol classes annotated with {@link MessageHeader}.
	 *
	 * @param basePackageClasses	Classes to be used ase starting point from which to load annotated classes.
	 */
	void loadTemplatesFrom(final Class<?>... basePackageClasses) throws AnnotationException, TemplateException{
		loaderTemplate.loadTemplatesFrom(basePackageClasses);
	}

	/**
	 * Load the specified protocol class annotated with {@link MessageHeader}.
	 *
	 * @param templateClass	Template class.
	 * @throws AnnotationException	If the annotation is not well formatted.
	 * @throws TemplateException	If the template is not well formatted.
	 */
	void loadTemplate(final Class<?> templateClass) throws AnnotationException, TemplateException{
		loaderTemplate.loadTemplate(templateClass);
	}


	/**
	 * Constructs a new {@link Template}.
	 *
	 * @param type	The class of the object to be returned as a {@link Template}.
	 * @param <T>	The type of the object to be returned as a {@link Template}.
	 * @return	The {@link Template} for the given type.
	 * @throws AnnotationException	If an annotation has validation problems.
	 */
	<T> Template<T> createTemplate(final Class<T> type) throws AnnotationException{
		return loaderTemplate.createTemplate(type);
	}

	/**
	 * Retrieve the next template.
	 *
	 * @param reader	The reader from which to read the header from.
	 * @return	The template that is able to decode/encode the next message in the given reader.
	 */
	Template<?> getTemplate(final BitReaderInterface reader) throws TemplateException{
		return loaderTemplate.getTemplate(reader);
	}

	/**
	 * Retrieve the template by class.
	 *
	 * @param type	The class to retrieve the template.
	 * @return	The template that is able to decode/encode the given class.
	 */
	Template<?> getTemplate(final Class<?> type) throws TemplateException{
		return loaderTemplate.getTemplate(type);
	}

	/**
	 * Tries to infer the next message start by scanning all templates in header-start-length order.
	 *
	 * @param reader	The reader from which to read the data from.
	 * @return	The index of the next message.
	 */
	int findNextMessageIndex(final BitReaderInterface reader){
		return loaderTemplate.findNextMessageIndex(reader);
	}


	EventListener getEventListener(){
		return eventListener;
	}

	LoaderCodecInterface getLoaderCodec(){
		return loaderCodec;
	}

	LoaderTemplate getLoaderTemplate(){
		return loaderTemplate;
	}

	Evaluator getEvaluator(){
		return evaluator;
	}

}
