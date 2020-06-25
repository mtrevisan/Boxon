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
package unit731.boxon.codecs;

import unit731.boxon.annotations.MessageHeader;
import unit731.boxon.helpers.AnnotationHelper;
import unit731.boxon.helpers.ByteHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicBoolean;


class Loader{

	private static final Logger LOGGER = LoggerFactory.getLogger(Loader.class.getName());

	private final Map<String, Codec<?>> codecs = new TreeMap<>(Comparator.comparingInt(String::length).reversed().thenComparing(String::compareTo));

	private final AtomicBoolean initialized = new AtomicBoolean(false);


	Loader(){}

	/** This method should be called from a method inside a class that lies on a parent of all the decoders */
	synchronized void init(){
		try{
			final String callerClassName = Thread.currentThread().getStackTrace()[3].getClassName();
			init(Class.forName(callerClassName));
		}
		catch(final ClassNotFoundException ignored){}
	}

	/**
	 * This method should be called before instantiating a {@link Parser}
	 *
	 * @param basePackageClasses	Classes to be used ase starting point from which to load annotated classes
	 */
	synchronized void init(final Class<?>... basePackageClasses){
		if(!initialized.get()){
			LOGGER.info("Load parsing classes from package {}",
				Arrays.toString(Arrays.stream(basePackageClasses).map(Class::getName)
					.map(name -> name.substring(0, name.lastIndexOf('.'))).toArray(String[]::new)));

			final Collection<Class<?>> annotatedClasses = AnnotationHelper.extractAnnotatedClasses(MessageHeader.class, basePackageClasses);
			final Collection<Codec<?>> codecs = new ArrayList<>();
			for(final Class<?> cls : annotatedClasses){
				final Codec<?> codec = Codec.createFrom(cls);
				if(codec.canBeDecoded())
					codecs.add(codec);
			}
			loadCodecs(codecs);

			LOGGER.trace("Codecs loaded are {}", codecs.size());

			initialized.set(true);
		}
	}

	/**
	 * This method should be called before instantiating a {@link Parser}
	 *
	 * @param codecs	The list of codecs to be loaded
	 */
	synchronized void init(final Collection<Codec<?>> codecs){
		if(!initialized.get()){
			LOGGER.info("Load parsing classes from method");

			loadCodecs(codecs);

			LOGGER.trace("Codecs loaded are {}", codecs.size());

			initialized.set(true);
		}
	}

	synchronized boolean isInitialized(){
		return initialized.get();
	}

	private void loadCodecs(final Collection<Codec<?>> codecs){
		for(final Codec<?> codec : codecs){
			try{
				final MessageHeader header = codec.getHeader();
				final Charset charset = Charset.forName(header.charset());
				for(final String headerStart : header.start()){
					//calculate key
					final String key = ByteHelper.byteArrayToHexString(headerStart.getBytes(charset));
					if(this.codecs.containsKey(key))
						throw new IllegalArgumentException("Duplicate key `" + headerStart + "` found for class "
							+ codec.getType().getSimpleName());

					this.codecs.put(key, codec);
				}
			}
			catch(final Exception e){
				LOGGER.error("Cannot load class {}", codec.getType().getSimpleName(), e);
			}
		}
	}

	Codec<?> getCodec(final BitBuffer reader){
		final int index = reader.positionAsBits() / Byte.SIZE;

		Codec<?> codec = null;
		for(final Map.Entry<String, Codec<?>> codecElem : codecs.entrySet()){
			final String header = codecElem.getKey();

			final byte[] codecHeader = ByteHelper.hexStringToByteArray(header);
			final byte[] messageHeader = Arrays.copyOfRange(reader.array(), index, index + codecHeader.length);

			//verify if it's a valid message header
			if(Arrays.equals(messageHeader, codecHeader)){
				codec = codecElem.getValue();
				break;
			}
		}
		if(codec == null)
			throw new IllegalArgumentException("Cannot find any codec for message");

		return codec;
	}

	int findNextMessageIndex(final BitBuffer reader){
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
		final int startIndex = reader.positionAsBits() / Byte.SIZE;
		final int index = ByteHelper.indexOf(message, startMessageSequence, startIndex + 1, boundarySequenceLps);
		return (index >= startIndex? index: -1);
	}

}
