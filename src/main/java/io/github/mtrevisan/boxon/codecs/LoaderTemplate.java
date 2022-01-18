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
package io.github.mtrevisan.boxon.codecs;

import io.github.mtrevisan.boxon.annotations.MessageHeader;
import io.github.mtrevisan.boxon.codecs.managers.Memoizer;
import io.github.mtrevisan.boxon.codecs.managers.ReflectionHelper;
import io.github.mtrevisan.boxon.codecs.managers.Template;
import io.github.mtrevisan.boxon.codecs.managers.matchers.BNDMPatternMatcher;
import io.github.mtrevisan.boxon.codecs.managers.matchers.PatternMatcher;
import io.github.mtrevisan.boxon.exceptions.AnnotationException;
import io.github.mtrevisan.boxon.exceptions.TemplateException;
import io.github.mtrevisan.boxon.external.codecs.BitReaderInterface;
import io.github.mtrevisan.boxon.external.logs.EventListener;
import io.github.mtrevisan.boxon.internal.StringHelper;
import io.github.mtrevisan.boxon.internal.ThrowingFunction;

import java.lang.annotation.Annotation;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;


public final class LoaderTemplate{

	private static final PatternMatcher PATTERN_MATCHER = BNDMPatternMatcher.getInstance();
	private static final Function<byte[], int[]> PRE_PROCESSED_PATTERNS = Memoizer.memoize(PATTERN_MATCHER::preProcessPattern);


	private final EventListener eventListener;

	private final ThrowingFunction<Class<?>, Template<?>, AnnotationException> templateStore
		= Memoizer.throwingMemoize(type -> Template.create(type, this::filterAnnotationsWithCodec));

	private final Map<String, Template<?>> templates = new TreeMap<>(Comparator.comparingInt(String::length).reversed()
		.thenComparing(String::compareTo));

	private final LoaderCodecInterface loaderCodec;


	/**
	 * Create a template parser.
	 *
	 * @param loaderCodec	A codec loader.
	 * @return	A template parser.
	 */
	static LoaderTemplate create(final LoaderCodecInterface loaderCodec){
		return create(loaderCodec, null);
	}

	/**
	 * Create a template parser.
	 *
	 * @param loaderCodec	A codec loader.
	 * @param eventListener	The event listener.
	 * @return	A template parser.
	 */
	static LoaderTemplate create(final LoaderCodecInterface loaderCodec, final EventListener eventListener){
		return new LoaderTemplate(loaderCodec, (eventListener != null? eventListener: EventListener.getNoOpInstance()));
	}


	private LoaderTemplate(final LoaderCodecInterface loaderCodec, final EventListener eventListener){
		this.eventListener = eventListener;
		this.loaderCodec = loaderCodec;
	}

	/**
	 * Loads all the protocol classes annotated with {@link MessageHeader}.
	 * <p>This method SHOULD BE called from a method inside a class that lies on a parent of all the protocol classes.</p>
	 *
	 * @throws IllegalArgumentException	If the codecs was not loaded yet.
	 */
	void loadDefaultTemplates() throws AnnotationException, TemplateException{
		loadTemplates(ReflectionHelper.extractCallerClasses());
	}

	/**
	 * Loads all the protocol classes annotated with {@link MessageHeader}.
	 *
	 * @param basePackageClasses	Classes to be used ase starting point from which to load annotated classes.
	 */
	void loadTemplates(final Class<?>... basePackageClasses) throws AnnotationException, TemplateException{
		eventListener.loadingTemplates(basePackageClasses);

		/** extract all classes annotated with {@link MessageHeader}. */
		final Collection<Class<?>> annotatedClasses = ReflectionHelper.extractClasses(MessageHeader.class, basePackageClasses);
		final List<Template<?>> templates = extractTemplates(annotatedClasses);
		addTemplatesInner(templates);

		eventListener.loadedTemplates(templates.size());
	}

	/**
	 * Load the specified protocol class annotated with {@link MessageHeader}.
	 *
	 * @param templateClass	Template class.
	 */
	void loadTemplate(final Class<?> templateClass) throws AnnotationException, TemplateException{
		eventListener.loadingTemplate(templateClass);

		if(templateClass.isAnnotationPresent(MessageHeader.class)){
			/** extract all classes annotated with {@link MessageHeader}. */
			final Template<?> template = extractTemplate(templateClass);
			if(template.canBeCoded()){
				addTemplateInner(template);

				eventListener.loadedTemplates(templates.size());
			}
		}
	}

	private List<Template<?>> extractTemplates(final Collection<Class<?>> annotatedClasses) throws AnnotationException,
			TemplateException{
		final List<Template<?>> templates = new ArrayList<>(annotatedClasses.size());
		for(final Class<?> type : annotatedClasses){
			//for each extracted class, try to parse it, extracting all the information needed for the codec of a message
			final Template<?> from = createTemplate(type);
			if(from.canBeCoded())
				//if the template is valid, add it to the list of templates...
				templates.add(from);
			else
				//... otherwise throw exception
				throw TemplateException.create("Cannot create a raw message from data: cannot scan template for {}",
					type.getSimpleName());
		}
		return templates;
	}

	public Template<?> extractTemplate(final Class<?> type) throws AnnotationException, TemplateException{
		final Template<?> from = createTemplate(type);
		if(!from.canBeCoded())
			throw TemplateException.create("Cannot create a raw message from data: cannot scan template for {}",
				type.getSimpleName());

		return from;
	}

