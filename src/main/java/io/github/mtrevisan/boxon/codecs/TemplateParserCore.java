/**
 * Copyright (c) 2020-2021 Mauro Trevisan
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
package io.github.mtrevisan.boxon.codecs;

import io.github.mtrevisan.boxon.annotations.MessageHeader;
import io.github.mtrevisan.boxon.codecs.managers.ReflectionHelper;
import io.github.mtrevisan.boxon.codecs.managers.Template;
import io.github.mtrevisan.boxon.exceptions.AnnotationException;
import io.github.mtrevisan.boxon.exceptions.TemplateException;
import io.github.mtrevisan.boxon.external.codecs.BitReader;
import io.github.mtrevisan.boxon.external.logs.EventListener;


@SuppressWarnings("unused")
public final class TemplateParserCore{

	private final EventListener eventListener;

	private final LoaderCodecInterface loaderCodec;
	private final LoaderTemplate loaderTemplate;

	private final Evaluator evaluator;


	/**
	 * Create a template parser core.
	 *
	 * @param loaderCodec	A codec loader.
	 * @param evaluator	An evaluator.
	 * @return	A template parser core.
	 */
	public static TemplateParserCore create(final LoaderCodecInterface loaderCodec, final Evaluator evaluator){
		return new TemplateParserCore(loaderCodec, EventListener.getNoOpInstance(), evaluator);
	}

	/**
	 * Create a template parser core.
	 *
	 * @param loaderCodec	A codec loader.
	 * @param eventListener	The event listener.
	 * @param evaluator	An evaluator.
	 * @return	A template parser core.
	 */
	public static TemplateParserCore create(final LoaderCodecInterface loaderCodec, final EventListener eventListener,
			final Evaluator evaluator){
		return new TemplateParserCore(loaderCodec, (eventListener != null? eventListener: EventListener.getNoOpInstance()), evaluator);
	}


	private TemplateParserCore(final LoaderCodecInterface loaderCodec, final EventListener eventListener, final Evaluator evaluator){
		this.eventListener = eventListener;

		this.loaderCodec = loaderCodec;
		loaderTemplate = LoaderTemplate.create(loaderCodec, eventListener);
		this.evaluator = evaluator;
	}


	/**
	 * Loads all the protocol classes annotated with {@link MessageHeader}.
	 * <p>This method SHOULD BE called from a method inside a class that lies on a parent of all the protocol classes.</p>
	 *
	 * @throws IllegalArgumentException	If the codecs was not loaded yet.
	 */
	public void loadDefaultTemplates() throws AnnotationException, TemplateException{
		loaderTemplate.loadTemplates(ReflectionHelper.extractCallerClasses());
	}

	/**
	 * Loads all the protocol classes annotated with {@link MessageHeader}.
	 *
	 * @param basePackageClasses	Classes to be used ase starting point from which to load annotated classes.
	 */
	public void loadTemplates(final Class<?>... basePackageClasses) throws AnnotationException, TemplateException{
		loaderTemplate.loadTemplates(basePackageClasses);
	}

	/**
	 * Load the specified protocol class annotated with {@link MessageHeader}.
	 *
	 * @param templateClass	Template class.
	 * @throws AnnotationException	If the annotation is not well formatted.
	 * @throws TemplateException	If the template is not well formatted.
	 */
	public void loadTemplate(final Class<?> templateClass) throws AnnotationException, TemplateException{
		loaderTemplate.loadTemplate(templateClass);
	}

	/**
	 * Constructs a new {@link Template}.
	 *
	 * @param <T>	The type of the object to be returned as a {@link Template}.
	 * @param type	The class of the object to be returned as a {@link Template}.
	 * @return	The {@link Template} for the given type.
	 */
	public <T> Template<T> createTemplate(final Class<T> type) throws AnnotationException{
		return loaderTemplate.createTemplate(type);
	}

	/**
	 * Retrieve the next template.
	 *
	 * @param reader	The reader to read the header from.
	 * @return	The template that is able to decode/encode the next message in the given reader.
	 */
	public Template<?> getTemplate(final BitReader reader) throws TemplateException{
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
	 * @param reader	The reader.
	 * @return	The index of the next message.
	 */
	public int findNextMessageIndex(final BitReader reader){
		return loaderTemplate.findNextMessageIndex(reader);
	}


	EventListener getEventListener(){
		return eventListener;
	}

	LoaderCodecInterface getLoaderCodec(){
		return loaderCodec;
	}

	public LoaderTemplate getLoaderTemplate(){
		return loaderTemplate;
	}

	Evaluator getEvaluator(){
		return evaluator;
	}

}
