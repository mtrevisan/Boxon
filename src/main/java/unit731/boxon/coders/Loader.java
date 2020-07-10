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
package unit731.boxon.coders;

import unit731.boxon.annotations.MessageHeader;
import unit731.boxon.annotations.exceptions.CodecException;
import unit731.boxon.helpers.AnnotationHelper;
import unit731.boxon.helpers.ByteHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import unit731.boxon.helpers.ReflectionHelper;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;


final class Loader{

	private static final Logger LOGGER = LoggerFactory.getLogger(Loader.class.getName());

	private final Map<String, Codec<?>> codecs = new TreeMap<>(Comparator.comparingInt(String::length).reversed().thenComparing(String::compareTo));
	private final Map<Class<?>, CoderInterface<?>> coders = new HashMap<>(0);

	private final AtomicBoolean initialized = new AtomicBoolean(false);


	Loader(){}

	/**
	 * Loads all the protocol classes annotated with {@link MessageHeader}.
	 * <p>This method should be called from a method inside a class that lies on a parent of all the protocol classes.</p>
	 */
	synchronized final void init(){
		init(extractCallerClasses());
	}

	/**
	 * Loads all the protocol classes annotated with {@link MessageHeader}.
	 * <p>This method should be called before instantiating a {@link Parser}.</p>
	 *
	 * @param basePackageClasses	Classes to be used ase starting point from which to load annotated classes
	 */
	synchronized final void init(final Class<?>... basePackageClasses){
		if(!initialized.get()){
			LOGGER.info("Load parsing classes from package(s) {}",
				Arrays.stream(basePackageClasses).map(Class::getPackageName).collect(Collectors.joining(", ", "[", "]")));

			final Collection<Class<?>> annotatedClasses = AnnotationHelper.extractClasses(MessageHeader.class, basePackageClasses);
			final Collection<Codec<?>> codecs = new ArrayList<>(annotatedClasses.size());
			for(final Class<?> type : annotatedClasses){
				final Codec<?> codec = Codec.createFrom(type);
				if(codec.canBeDecoded())
					codecs.add(codec);
			}
			loadCodecs(codecs);

			LOGGER.trace("Codecs loaded are {}", codecs.size());

			initialized.set(true);
		}
	}

	/**
	 * Loads all the protocol classes annotated with {@link MessageHeader}.
	 * <p>This method should be called before instantiating a {@link Parser}.</p>
	 *
	 * @param codecs	The list of codecs to be loaded
	 */
	synchronized final void init(final Collection<Codec<?>> codecs){
		if(!initialized.get()){
			LOGGER.info("Load parsing classes from input");

			loadCodecs(codecs);

			LOGGER.trace("Codecs loaded are {}", codecs.size());

			initialized.set(true);
		}
	}

	synchronized final boolean isInitialized(){
		return initialized.get();
	}

	private void loadCodecs(final Collection<Codec<?>> codecs){
		for(final Codec<?> codec : codecs){
			try{
				final MessageHeader header = codec.getHeader();
				final Charset charset = Charset.forName(header.charset());
				for(final String headerStart : header.start()){
					//calculate key
					final String key = ByteHelper.toHexString(headerStart.getBytes(charset));
					if(this.codecs.containsKey(key))
						throw new CodecException("Duplicate key `{}` found for class {}", headerStart, codec.getType().getSimpleName());

					this.codecs.put(key, codec);
				}
			}
			catch(final Exception e){
				LOGGER.error("Cannot load class {}", codec.getType().getSimpleName(), e);
			}
		}
	}

	final Codec<?> getCodec(final BitBuffer reader){
		final int index = reader.position();

		Codec<?> codec = null;
		for(final Map.Entry<String, Codec<?>> elem : codecs.entrySet()){
			final String header = elem.getKey();

			final byte[] codecHeader = ByteHelper.toByteArray(header);
			final byte[] messageHeader = Arrays.copyOfRange(reader.array(), index, index + codecHeader.length);

			//verify if it's a valid message header
			if(Arrays.equals(messageHeader, codecHeader)){
				codec = elem.getValue();
				break;
			}
		}
		if(codec == null)
			throw new CodecException("Cannot find any codec for message");

		return codec;
	}


