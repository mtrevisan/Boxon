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
import io.github.mtrevisan.boxon.core.codecs.LoaderCodecInterface;
import io.github.mtrevisan.boxon.core.helpers.templates.Template;
import io.github.mtrevisan.boxon.core.parsers.matchers.KMPPatternMatcher;
import io.github.mtrevisan.boxon.core.parsers.matchers.PatternMatcher;
import io.github.mtrevisan.boxon.exceptions.AnnotationException;
import io.github.mtrevisan.boxon.exceptions.TemplateException;
import io.github.mtrevisan.boxon.helpers.CharsetHelper;
import io.github.mtrevisan.boxon.helpers.JavaHelper;
import io.github.mtrevisan.boxon.helpers.Memoizer;
import io.github.mtrevisan.boxon.helpers.ReflectiveClassLoader;
import io.github.mtrevisan.boxon.helpers.StringHelper;
import io.github.mtrevisan.boxon.helpers.ThrowingFunction;
import io.github.mtrevisan.boxon.io.BitReaderInterface;
import io.github.mtrevisan.boxon.logs.EventListener;

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


/**
 * Loader for the templates.
 */
public final class LoaderTemplate{

	private static final PatternMatcher PATTERN_MATCHER = KMPPatternMatcher.getInstance();
	private static final Function<byte[], int[]> PRE_PROCESSED_PATTERNS = Memoizer.memoize(PATTERN_MATCHER::preProcessPattern);


	private final ThrowingFunction<Class<?>, Template<?>, AnnotationException> templateStore
		= Memoizer.throwingMemoize(type -> Template.create(type, this::filterAnnotationsWithCodec));

	private final Map<String, Template<?>> templates = new TreeMap<>(Comparator.comparingInt(String::length).reversed()
		.thenComparing(String::compareTo));

	private final LoaderCodecInterface loaderCodec;

	private EventListener eventListener;


	/**
	 * Create a template parser.
	 *
	 * @param loaderCodec	A codec loader.
	 * @return	A template parser.
	 */
	static LoaderTemplate create(final LoaderCodecInterface loaderCodec){
		return new LoaderTemplate(loaderCodec);
	}


	private LoaderTemplate(final LoaderCodecInterface loaderCodec){
		this.loaderCodec = loaderCodec;

		eventListener = EventListener.getNoOpInstance();
	}


	/**
	 * Assign an event listener.
	 *
	 * @param eventListener	The event listener.
	 * @return	This instance, used for chaining.
	 */
	LoaderTemplate withEventListener(final EventListener eventListener){
		this.eventListener = JavaHelper.nonNullOrDefault(eventListener, EventListener.getNoOpInstance());

		return this;
	}

	/**
	 * Loads all the protocol classes annotated with {@link TemplateHeader}.
	 *
	 * @param basePackageClasses	Classes to be used ase starting point from which to load annotated classes.
	 * @throws AnnotationException	If an annotation has validation problems.
	 * @throws TemplateException	If the template was already added (defined by `start` parameter in the header definition).
	 */
	void loadTemplatesFrom(final Class<?>... basePackageClasses) throws AnnotationException, TemplateException{
		eventListener.loadingTemplatesFrom(basePackageClasses);

		final ReflectiveClassLoader reflectiveClassLoader = ReflectiveClassLoader.createFrom(basePackageClasses);
		/** extract all classes annotated with {@link TemplateHeader}. */
		final List<Class<?>> annotatedClasses = reflectiveClassLoader.extractClassesWithAnnotation(TemplateHeader.class);
		final List<Template<?>> templates = extractTemplates(annotatedClasses);
		addTemplatesInner(templates);

		eventListener.loadedTemplates(templates.size());
	}

	/**
	 * Load the specified protocol class annotated with {@link TemplateHeader}.
	 *
	 * @param templateClass	Template class.
	 * @throws AnnotationException	If an annotation has validation problems.
	 * @throws TemplateException	If a template is not well formatted.
	 */
	public void loadTemplate(final Class<?> templateClass) throws AnnotationException, TemplateException{
		eventListener.loadingTemplate(templateClass);

		if(templateClass.isAnnotationPresent(TemplateHeader.class)){
			/** extract all classes annotated with {@link TemplateHeader}. */
			final Template<?> template = extractTemplate(templateClass);
			if(template.canBeCoded()){
				addTemplateInner(template);

				eventListener.loadedTemplates(templates.size());
			}
		}
	}