	/**
	 * Constructs a new {@link Template}.
	 *
	 * @param <T>	The type of the object to be returned as a {@link Template}.
	 * @param type	The class of the object to be returned as a {@link Template}.
	 * @return	The {@link Template} for the given type.
	 */
	@SuppressWarnings("unchecked")
	public <T> Template<T> createTemplate(final Class<T> type) throws AnnotationException{
		return (Template<T>)templateStore.apply(type);
	}

	private void addTemplatesInner(final List<Template<?>> templates){
		//load each template into the available templates list
		for(int i = 0; i < templates.size(); i ++){
			final Template<?> template = templates.get(i);
			if(template != null && template.canBeCoded())
				addTemplateInner(template);
		}
	}

	/**
	 * For each valid template, add it to the map of templates indexed by starting message bytes.
	 *
	 * @param template	The template to add to the list of available templates.
	 */
	private void addTemplateInner(final Template<?> template){
		try{
			final MessageHeader header = template.getHeader();
			final Charset charset = Charset.forName(header.charset());
			final String[] starts = header.start();
			for(int i = 0; i < starts.length; i ++)
				loadTemplateInner(template, starts[i], charset);
		}
		catch(final Exception e){
			eventListener.cannotLoadTemplate(template.getType().getName(), e);
		}
	}

	private void loadTemplateInner(final Template<?> template, final String headerStart, final Charset charset) throws TemplateException{
		final String key = calculateKey(headerStart, charset);
		if(templates.containsKey(key))
			throw TemplateException.create("Duplicated key `{}` found for class {}", headerStart, template.getType().getName());

		templates.put(key, template);
	}

	/**
	 * Retrieve the next template.
	 *
	 * @param reader	The reader to read the header from.
	 * @return	The template that is able to decode/encode the next message in the given reader.
	 */
	Template<?> getTemplate(final BitReaderInterface reader) throws TemplateException{
		final int index = reader.position();

		//for each available template, select the first that matches the starting bytes
		//note that the templates are ordered by the length of the starting bytes, descending, so the first that matches is that
		//with the longest match
		final byte[] array = reader.array();
		for(final Map.Entry<String, Template<?>> entry : templates.entrySet()){
			final String header = entry.getKey();
			final byte[] templateHeader = StringHelper.toByteArray(header);

			//verify if it's a valid message header
			final int lastIndex = index + templateHeader.length;
			if(lastIndex <= array.length && Arrays.equals(array, index, lastIndex, templateHeader, 0, templateHeader.length))
				return entry.getValue();
		}

		throw TemplateException.create("Cannot find any template for given raw message");
	}

	/**
	 * Retrieve the template by class.
	 *
	 * @param type	The class to retrieve the template.
	 * @return	The template that is able to decode/encode the given class.
	 */
	Template<?> getTemplate(final Class<?> type) throws TemplateException{
		final MessageHeader header = type.getAnnotation(MessageHeader.class);
		if(header == null)
			throw TemplateException.create("The given class type is not a valid template");

		final String key = calculateKey(header.start()[0], Charset.forName(header.charset()));
		final Template<?> template = templates.get(key);
		if(template == null)
			throw TemplateException.create("Cannot find any template for given class type");

		return template;
	}

	public Collection<Template<?>> getTemplates(){
		return Collections.unmodifiableCollection(templates.values());
	}

	private static String calculateKey(final String headerStart, final Charset charset){
		return StringHelper.toHexString(headerStart.getBytes(charset));
	}

	private List<Annotation> filterAnnotationsWithCodec(final Annotation[] declaredAnnotations){
		final List<Annotation> annotations = new ArrayList<>(declaredAnnotations.length);
		for(int i = 0; i < declaredAnnotations.length; i ++)
			if(loaderCodec.hasCodec(declaredAnnotations[i].annotationType()))
				annotations.add(declaredAnnotations[i]);
		return annotations;
	}

	/**
	 * Tries to infer the next message start by scanning all templates in header-start-length order.
	 *
	 * @param reader	The reader.
	 * @return	The index of the next message.
	 */
	int findNextMessageIndex(final BitReaderInterface reader){
		int minOffset = -1;
		for(final Template<?> template : templates.values()){
			final MessageHeader header = template.getHeader();

			minOffset = findNextMessageIndex(reader, header, minOffset);
		}
		return minOffset;
	}

	private static int findNextMessageIndex(final BitReaderInterface reader, final MessageHeader header, int minOffset){
		final Charset charset = Charset.forName(header.charset());
		final String[] messageStarts = header.start();
		//select the minimum index with a valid template
		for(int i = 0; i < messageStarts.length; i ++){
			final int offset = searchNextSequence(reader, messageStarts[i].getBytes(charset));
			if(offset >= 0 && (minOffset < 0 || offset < minOffset))
				minOffset = offset;
		}
		return minOffset;
	}

	private static int searchNextSequence(final BitReaderInterface reader, final byte[] startMessageSequence){
		final byte[] message = reader.array();
		final int startIndex = reader.position();
		final int[] preProcessedPattern = PRE_PROCESSED_PATTERNS.apply(startMessageSequence);
		final int index = PATTERN_MATCHER.indexOf(message, startIndex + 1, startMessageSequence, preProcessedPattern);
		return (index >= startIndex? index: -1);
	}

}
