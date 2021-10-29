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
import io.github.mtrevisan.boxon.exceptions.AnnotationException;
import io.github.mtrevisan.boxon.exceptions.TemplateException;
import io.github.mtrevisan.boxon.external.BitReader;
import io.github.mtrevisan.boxon.external.EventListener;
import io.github.mtrevisan.boxon.internal.InjectEventListener;
import io.github.mtrevisan.boxon.internal.JavaHelper;
import io.github.mtrevisan.boxon.internal.Memoizer;
import io.github.mtrevisan.boxon.internal.ReflectionHelper;
import io.github.mtrevisan.boxon.internal.ReflectiveClassLoader;
import io.github.mtrevisan.boxon.internal.ThrowingFunction;
import io.github.mtrevisan.boxon.internal.matchers.BNDMPatternMatcher;
import io.github.mtrevisan.boxon.internal.matchers.PatternMatcher;

import java.lang.annotation.Annotation;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.function.Function;


final class Loader{

	@InjectEventListener
	private final EventListener eventListener;

	private final ThrowingFunction<Class<?>, Template<?>, AnnotationException> templateStore = Memoizer.throwingMemoize(
		type -> new Template<>(type, this::filterAnnotationsWithCodec));

	private static final PatternMatcher PATTERN_MATCHER = BNDMPatternMatcher.getInstance();
	private static final Function<byte[], int[]> PRE_PROCESSED_PATTERNS = Memoizer.memoize(PATTERN_MATCHER::preProcessPattern);

	private final Map<String, Template<?>> templates = new TreeMap<>(Comparator.comparingInt(String::length).reversed()
		.thenComparing(String::compareTo));
	private final Map<Class<?>, CodecInterface<?>> codecs = new HashMap<>(0);


	/**
	 * Create a loader.
	 *
	 * @return	A loader.
	 */
	public static Loader create(){
		return new Loader(EventListener.getNoOpInstance());
	}

	/**
	 * Create a loader.
	 *
	 * @param eventListener	The event listener.
	 * @return	A loader.
	 */
	public static Loader create(final EventListener eventListener){
		return new Loader(eventListener != null? eventListener: EventListener.getNoOpInstance());
	}


	private Loader(final EventListener eventListener){
		this.eventListener = eventListener;
	}


	/**
	 * Loads all the codecs that extends {@link CodecInterface}.
	 * <p>This method SHOULD BE called from a method inside a class that lies on a parent of all the codecs.</p>
	 */
	void loadDefaultCodecs(){
		loadCodecs(ReflectionHelper.extractCallerClasses());
	}

	/**
	 * Loads all the codecs that extends {@link CodecInterface}.
	 *
	 * @param basePackageClasses	Classes to be used ase starting point from which to load codecs.
	 */
	void loadCodecs(final Class<?>... basePackageClasses){
		eventListener.loadingCodecs(basePackageClasses);

		/** extract all classes that implements {@link CodecInterface}. */
		final Collection<Class<?>> derivedClasses = extractClasses(CodecInterface.class, basePackageClasses);
		final List<CodecInterface<?>> codecs = extractCodecs(derivedClasses);
		addCodecsInner(codecs);

		eventListener.loadedCodecs(codecs.size());
	}

	private List<CodecInterface<?>> extractCodecs(final Collection<Class<?>> derivedClasses){
		final List<CodecInterface<?>> codecs = new ArrayList<>(derivedClasses.size());
		for(final Class<?> type : derivedClasses){
			//for each extracted class, try to create an instance
			final CodecInterface<?> codec = (CodecInterface<?>)ReflectionHelper.getCreator(type)
				.get();
			if(codec != null)
				//if the codec was created successfully instanced, add it to the list of codecs...
				codecs.add(codec);
			else
				//... otherwise warn
				eventListener.cannotCreateCodec(type.getSimpleName());
		}
		return codecs;
	}

	/**
	 * Loads all the given codecs that extends {@link CodecInterface}.
	 * <p>NOTE: If the loader previously contains a codec for a given key, the old codec is replaced by the new one.</p>
	 *
	 * @param codecs	The list of codecs to be loaded.
	 */
	void addCodecs(final CodecInterface<?>... codecs){
		Objects.requireNonNull(codecs, "Codecs cannot be null");

		eventListener.loadingCodec(codecs);

		addCodecsInner(codecs);

		eventListener.loadedCodecs(codecs.length);
	}

	private void addCodecsInner(final List<CodecInterface<?>> codecs){
		//load each codec into the available codec list
		for(int i = 0; i < codecs.size(); i ++)
			if(codecs.get(i) != null)
				addCodecInner(codecs.get(i));
	}

	private void addCodecsInner(final CodecInterface<?>... codecs){
		//load each codec into the available codec list
		for(int i = 0; i < codecs.length; i ++)
			if(codecs[i] != null)
				addCodecInner(codecs[i]);
	}

	private void addCodecInner(final CodecInterface<?> codec){
		final Class<?> codecType = ReflectionHelper.resolveGenericTypes(codec.getClass(), CodecInterface.class).get(0);
		codecs.put(codecType, codec);
	}

