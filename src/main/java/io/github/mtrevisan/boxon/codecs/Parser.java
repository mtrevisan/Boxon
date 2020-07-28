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

import io.github.mtrevisan.boxon.codecs.dtos.ComposeResponse;
import io.github.mtrevisan.boxon.codecs.dtos.ParseResponse;
import io.github.mtrevisan.boxon.codecs.exceptions.ComposeException;
import io.github.mtrevisan.boxon.codecs.exceptions.ParseException;
import io.github.mtrevisan.boxon.annotations.MessageHeader;
import io.github.mtrevisan.boxon.annotations.exceptions.ProtocolMessageException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;


public class Parser{

	private final ProtocolMessageParser protocolMessageParser = new ProtocolMessageParser();


	/**
	 * Create an empty parser (context, codecs and protocol messages MUST BE manually loaded!).
	 *
	 * @return	A basic empty parser
	 */
	public static Parser create(){
		return new Parser();
	}


	private Parser(){}

	/**
	 * Loads the context for the {@link Evaluator}.
	 *
	 * @param context	The context map.
	 * @return	The {@link Parser}, used for chaining.
	 */
	public Parser withContext(final Map<String, Object> context){
		if(context != null)
			for(final Map.Entry<String, Object> elem : context.entrySet())
				Evaluator.addToContext(elem.getKey(), elem.getValue());
		return this;
	}


	/**
	 * Loads all the codecs that extends {@link CodecInterface}.
	 * <p>This method should be called from a method inside a class that lies on a parent of all the codecs.</p>
	 *
	 * @return	The {@link Parser}, used for chaining.
	 */
	public final Parser withDefaultCodecs(){
		protocolMessageParser.loader.loadCodecs();
		return this;
	}

	/**
	 * Loads all the codecs that extends {@link CodecInterface}.
	 *
	 * @param basePackageClasses	Classes to be used ase starting point from which to load codecs
	 * @return	The {@link Parser}, used for chaining.
	 */
	public final Parser withCodecs(final Class<?>... basePackageClasses){
		protocolMessageParser.loader.loadCodecs(basePackageClasses);
		return this;
	}

	/**
	 * Loads all the codecs that extends {@link CodecInterface}.
	 *
	 * @param codecs	The list of codecs to be loaded
	 * @return	The {@link Parser}, used for chaining.
	 */
	public final Parser withCodecs(final Collection<CodecInterface<?>> codecs){
		protocolMessageParser.loader.loadCodecs(codecs);
		return this;
	}

	/**
	 * Loads all the codecs that extends {@link CodecInterface}.
	 *
	 * @param codecs	The list of codecs to be loaded
	 * @return	The {@link Parser}, used for chaining.
	 */
	public final Parser withCodecs(final CodecInterface<?>... codecs){
		protocolMessageParser.loader.loadCodecs(codecs);
		return this;
	}

	/**
	 * Load a singe codec that extends {@link CodecInterface}.
	 * <p>If the parser previously contained a codec for the given key, the old codec is replaced by the specified one.</p>
	 *
	 * @param codec	The codec to add
	 * @return	The {@link Parser}, used for chaining.
	 */
	public final Parser addCodec(final CodecInterface<?> codec){
		protocolMessageParser.loader.loadCodecs(codec);
		return this;
	}


	/**
	 * Loads all the protocol classes annotated with {@link MessageHeader}.
	 *
	 * @return	The {@link Parser}, used for chaining.
	 */
	public final Parser withDefaultProtocolMessages(){
		protocolMessageParser.loader.loadProtocolMessages();
		return this;
	}

	/**
	 * Loads all the protocol classes annotated with {@link MessageHeader}.
	 *
	 * @param basePackageClasses	Classes to be used ase starting point from which to load annotated classes
	 * @return	The {@link Parser}, used for chaining.
	 */
	public final Parser withProtocolMessages(final Class<?>... basePackageClasses){
		protocolMessageParser.loader.loadProtocolMessages(basePackageClasses);
		return this;
	}

	/**
	 * Loads all the protocol classes annotated with {@link MessageHeader}.
	 *
	 * @param protocolMessages	The list of protocol messages to be loaded
	 * @return	The {@link Parser}, used for chaining.
	 */
	public final Parser withProtocolMessages(final Collection<ProtocolMessage<?>> protocolMessages){
		protocolMessageParser.loader.loadProtocolMessages(protocolMessages);
		return this;
	}

