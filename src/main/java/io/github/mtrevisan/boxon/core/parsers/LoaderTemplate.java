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

import io.github.mtrevisan.boxon.annotations.ContextParameter;
import io.github.mtrevisan.boxon.annotations.TemplateHeader;
import io.github.mtrevisan.boxon.annotations.bindings.BindAsArray;
import io.github.mtrevisan.boxon.annotations.bindings.BindAsList;
import io.github.mtrevisan.boxon.core.Parser;
import io.github.mtrevisan.boxon.core.codecs.LoaderCodec;
import io.github.mtrevisan.boxon.core.helpers.generators.AnnotationCreator;
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
import java.lang.annotation.ElementType;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Function;


/**
 * Loader for the templates.
 */
public final class LoaderTemplate{

	private static final PatternMatcher PATTERN_MATCHER = KMPPatternMatcher.getInstance();
	private static Function<byte[], int[]> PRE_PROCESSED_PATTERNS;
	static{
		initialize(Parser.UNBOUNDED_MEMOIZER_SIZE);
	}

	private static final Set<Class<? extends Annotation>> LIBRARY_ANNOTATIONS = ReflectiveClassLoader.extractAnnotations(
		ElementType.ANNOTATION_TYPE);

	private static final Collection<Class<? extends Annotation>> ANNOTATIONS_WITHOUT_CODEC = new HashSet<>(List.of(ContextParameter.class,
		BindAsArray.class, BindAsList.class));


	private final ThrowingFunction<Class<?>, Template<?>, AnnotationException> templateStore
		= Memoizer.throwingMemoize(type -> Template.create(type, this::filterAnnotationsWithCodec));

	private final Map<String, Template<?>> templates = new TreeMap<>(Comparator.comparingInt(String::length).reversed()
		.thenComparing(String::compareTo));

	private final LoaderCodec loaderCodec;

	private EventListener eventListener;


	/**
	 * Create a template parser.
	 *
	 * @param loaderCodec	A codec loader.
	 * @return	A template parser.
	 */
	static LoaderTemplate create(final LoaderCodec loaderCodec){
		return new LoaderTemplate(loaderCodec);
	}


	private LoaderTemplate(final LoaderCodec loaderCodec){
		this.loaderCodec = loaderCodec;

		withEventListener(null);
	}


	public static void initialize(final int maxTemplateMemoizerSize){
		PRE_PROCESSED_PATTERNS = Memoizer.memoize(PATTERN_MATCHER::preProcessPattern, maxTemplateMemoizerSize);
	}


	/**
	 * Assign an event listener.
	 *
	 * @param eventListener	The event listener.
	 */
	void withEventListener(final EventListener eventListener){
		this.eventListener = (eventListener != null? eventListener: EventListener.getNoOpInstance());
	}

	/**
	 * Loads all the protocol classes annotated with {@link TemplateHeader}.
	 *
	 * @param basePackageClasses	Classes to be used ase starting point from which to load annotated classes.
	 * @throws AnnotationException	If an annotation error occurs.
	 * @throws TemplateException	If the template was already added (defined by `start` parameter in the header definition).
	 */
	void loadTemplatesFrom(final Class<?>... basePackageClasses) throws AnnotationException, TemplateException{
		eventListener.loadingTemplatesFrom(basePackageClasses);

		final List<Class<?>> annotatedClasses = getAnnotatedClasses(basePackageClasses);
		final Template<?>[] templates = extractValidTemplates(annotatedClasses);
		addTemplatesToMap(templates);

		eventListener.loadedTemplates(templates.length);
	}

	/** Extract all classes annotated with {@link TemplateHeader}. */
	private static List<Class<?>> getAnnotatedClasses(final Class<?>[] basePackageClasses){
		final ReflectiveClassLoader reflectiveClassLoader = ReflectiveClassLoader.createFrom(basePackageClasses);
		return reflectiveClassLoader.extractClassesWithAnnotation(TemplateHeader.class);
	}

	/**
	 * Load the specified protocol class annotated with {@link TemplateHeader}.
	 *
	 * @param templateClass	Template class.
	 * @throws AnnotationException	If an annotation error occurs.
	 * @throws TemplateException	If a template error occurs.
	 */
	void loadTemplate(final Class<?> templateClass) throws AnnotationException, TemplateException{
		eventListener.loadingTemplate(templateClass);

		if(templateClass.isAnnotationPresent(TemplateHeader.class)){
			/** extract all classes annotated with {@link TemplateHeader}. */
			final Template<?> template = extractTemplate(templateClass);
			if(template.canBeCoded()){
				addTemplateToMap(template);

				eventListener.loadedTemplates(templates.size());
			}
		}
	}

	private Template<?>[] extractValidTemplates(final List<Class<?>> annotatedClasses) throws AnnotationException, TemplateException{
		final int size = annotatedClasses.size();
		final Template<?>[] templates = new Template<?>[size];
		for(int i = 0; i < size; i ++){
			final Class<?> type = annotatedClasses.get(i);

			final Template<?> from = extractTemplate(type);

			templates[i] = from;
		}
		return templates;
	}

	/**
	 * Extract a template for the given class.
	 *
	 * @param type	The class type.
	 * @return	A template.
	 * @throws AnnotationException	If an annotation error occurs.
	 * @throws TemplateException	If a template error occurs.
	 */
	Template<?> extractTemplate(final Class<?> type) throws AnnotationException, TemplateException{
		final Template<?> from = createTemplate(type);
		validate(from, type);

		return from;
	}

	private static void validate(final Template<?> from, final Class<?> type) throws TemplateException{
		if(!from.canBeCoded())
			throw TemplateException.create("Cannot create a raw message from data: cannot scan template for {}",
				type.getSimpleName());
	}