	private List<Template<?>> extractTemplates(final List<Class<?>> annotatedClasses) throws AnnotationException, TemplateException{
		final int size = annotatedClasses.size();
		final List<Template<?>> templates = new ArrayList<>(size);
		for(int i = 0; i < size; i ++){
			final Class<?> type = annotatedClasses.get(i);

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

	/**
	 * Extract a template for the given class.
	 *
	 * @param type	The class type.
	 * @return	A template.
	 * @throws AnnotationException	If an annotation has validation problems.
	 * @throws TemplateException	If a template is not well formatted.
	 */
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
	 * @param type	The class of the object to be returned as a {@link Template}.
	 * @param <T>	The type of the object to be returned as a {@link Template}.
	 * @return	The {@link Template} for the given type.
	 * @throws AnnotationException	If an annotation has validation problems.
	 */
	@SuppressWarnings("unchecked")
	<T> Template<T> createTemplate(final Class<T> type) throws AnnotationException{
		return (Template<T>)templateStore.apply(type);
	}

	private void addTemplatesInner(final List<Template<?>> templates) throws TemplateException{
		//load each template into the available templates list
		for(int i = 0, length = templates.size(); i < length; i ++){
			final Template<?> template = templates.get(i);

			if(template != null && template.canBeCoded())
				addTemplateInner(template);
		}
	}

	/**
	 * For each valid template, add it to the map of templates indexed by starting message bytes.
	 *
	 * @param template	The template to add to the list of available templates.
	 * @throws TemplateException	If the template was already added (defined by `start` parameter in the header definition).
	 */
	private void addTemplateInner(final Template<?> template) throws TemplateException{
		try{
			final TemplateHeader header = template.getHeader();
			final Charset charset = CharsetHelper.lookup(header.charset());
			final String[] starts = header.start();
			for(int i = 0, length = starts.length; i < length; i ++)
				loadTemplateInner(template, starts[i], charset);
		}
		catch(final TemplateException e){
			eventListener.cannotLoadTemplate(template.getType().getName(), e);

			throw e;
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
	 * @param reader	The reader from which to read the header from.
	 * @return	The template that is able to decode/encode the next message in the given reader.
	 * @throws TemplateException	If no template cannot be found that is able to parse the given message.
	 */
	Template<?> getTemplate(final BitReaderInterface reader) throws TemplateException{
		final int index = reader.position();

		//for each available template, select the first that matches the starting bytes
		//note that the templates are ordered by the length of the starting bytes, descending, so the first that matches is that
		//with the longest match
		final byte[] array = reader.array();
		for(final Map.Entry<String, Template<?>> entry : templates.entrySet()){
			final String header = entry.getKey();

			//FIXME can this (newly created) array be substituted with something else?
			final byte[] templateHeader = StringHelper.hexToByteArray(header);

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
	 * @throws TemplateException	Whether the template is not valid.
	 */
	Template<?> getTemplate(final Class<?> type) throws TemplateException{
		final TemplateHeader header = type.getAnnotation(TemplateHeader.class);
		if(header == null)
			throw TemplateException.create("The given class type is not a valid template");

		//NOTE: we want only a template, so we pick the first `start`
		final String headerFirstStart = header.start()[0];
		final String key = calculateKey(headerFirstStart, CharsetHelper.lookup(header.charset()));
		final Template<?> template = templates.get(key);
		if(template == null)
			throw TemplateException.create("Cannot find any template for given class type");

		return template;
	}

	/**
	 * Unmodifiable collection of templates.
	 *
	 * @return	Collection of templates.
	 */
	public Collection<Template<?>> getTemplates(){
		return Collections.unmodifiableCollection(templates.values());
	}

	private static String calculateKey(final String headerStart, final Charset charset){
		return StringHelper.toHexString(headerStart.getBytes(charset));
	}

	private List<Annotation> filterAnnotationsWithCodec(final Annotation[] declaredAnnotations){
		final int length = declaredAnnotations.length;
		final List<Annotation> annotations = new ArrayList<>(length);
		for(int i = 0; i < length; i ++){
			final Annotation declaredAnnotation = declaredAnnotations[i];
			if(loaderCodec.hasCodec(declaredAnnotation.annotationType()))
				annotations.add(declaredAnnotation);
		}
		return annotations;
	}

	/**
	 * Tries to infer the next message start by scanning all templates in header-start-length order.
	 *
	 * @param reader	The reader from which to read the data from.
	 * @return	The index of the next message.
	 */
	int findNextMessageIndex(final BitReaderInterface reader){
		int minOffset = -1;
		for(final Template<?> template : templates.values()){
			final TemplateHeader header = template.getHeader();

			minOffset = findNextMessageIndex(reader, header, minOffset);
		}
		return minOffset;
	}

	private static int findNextMessageIndex(final BitReaderInterface reader, final TemplateHeader header, int minOffset){
		final Charset charset = CharsetHelper.lookup(header.charset());
		final String[] messageStarts = header.start();
		//select the minimum index with a valid template
		for(int i = 0, length = messageStarts.length; i < length; i ++){
			final int offset = searchNextSequence(reader, messageStarts[i].getBytes(charset));
			if(offset >= 0 && !(0 <= minOffset && minOffset <= offset))
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