	/**
	 * Loads all the protocol classes annotated with {@link MessageHeader}.
	 *
	 * @param protocolMessages	The list of protocol messages to be loaded
	 * @return	The {@link Parser}, used for chaining.
	 */
	public final Parser withProtocolMessages(final ProtocolMessage<?>... protocolMessages){
		protocolMessageParser.loader.loadProtocolMessages(Arrays.asList(protocolMessages));
		return this;
	}


	/**
	 * Parse a message from a file containing a binary stream
	 *
	 * @param file	The file containing the binary stream
	 * @return	The parse response
	 * @throws FileNotFoundException	If the file does not exist, is a directory rather than a regular file,
	 * 	or for some other reason cannot be opened for reading.
	 * @throws SecurityException	If a security manager exists and its {@code checkRead} method denies read access to the file.
	 */
	public ParseResponse parse(final File file) throws IOException{
		final BitReader reader = BitReader.wrap(file);
		return parse(reader);
	}

	/**
	 * Parse a message
	 *
	 * @param buffer	The message to be parsed backed by a {@link ByteBuffer}
	 * @return	The parse response
	 */
	public ParseResponse parse(final ByteBuffer buffer){
		final BitReader reader = BitReader.wrap(buffer);
		return parse(reader);
	}

	/**
	 * Parse a message
	 *
	 * @param payload	The message to be parsed
	 * @return	The parse response
	 */
	public ParseResponse parse(final byte[] payload){
		final BitReader reader = BitReader.wrap(payload);
		return parse(reader);
	}

	/**
	 * Parse a message
	 *
	 * @param reader	The message to be parsed backed by a {@link BitReader}
	 * @return	The parse response
	 */
	public ParseResponse parse(final BitReader reader){
		final byte[] array = reader.array();
		final ParseResponse response = new ParseResponse(array);

		int start = 0;
		while(reader.hasRemaining()){
			start = reader.position();
			try{
				//save state of the reader (restored upon a decoding error)
				reader.createFallbackPoint();

				final ProtocolMessage<?> protocolMessage = protocolMessageParser.loader.getProtocolMessage(reader);

				final Object partialDecodedMessage = protocolMessageParser.decode(protocolMessage, reader);

				final int end = reader.position();
				response.addParsedMessage(start, partialDecodedMessage);
			}
			catch(final Throwable t){
				final ParseException pe = new ParseException(reader.position(), t);

				//restore state of the reader
				reader.restoreFallbackPoint();

				final int position = protocolMessageParser.loader.findNextMessageIndex(reader);
				response.addError(start, pe);
				if(position < 0)
					//cannot find any protocol message for message
					break;

				reader.position(position);
			}
		}

		//check if there are unread bytes
		assertNoLeftBytes(reader, start, response);

		return response;
	}

	private void assertNoLeftBytes(final BitReader reader, final int start, final ParseResponse response){
		if(!response.hasErrors() && reader.hasRemaining()){
			final byte[] array = reader.array();
			final int position = reader.position();
			final IllegalArgumentException error = new IllegalArgumentException("There are remaining unread bytes");
			final ParseException pe = new ParseException(position, error);
			response.addError(start, pe);
		}
	}


	/**
	 * Compose a list of messages
	 *
	 * @param data	The messages to be composed
	 * @return	The composition response
	 */
	public ComposeResponse compose(final Collection<Object> data){
		return compose(data.toArray(Object[]::new));
	}

	/**
	 * Compose a message
	 *
	 * @param data	The message(s) to be composed
	 * @return	The composition response
	 */
	public ComposeResponse compose(final Object... data){
		final ComposeResponse response = new ComposeResponse();
		final BitWriter writer = new BitWriter();
		for(final Object elem : data){
			try{
				final ProtocolMessage<?> protocolMessage = ProtocolMessage.createFrom(elem.getClass(), protocolMessageParser.loader);
				if(!protocolMessage.canBeDecoded())
					throw new ProtocolMessageException("Cannot create a protocol message from data");

				protocolMessageParser.encode(protocolMessage, writer, elem);
			}
			catch(final Throwable t){
				final ComposeException ce = new ComposeException(elem, t);
				response.addError(ce);
			}
		}
		writer.flush();

		response.setComposedMessage(writer.array());

		return response;
	}

}
