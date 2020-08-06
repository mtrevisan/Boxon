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
import io.github.mtrevisan.boxon.annotations.exceptions.ProtocolMessageException;
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

	private final Map<String, ProtocolMessage<?>> protocolMessages = new TreeMap<>(Comparator.comparingInt(String::length).reversed().thenComparing(String::compareTo));
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

		final Collection<Class<?>> derivedClasses = AnnotationHelper.extractClasses(CodecInterface.class, basePackageClasses);
		@SuppressWarnings("rawtypes")
		final DynamicArray<CodecInterface> codecs = DynamicArray.create(CodecInterface.class, derivedClasses.size());
		for(final Class<?> type : derivedClasses){
			final CodecInterface<?> codec = (CodecInterface<?>)ReflectionHelper.getCreator(type)
				.get();
			if(codec != null)
				codecs.add(codec);
			else
				LOGGER.warn("Cannot create an instance of codec {}", type.getSimpleName());
		}
		addCodecsInner(codecs.data);

		LOGGER.trace("Codecs loaded are {}", codecs.limit);
	}

	/**
	 * Loads all the given codecs that extends {@link CodecInterface}.
	 * <p>NOTE: If the loader previously contains a codec for a given key, the old codec is replaced by the new one.</p>
	 *
	 * @param codecs	The list of codecs to be loaded
	 */
	final void addCodecs(final CodecInterface<?>... codecs){
		Objects.requireNonNull(codecs);

		LOGGER.info("Load codecs from input");

		addCodecsInner(codecs);

		LOGGER.trace("Codecs loaded are {}", codecs.length);
	}

	private void addCodecsInner(final CodecInterface<?>[] codecs){
		for(int i = 0; i < codecs.length; i ++){
			final CodecInterface<?> codec = codecs[i];
			if(codec != null)
				this.codecs.put(codec.codecType(), codec);
		}
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
	final void loadDefaultProtocolMessages(){
		loadProtocolMessages(ReflectionHelper.extractCallerClasses());
	}

	/**
	 * Loads all the protocol classes annotated with {@link MessageHeader}.
	 *
	 * @param basePackageClasses	Classes to be used ase starting point from which to load annotated classes
	 */
	final void loadProtocolMessages(final Class<?>... basePackageClasses){
		if(LOGGER.isInfoEnabled())
			LOGGER.info("Load parsing classes from package(s) {}",
				Arrays.stream(basePackageClasses).map(Class::getPackageName).collect(Collectors.joining(", ", "[", "]")));

		final Collection<Class<?>> annotatedClasses = AnnotationHelper.extractClasses(MessageHeader.class, basePackageClasses);
		@SuppressWarnings("rawtypes")
		final DynamicArray<ProtocolMessage> protocolMessages = extractProtocolMessages(annotatedClasses);
		addProtocolMessagesInner(protocolMessages.data);

		LOGGER.trace("Protocol messages loaded are {}", protocolMessages.limit);
	}

	@SuppressWarnings("rawtypes")
	private DynamicArray<ProtocolMessage> extractProtocolMessages(final Collection<Class<?>> annotatedClasses){
		final DynamicArray<ProtocolMessage> protocolMessages = DynamicArray.create(ProtocolMessage.class, annotatedClasses.size());
		for(final Class<?> type : annotatedClasses){
			final ProtocolMessage<?> from = ProtocolMessage.createFrom(type, this);
			if(from.canBeCoded())
				protocolMessages.add(from);
			else
				throw new ProtocolMessageException("Cannot create a raw message from data: cannot scan protocol message");
		}
		return protocolMessages;
	}

	/**
	 * Adds all the protocol classes annotated with {@link MessageHeader}.
	 * <p>NOTE: If the loader previously contains a protocol message for a given key, the old protocol message is replaced by the new one.</p>
	 *
	 * @param protocolMessages	The list of protocol messages to be loaded
	 */
	final void addProtocolMessages(final ProtocolMessage<?>... protocolMessages){
		LOGGER.info("Load parsing classes from input");

		addProtocolMessagesInner(protocolMessages);

		LOGGER.trace("Protocol messages loaded are {}", protocolMessages.length);
	}

	private void addProtocolMessagesInner(final ProtocolMessage<?>[] protocolMessages){
		for(int i = 0; i < protocolMessages.length; i ++)
			if(protocolMessages[i] != null){
				try{
					loadProtocolMessageInner(protocolMessages[i]);
				}
				catch(final Exception e){
					LOGGER.error("Cannot load class {}", protocolMessages[i].getType().getSimpleName(), e);
				}
			}
	}

	private void loadProtocolMessageInner(final ProtocolMessage<?> protocolMessage){
		final MessageHeader header = protocolMessage.getHeader();
		final Charset charset = Charset.forName(header.charset());
		final String[] starts = header.start();
		for(int i = 0; i < starts.length; i ++){
			final String headerStart = starts[i];
			//calculate key
			final String key = ByteHelper.toHexString(headerStart.getBytes(charset));
			if(this.protocolMessages.containsKey(key))
				throw new ProtocolMessageException("Duplicate key `{}` found for class {}", headerStart, protocolMessage.getType().getSimpleName());

			this.protocolMessages.put(key, protocolMessage);
		}
	}

	final ProtocolMessage<?> getProtocolMessage(final BitReader reader){
		final int index = reader.position();

		for(final Map.Entry<String, ProtocolMessage<?>> entry : protocolMessages.entrySet()){
			final String header = entry.getKey();
			final byte[] protocolMessageHeader = ByteHelper.toByteArray(header);

			//verify if it's a valid message header
			if(Arrays.equals(reader.array(), index, index + protocolMessageHeader.length, protocolMessageHeader, 0, protocolMessageHeader.length))
				return entry.getValue();
		}

		throw new ProtocolMessageException("Cannot find any protocol message for given raw message");
	}

	final ProtocolMessage<?> getProtocolMessage(final Class<?> type) throws UnsupportedEncodingException{
		final MessageHeader header = type.getAnnotation(MessageHeader.class);
		if(header == null)
			throw new ProtocolMessageException("The given class type is not a valid protocol message");

		//calculate key
		final String key = ByteHelper.toHexString(header.start()[0].getBytes(header.charset()));

		final ProtocolMessage<?> protocolMessage = protocolMessages.get(key);
		if(protocolMessage == null)
			throw new ProtocolMessageException("Cannot find any protocol message for given class type");

		return protocolMessage;
	}


	final int findNextMessageIndex(final BitReader reader){
		int minOffset = -1;
		for(final ProtocolMessage<?> protocolMessage : protocolMessages.values()){
			final MessageHeader header = protocolMessage.getHeader();

			minOffset = findNextMessageIndex(reader, header, minOffset);
		}
		return minOffset;
	}

	private int findNextMessageIndex(final BitReader reader, final MessageHeader header, int minOffset){
		final Charset charset = Charset.forName(header.charset());
		final String[] messageStarts = header.start();
		for(int i = 0; i < messageStarts.length; i ++){
			final int offset = searchNextSequence(reader, messageStarts[i].getBytes(charset));
			if(offset >= 0 && (minOffset < 0 || offset < minOffset))
				minOffset = offset;
		}
		return minOffset;
	}

	private static int[] getPreProcessedPattern(final byte[] pattern){
		return PATTERN_MATCHER.preProcessPattern(pattern);
	}

	private int searchNextSequence(final BitReader reader, final byte[] startMessageSequence){
		final int[] preProcessedPattern = PRE_PROCESSED_PATTERNS.apply(startMessageSequence);

		final byte[] message = reader.array();
		//search inside message:
		final int startIndex = reader.position();
		final int index = PATTERN_MATCHER.indexOf(message, startIndex + 1, startMessageSequence, preProcessedPattern);
		return (index >= startIndex? index: -1);
	}

}