	/**
	 * Loads all the coders that extends {@link CoderInterface}.
	 * <p>This method should be called from a method inside a class that lies on a parent of all the coders.</p>
	 */
	final void loadCoders(){
		loadCoders(extractCallerClasses());
	}

	/**
	 * Loads all the coders that extends {@link CoderInterface}.
	 *
	 * @param basePackageClasses	Classes to be used ase starting point from which to load coders
	 */
	final void loadCoders(final Class<?>... basePackageClasses){
		LOGGER.info("Load coders from package(s) {}",
			Arrays.stream(basePackageClasses).map(Class::getPackageName).collect(Collectors.joining(", ", "[", "]")));

		final Collection<Class<?>> derivedClasses = AnnotationHelper.extractClasses(CoderInterface.class, basePackageClasses);
		final Collection<CoderInterface<?>> coders = new ArrayList<>(derivedClasses.size());
		for(final Class<?> type : derivedClasses)
			if(!type.isInterface()){
				final CoderInterface<?> coder = (CoderInterface<?>)ReflectionHelper.getCreator(type)
					.get();
				if(coder != null)
					coders.add(coder);
			}
		for(final CoderInterface<?> coder : coders)
			addCoder(coder);

		LOGGER.trace("Coders loaded are {}", coders.size());
	}

	/**
	 * Loads all the coders that extends {@link CoderInterface}.
	 *
	 * @param coders	The list of coders to be loaded
	 */
	final void loadCoders(final Collection<CoderInterface<?>> coders){
		loadCoders(coders.toArray(CoderInterface[]::new));
	}

	/**
	 * Loads all the coders that extends {@link CoderInterface}.
	 *
	 * @param coders	The list of coders to be loaded
	 */
	final void loadCoders(final CoderInterface<?>... coders){
		LOGGER.info("Load coders from input");

		for(final CoderInterface<?> coder : coders)
			addCoder(coder);

		LOGGER.trace("Coders loaded are {}", coders.length);
	}

	/**
	 * Load a singe coder that extends {@link CoderInterface}.
	 * <p>If the parser previously contained a coder for the given key, the old coder is replaced by the specified one.</p>
	 *
	 * @param coder	The coder to add
	 * @return	The previous coder associated with {@link CoderInterface#coderType()}, or {@code null} if there was no previous coder.
	 */
	final CoderInterface<?> addCoder(final CoderInterface<?> coder){
		return coders.put(coder.coderType(), coder);
	}

	final CoderInterface<?> getCoder(final Class<?> type){
		return coders.get(type);
	}

	final int findNextMessageIndex(final BitBuffer reader){
		int minOffset = -1;
		for(final Codec<?> codec : codecs.values()){
			final MessageHeader header = codec.getHeader();
			final Charset charset = Charset.forName(header.charset());
			final String[] messageStarts = header.start();
			for(final String messageStart : messageStarts){
				final int offset = searchNextSequence(reader, messageStart.getBytes(charset));
				if(offset >= 0 && (minOffset < 0 || offset < minOffset))
					minOffset = offset;
			}
		}
		return minOffset;
	}

	private int searchNextSequence(final BitBuffer reader, final byte[] startMessageSequence){
		final int[] boundarySequenceLps = ByteHelper.indexOfComputeLPS(startMessageSequence);

		final byte[] message = reader.array();
		//search inside message:
		final int startIndex = reader.position();
		final int index = ByteHelper.indexOf(message, startMessageSequence, startIndex + 1, boundarySequenceLps);
		return (index >= startIndex? index: -1);
	}


	private Class<?>[] extractCallerClasses(){
		Class<?>[] classes = new Class[0];
		try{
			final StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
			final String callerClassName1 = stackTrace[2].getClassName();
			final String callerClassName2 = stackTrace[3].getClassName();
			classes = new Class[]{Class.forName(callerClassName1), Class.forName(callerClassName2)};
		}
		catch(final ClassNotFoundException ignored){}
		return classes;
	}

}