	private boolean hasCodec(final Class<?> type){
		return codecs.containsKey(type);
	}

	CodecInterface<?> getCodec(final Class<?> type){
		return codecs.get(type);
	}


	private List<Annotation> filterAnnotationsWithCodec(final Annotation[] declaredAnnotations){
		final List<Annotation> annotations = new ArrayList<>(declaredAnnotations.length);
		for(int i = 0; i < declaredAnnotations.length; i ++)
			if(hasCodec(declaredAnnotations[i].annotationType()))
				annotations.add(declaredAnnotations[i]);
		return annotations;
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
		final Collection<Class<?>> annotatedClasses = extractClasses(MessageHeader.class, basePackageClasses);
		final List<Template<?>> templates = extractTemplates(annotatedClasses);
		addTemplatesInner(templates);

		eventListener.loadedTemplates(templates.size());
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

	/**
	 * Constructs a new {@link Template}.
	 *
	 * @param <T>	The type of the object to be returned as a {@link Template}.
	 * @param type	The class of the object to be returned as a {@link Template}.
	 * @return	The {@link Template} for the given type.
	 */
	@SuppressWarnings("unchecked")
	<T> Template<T> createTemplate(final Class<T> type) throws AnnotationException{
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

	private void loadTemplateInner(final Template<?> template, final String headerStart, final Charset charset)
			throws TemplateException{
		final String key = calculateTemplateKey(headerStart, charset);
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
	Template<?> getTemplate(final BitReader reader) throws TemplateException{
		final int index = reader.position();

		//for each available template, select the first that matches the starting bytes
		//note that the templates are ordered by the length of the starting bytes, descending, so the first that matches is that
		//with the longest match
		for(final Map.Entry<String, Template<?>> entry : templates.entrySet()){
			final String header = entry.getKey();
			final byte[] templateHeader = JavaHelper.toByteArray(header);

			//verify if it's a valid message header
			final int lastIndex = index + templateHeader.length;
			final byte[] array = reader.array();
			if(lastIndex <= array.length && Arrays.equals(array, index, lastIndex, templateHeader, 0, templateHeader.length))
				return entry.getValue();
		}

		throw TemplateException.create("Cannot find any template for given raw message");
	}

	/**
	 * Retrieve the next template by class.
	 *
	 * @param type	The class to retrieve the template.
	 * @return	The template that is able to decode/encode the given class.
	 */
	Template<?> getTemplate(final Class<?> type) throws TemplateException{
		final MessageHeader header = type.getAnnotation(MessageHeader.class);
		if(header == null)
			throw TemplateException.create("The given class type is not a valid template");

		final String key = calculateTemplateKey(header.start()[0], Charset.forName(header.charset()));
		final Template<?> template = templates.get(key);
		if(template == null)
			throw TemplateException.create("Cannot find any template for given class type");

		return template;
	}

	private String calculateTemplateKey(final String headerStart, final Charset charset){
		return JavaHelper.toHexString(headerStart.getBytes(charset));
	}


	/**
	 * Scans all classes accessible from the context class loader which belong to the given package.
	 *
	 * @param type	Whether a class or an interface (for example).
	 * @param basePackageClasses	A list of classes that resides in a base package(s).
	 * @return	The classes.
	 */
	private Collection<Class<?>> extractClasses(final Object type, final Class<?>... basePackageClasses){
		final Collection<Class<?>> classes = new HashSet<>(0);

		final ReflectiveClassLoader reflectiveClassLoader = ReflectiveClassLoader.createFrom(basePackageClasses);
		reflectiveClassLoader.scan(CodecInterface.class, MessageHeader.class);
		@SuppressWarnings("unchecked")
		final Collection<Class<?>> modules = reflectiveClassLoader.getImplementationsOf((Class<Object>)type);
		@SuppressWarnings("unchecked")
		final Collection<Class<?>> singletons = reflectiveClassLoader.getTypesAnnotatedWith((Class<? extends Annotation>)type);
		classes.addAll(modules);
		classes.addAll(singletons);
		return classes;
	}

	/**
	 * Tries to infer the next message start by scanning all templates in header-start-length order.
	 *
	 * @param reader	The reader.
	 * @return	The index of the next message.
	 */
	int findNextMessageIndex(final BitReader reader){
		int minOffset = -1;
		for(final Template<?> template : templates.values()){
			final MessageHeader header = template.getHeader();

			minOffset = findNextMessageIndex(reader, header, minOffset);
		}
		return minOffset;
	}

	private int findNextMessageIndex(final BitReader reader, final MessageHeader header, int minOffset){
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

	private int searchNextSequence(final BitReader reader, final byte[] startMessageSequence){
		final byte[] message = reader.array();
		final int startIndex = reader.position();
		final int[] preProcessedPattern = PRE_PROCESSED_PATTERNS.apply(startMessageSequence);
		final int index = PATTERN_MATCHER.indexOf(message, startIndex + 1, startMessageSequence, preProcessedPattern);
		return (index >= startIndex? index: -1);
	}

}