	/**
	 * Constructs a new {@link Template}.
	 *
	 * @param <T>	The type of the object to be returned as a {@link Template}.
	 * @param type	The class of the object to be returned as a {@link Template}.
	 * @return	The {@link Template} for the given type.
	 * @throws AnnotationException	If an annotation error occurs.
	 */
	<T> Template<T> createTemplate(final Class<T> type) throws AnnotationException{
		return (Template<T>)templateStore.apply(type);
	}

	private void addTemplatesToMap(final Template<?>[] templates) throws TemplateException{
		//load each template into the available templates list
		for(int i = 0, length = templates.length; i < length; i ++){
			final Template<?> template = templates[i];

			if(template != null && template.canBeCoded())
				addTemplateToMap(template);
		}
	}

	/**
	 * For each valid template, add it to the map of templates indexed by starting message bytes.
	 *
	 * @param template	The template to add to the list of available templates.
	 * @throws TemplateException	If the template was already added (defined by `start` parameter in the header definition).
	 */
	private void addTemplateToMap(final Template<?> template) throws TemplateException{
		try{
			final TemplateHeader header = template.getHeader();
			final Charset charset = CharsetHelper.lookup(header.charset());
			final String[] starts = header.start();
			for(int i = 0, length = starts.length; i < length; i ++)
				processTemplate(template, starts[i], charset);
		}
		catch(final TemplateException te){
			eventListener.cannotLoadTemplate(template.getName(), te);

			throw te;
		}
	}

	private void processTemplate(final Template<?> template, final String headerStart, final Charset charset) throws TemplateException{
		final String key = calculateKey(headerStart, charset);
		if(templates.containsKey(key))
			throw TemplateException.create("Duplicated key `{}` found for class {}", headerStart, template.getName());

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

			//verify if it's a valid message header
			final int length = header.length() >>> 1;
			if(index + length <= array.length && byteArrayHexStringEquals(array, index, header, length))
				return entry.getValue();
		}

		throw TemplateException.create("Cannot find any template for given raw message");
	}

	private static boolean byteArrayHexStringEquals(final byte[] byteArray, final int arrayFromIndex, final CharSequence hexString,
			final int length){
		for(int i = 0, j = arrayFromIndex, k = 0; i < length; i ++, j ++, k += 2){
			final int byteValue = byteArray[j] & 0xFF;
			final int highDigit = Character.digit(hexString.charAt(k), 16);
			final int lowDigit = Character.digit(hexString.charAt(k + 1), 16);
			final int hexValue = (highDigit << 4) | lowDigit;
			if(byteValue != hexValue)
				return false;
		}
		return true;
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

		//NOTE: we want only one template, so we pick the first `start`
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
	Collection<Template<?>> getTemplates(){
		return Collections.unmodifiableCollection(templates.values());
	}

	private static String calculateKey(final String headerStart, final Charset charset){
		return StringHelper.toHexString(headerStart.getBytes(charset));
	}

	private List<Annotation> filterAnnotationsWithCodec(final Annotation[] declaredAnnotations){
		final int length = declaredAnnotations.length;
		final List<Annotation> annotations = JavaHelper.createListOrEmpty(length);
		for(int i = 0; i < length; i ++){
			final Annotation declaredAnnotation = declaredAnnotations[i];

			final Class<? extends Annotation> annotationType = declaredAnnotation.annotationType();
			final Annotation[] parentAnnotations = annotationType.getAnnotations();
			final Annotation parentAnnotation = findParentAnnotation(parentAnnotations);
			if(parentAnnotation != null){
				final Annotation annotation = createAnnotationWithDefaults(declaredAnnotation, parentAnnotation);
				annotations.add(annotation);
			}
			else if(shouldIncludeAnnotation(annotationType))
				annotations.add(declaredAnnotation);
		}
		return annotations;
	}

	private static Annotation createAnnotationWithDefaults(final Annotation declaredAnnotation, final Annotation parentAnnotation){
		final Map<String, Object> parentValues = AnnotationCreator.extractAnnotationValues(parentAnnotation);
		final Map<String, Object> values = AnnotationCreator.extractAnnotationValues(declaredAnnotation);
		populateDefaultValues(values, parentValues);
		//create annotation of type `foundAnnotation` with the defaults written in the annotation of `declaredAnnotation` and
		// parameters from `declaredAnnotation`
		return AnnotationCreator.createAnnotation(parentAnnotation.annotationType(), values);
	}

	private static void populateDefaultValues(final Map<String, Object> values, final Map<String, Object> parentValues){
		//replace with default parent values
		for(final Map.Entry<String, Object> entry : parentValues.entrySet()){
			final String key = entry.getKey();
			final Object value = entry.getValue();

			values.putIfAbsent(key, value);
		}
	}

	private static Annotation findParentAnnotation(final Annotation[] parentAnnotations){
		for(int j = 0, length = parentAnnotations.length; j < length; j ++){
			final Annotation parentAnnotation = parentAnnotations[j];
			if(LIBRARY_ANNOTATIONS.contains(parentAnnotation.annotationType()))
				return parentAnnotation;
		}
		return null;
	}

	private boolean shouldIncludeAnnotation(final Class<? extends Annotation> annotationType){
		return (loaderCodec.hasCodec(annotationType) || ANNOTATIONS_WITHOUT_CODEC.contains(annotationType));
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
		final String[] starts = header.start();
		//select the minimum index with a valid template
		for(int i = 0, length = starts.length; i < length; i ++){
			final byte[] startMessageSequence = starts[i].getBytes(charset);
			final int offset = searchNextSequence(reader, startMessageSequence);
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
