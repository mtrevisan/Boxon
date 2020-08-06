/**
 * Copyright (c) 2020 Mauro Trevisan
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
import io.github.mtrevisan.boxon.annotations.exceptions.TemplateException;
import io.github.mtrevisan.boxon.helpers.AnnotationHelper;
import io.github.mtrevisan.boxon.helpers.ByteHelper;
import io.github.mtrevisan.boxon.helpers.DynamicArray;
import io.github.mtrevisan.boxon.helpers.Memoizer;
import io.github.mtrevisan.boxon.helpers.ReflectionHelper;
import io.github.mtrevisan.boxon.helpers.matchers.BNDMPatternMatcher;
import io.github.mtrevisan.boxon.helpers.matchers.PatternMatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;


final class Loader{

	private static final Logger LOGGER = LoggerFactory.getLogger(Loader.class);

	private static final Function<byte[], int[]> PRE_PROCESSED_PATTERNS = Memoizer.memoizeThreadAndRecursionSafe(Loader::getPreProcessedPattern);
	private static final PatternMatcher PATTERN_MATCHER = new BNDMPatternMatcher();

	private final Map<String, Template<?>> templates = new TreeMap<>(Comparator.comparingInt(String::length).reversed().thenComparing(String::compareTo));
	private final Map<Class<?>, CodecInterface<?>> codecs = new HashMap<>(0);


	Loader(){}


	/**
	 * Loads all the codecs that extends {@link CodecInterface}.
	 * <p>This method should be called from a method inside a class that lies on a parent of all the codecs.</p>
	 */
	final void loadDefaultCodecs(){
		loadCodecs(ReflectionHelper.extractCallerClasses());
	}

	/**
	 * Loads all the codecs that extends {@link CodecInterface}.
	 *
	 * @param basePackageClasses	Classes to be used ase starting point from which to load codecs
	 */
	final void loadCodecs(final Class<?>... basePackageClasses){
		if(LOGGER.isInfoEnabled())
			LOGGER.info("Load codecs from package(s) {}",
				Arrays.stream(basePackageClasses).map(Class::getPackageName).collect(Collectors.joining(", ", "[", "]")));

		/** extract all classes that implements {@link CodecInterface} */
		final Collection<Class<?>> derivedClasses = AnnotationHelper.extractClasses(CodecInterface.class, basePackageClasses);
		final DynamicArray<CodecInterface> codecs = extractCodecs(derivedClasses);
		addCodecsInner(codecs.data);

		LOGGER.trace("Codecs loaded are {}", codecs.limit);
	}

	@SuppressWarnings("rawtypes")
	private DynamicArray<CodecInterface> extractCodecs(final Collection<Class<?>> derivedClasses){
		final DynamicArray<CodecInterface> codecs = DynamicArray.create(CodecInterface.class, derivedClasses.size());
		for(final Class<?> type : derivedClasses){
			//for each extracted class, try to create an instance
			final CodecInterface<?> codec = (CodecInterface<?>)ReflectionHelper.getCreator(type)
				.get();
			if(codec != null)
				//if the codec was created successfully instanced, add it to the list of codecs...
				codecs.add(codec);
			else
				//... otherwise warn
				LOGGER.warn("Cannot create an instance of codec {}", type.getSimpleName());
		}
		return codecs;
	}

	/**
	 * Loads all the given codecs that extends {@link CodecInterface}.
	 * <p>NOTE: If the loader previously contains a codec for a given key, the old codec is replaced by the new one.</p>
	 *
	 * @param codecs	The list of codecs to be loaded
	 */
	final void addCodecs(final CodecInterface<?>... codecs){
		Objects.requireNonNull(codecs);

		LOGGER.info("Load given codecs");

		addCodecsInner(codecs);

		LOGGER.trace("Codecs loaded are {}", codecs.length);
	}

	private void addCodecsInner(final CodecInterface<?>[] codecs){
		//load each codec into the available codec list
		for(int i = 0; i < codecs.length; i ++)
			if(codecs[i] != null)
				addCodecInner(codecs[i]);
	}

	private void addCodecInner(final CodecInterface<?> codec){
		codecs.put(codec.codecType(), codec);
	}

	final boolean hasCodec(final Class<?> type){
		return (getCodec(type) != null);
	}

	final CodecInterface<?> getCodec(final Class<?> type){
		return codecs.get(type);
	}


	/**
	 * Loads all the protocol classes annotated with {@link MessageHeader}.
	 * <p>This method should be called from a method inside a class that lies on a parent of all the protocol classes.</p>
	 *
	 * @throws IllegalArgumentException	If the codecs was not loaded yet,
	 */
	final void loadDefaultTemplates(){
		loadTemplates(ReflectionHelper.extractCallerClasses());
	}

	/**
	 * Loads all the protocol classes annotated with {@link MessageHeader}.
	 *
	 * @param basePackageClasses	Classes to be used ase starting point from which to load annotated classes
	 */
	final void loadTemplates(final Class<?>... basePackageClasses){
		if(LOGGER.isInfoEnabled())
			LOGGER.info("Load parsing classes from package(s) {}",
				Arrays.stream(basePackageClasses).map(Class::getPackageName).collect(Collectors.joining(", ", "[", "]")));

		/** extract all classes annotated with {@link MessageHeader} */
		final Collection<Class<?>> annotatedClasses = AnnotationHelper.extractClasses(MessageHeader.class, basePackageClasses);
		@SuppressWarnings("rawtypes")
		final DynamicArray<Template> templates = extractTemplates(annotatedClasses);
		addTemplatesInner(templates.data);

		LOGGER.trace("Templates loaded are {}", templates.limit);
	}

	@SuppressWarnings("rawtypes")
	private DynamicArray<Template> extractTemplates(final Collection<Class<?>> annotatedClasses){
		final DynamicArray<Template> templates = DynamicArray.create(Template.class, annotatedClasses.size());
		for(final Class<?> type : annotatedClasses){
			//for each extracted class, try to parse it, extracting all the information needed for the codec of a message
			final Template<?> from = Template.createFrom(type, this);
			if(from.canBeCoded())
				//if the template is valid, add it to the list of templates...
				templates.add(from);
			else
				//... otherwise throw exception
				throw new TemplateException("Cannot create a raw message from data: cannot scan template");
		}
		return templates;
	}

	/**
	 * Adds all the protocol classes annotated with {@link MessageHeader}.
	 * <p>NOTE: If the loader previously contains a template for a given key, the old template is replaced by the new one.</p>
	 *
	 * @param templates	The list of templates to be loaded
	 */
	final void addTemplates(final Template<?>... templates){
		LOGGER.info("Load given templates");

		addTemplatesInner(templates);

		LOGGER.trace("Templates loaded are {}", templates.length);
	}

	private void addTemplatesInner(final Template<?>[] templates){
		//load each template into the available templates list
		for(int i = 0; i < templates.length; i ++)
			if(templates[i] != null)
				addTemplateInner(templates[i]);
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
			LOGGER.error("Cannot load class {}", template.getType().getSimpleName(), e);
		}
	}

	private void loadTemplateInner(final Template<?> template, final String headerStart, final Charset charset){
		final String key = calculateTemplateKey(headerStart, charset);
		if(this.templates.containsKey(key))
			throw new TemplateException("Duplicated key `{}` found for class {}", headerStart, template.getType().getSimpleName());

		this.templates.put(key, template);
	}

	/**
	 * Retrieve the next template.
	 *
	 * @param reader	The reader to read the header from.
	 * @return	The template that is able to decode/encode the next message in the given reader.
	 */
	final Template<?> getTemplate(final BitReader reader){
		final int index = reader.position();

		//for each available template, select the first that matches the starting bytes
		//note that the templates are ordered by the length of the starting bytes, descending, so the first that matches is that
		//with the longest match
		for(final Map.Entry<String, Template<?>> entry : templates.entrySet()){
			final String header = entry.getKey();
			final byte[] templateHeader = ByteHelper.toByteArray(header);

			//verify if it's a valid message header
			if(Arrays.equals(reader.array(), index, index + templateHeader.length, templateHeader, 0, templateHeader.length))
				return entry.getValue();
		}

		throw new TemplateException("Cannot find any template for given raw message");
	}

	/**
	 * Retrieve the next template by class.
	 *
	 * @param type	The class to retrieve the template.
	 * @return	The template that is able to decode/encode the given class.
	 */
	final Template<?> getTemplate(final Class<?> type) throws UnsupportedEncodingException{
		final MessageHeader header = type.getAnnotation(MessageHeader.class);
		if(header == null)
			throw new TemplateException("The given class type is not a valid template");

		final String key = calculateTemplateKey(header.start()[0], Charset.forName(header.charset()));
		final Template<?> template = templates.get(key);
		if(template == null)
			throw new TemplateException("Cannot find any template for given class type");

		return template;
	}

	private String calculateTemplateKey(final String headerStart, final Charset charset){
		return ByteHelper.toHexString(headerStart.getBytes(charset));
	}


	/**
	 * Tries to infer the next message start by scanning all templates in header-start-length order.
	 *
	 * @param reader	The reader.
	 * @return	The index of the next message.
	 */
	final int findNextMessageIndex(final BitReader reader){
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

	private static int[] getPreProcessedPattern(final byte[] pattern){
		return PATTERN_MATCHER.preProcessPattern(pattern);
	}

}
