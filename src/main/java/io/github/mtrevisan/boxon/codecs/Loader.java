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
import io.github.mtrevisan.boxon.helpers.Memoizer;
import io.github.mtrevisan.boxon.helpers.ReflectionHelper;
import io.github.mtrevisan.boxon.helpers.matchers.BNDMPatternMatcher;
import io.github.mtrevisan.boxon.helpers.matchers.PatternMatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;


final class Loader{

	private static final Logger LOGGER = LoggerFactory.getLogger(Loader.class.getName());

	private static final Function<byte[], int[]> PRE_PROCESSED_PATTERNS = Memoizer.memoizeThreadAndRecursionSafe(Loader::getPreProcessedPattern);
	private static final PatternMatcher PATTERN_MATCHER = new BNDMPatternMatcher();

	private final Map<String, ProtocolMessage<?>> protocolMessages = new TreeMap<>(Comparator.comparingInt(String::length).reversed().thenComparing(String::compareTo));
	private final Map<Class<?>, CodecInterface<?>> codecs = new HashMap<>(0);


	Loader(){}


	/**
	 * Loads all the codecs that extends {@link CodecInterface}.
	 * <p>This method should be called from a method inside a class that lies on a parent of all the codecs.</p>
	 */
	final void loadCodecs(){
		loadCodecs(extractCallerClasses());
	}

	/**
	 * Loads all the codecs that extends {@link CodecInterface}.
	 *
	 * @param basePackageClasses	Classes to be used ase starting point from which to load codecs
	 */
	final void loadCodecs(Class<?>... basePackageClasses){
		//remove duplicates
		basePackageClasses = Arrays.stream(basePackageClasses)
			.filter(distinctByKey(Class::getPackageName))
			.toArray(Class[]::new);

		LOGGER.info("Load codecs from package(s) {}",
			Arrays.stream(basePackageClasses).map(Class::getPackageName).collect(Collectors.joining(", ", "[", "]")));

		final Collection<Class<?>> derivedClasses = AnnotationHelper.extractClasses(CodecInterface.class, basePackageClasses);
		for(final Class<?> type : derivedClasses){
			final CodecInterface<?> codec = (CodecInterface<?>)ReflectionHelper.getCreator(type)
				.get();
			if(codec == null)
				LOGGER.warn("Cannot create an instance of codec {}", type.getSimpleName());
			else
				addCodec(codec);
		}

		LOGGER.trace("Codecs loaded are {}", codecs.size());
	}

	private static <T> Predicate<T> distinctByKey(final Function<? super T, ?> keyExtractor){
		final Set<Object> seen = ConcurrentHashMap.newKeySet();
		return t -> seen.add(keyExtractor.apply(t));
	}

	/**
	 * Loads all the codecs that extends {@link CodecInterface}.
	 *
	 * @param codecs	The list of codecs to be loaded
	 */
	final void loadCodecs(final Collection<CodecInterface<?>> codecs){
		loadCodecs(codecs.toArray(CodecInterface[]::new));
	}

	/**
	 * Loads all the given codecs that extends {@link CodecInterface}.
	 *
	 * @param codecs	The list of codecs to be loaded
	 */
	final void loadCodecs(final CodecInterface<?>... codecs){
		LOGGER.info("Load codecs from input");

		for(int i = 0; i < codecs.length; i ++)
			addCodec(codecs[i]);

		LOGGER.trace("Codecs loaded are {}", codecs.length);
	}

	/**
	 * Load a singe codec that extends {@link CodecInterface}.
	 * <p>If the parser previously contained a codec for the given key, the old codec is replaced by the specified one.</p>
	 *
	 * @param codec   The codec to add
	 */
	private void addCodec(final CodecInterface<?> codec){
		codecs.put(codec.codecType(), codec);
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
	synchronized final void loadProtocolMessages(){
		loadProtocolMessages(extractCallerClasses());
	}

	/**
	 * Loads all the protocol classes annotated with {@link MessageHeader}.
	 *
	 * @param basePackageClasses	Classes to be used ase starting point from which to load annotated classes
	 */
	synchronized final void loadProtocolMessages(Class<?>... basePackageClasses){
		//remove duplicates
		basePackageClasses = Arrays.stream(basePackageClasses)
			.filter(distinctByKey(Class::getPackageName))
			.toArray(Class[]::new);

		LOGGER.info("Load parsing classes from package(s) {}",
			Arrays.stream(basePackageClasses).map(Class::getPackageName).collect(Collectors.joining(", ", "[", "]")));

		final Collection<Class<?>> annotatedClasses = AnnotationHelper.extractClasses(MessageHeader.class, basePackageClasses);
		final Collection<ProtocolMessage<?>> protocolMessages = annotatedClasses.stream()
			.map(type -> ProtocolMessage.createFrom(type, this))
			.filter(ProtocolMessage::canBeDecoded)
			.collect(Collectors.toList());
		loadProtocolMessagesInner(protocolMessages);

		LOGGER.trace("Protocol messages loaded are {}", protocolMessages.size());
	}

	/**
	 * Loads all the protocol classes annotated with {@link MessageHeader}.
	 *
	 * @param protocolMessages	The list of protocol messages to be loaded
	 */
	synchronized final void loadProtocolMessages(Collection<ProtocolMessage<?>> protocolMessages){
		//remove duplicates
		protocolMessages = protocolMessages.stream()
			.distinct()
			.collect(Collectors.toList());

		LOGGER.info("Load parsing classes from input");

		loadProtocolMessagesInner(protocolMessages);

		LOGGER.trace("Protocol messages loaded are {}", protocolMessages.size());
	}

	private void loadProtocolMessagesInner(final Collection<ProtocolMessage<?>> protocolMessages){
		for(final ProtocolMessage<?> protocolMessage : protocolMessages){
			try{
				loadProtocolMessageInner(protocolMessage);
			}
			catch(final Exception e){
				LOGGER.error("Cannot load class {}", protocolMessage.getType().getSimpleName(), e);
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

		ProtocolMessage<?> protocolMessage = null;
		for(final Map.Entry<String, ProtocolMessage<?>> elem : protocolMessages.entrySet()){
			final String header = elem.getKey();

			final byte[] protocolMessageHeader = ByteHelper.toByteArray(header);
			final byte[] messageHeader = Arrays.copyOfRange(reader.array(), index, index + protocolMessageHeader.length);

			//verify if it's a valid message header
			if(Arrays.equals(messageHeader, protocolMessageHeader)){
				protocolMessage = elem.getValue();
				break;
			}
		}
		if(protocolMessage == null)
			throw new ProtocolMessageException("Cannot find any protocol message for message");

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


	private Class<?>[] extractCallerClasses(){
		Class<?>[] classes = new Class[0];
		try{
			final StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
			final Class<?> callerClass1 = Class.forName(stackTrace[2].getClassName());
			final Class<?> callerClass2 = Class.forName(stackTrace[3].getClassName());
			classes = new Class[]{callerClass1, callerClass2};
		}
		catch(final ClassNotFoundException ignored){}
		return classes;
	}

}
