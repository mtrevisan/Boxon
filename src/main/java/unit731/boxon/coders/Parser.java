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
import unit731.boxon.annotations.exceptions.ProtocolMessageException;
import unit731.boxon.coders.exceptions.ComposeException;
import unit731.boxon.coders.dtos.ComposeResponse;
import unit731.boxon.coders.exceptions.ParseException;
import unit731.boxon.coders.dtos.ParseResponse;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;


@SuppressWarnings("unused")
public class Parser{

	private final MessageParser messageParser = new MessageParser();


	/** Create an empty parser (context, coders and protocol messages MUST BE manually loaded!). */
	public static Parser create(){
		return new Parser();
	}


	private Parser(){}

	public Parser withContext(final Map<String, Object> context){
		if(context != null)
			for(final Map.Entry<String, Object> elem : context.entrySet())
				Evaluator.addToContext(elem.getKey(), elem.getValue());
		return this;
	}


	/**
	 * Loads all the coders that extends {@link CoderInterface}.
	 * <p>This method should be called from a method inside a class that lies on a parent of all the coders.</p>
	 */
	public final Parser withDefaultCoders(){
		messageParser.loader.loadCoders();
		return this;
	}

	/**
	 * Loads all the coders that extends {@link CoderInterface}.
	 *
	 * @param basePackageClasses	Classes to be used ase starting point from which to load coders
	 */
	public final Parser withCoders(final Class<?>... basePackageClasses){
		messageParser.loader.loadCoders(basePackageClasses);
		return this;
	}

	/**
	 * Loads all the coders that extends {@link CoderInterface}.
	 *
	 * @param coders	The list of coders to be loaded
	 */
	public final Parser withCoders(final Collection<CoderInterface<?>> coders){
		messageParser.loader.loadCoders(coders);
		return this;
	}

	/**
	 * Loads all the coders that extends {@link CoderInterface}.
	 *
	 * @param coders	The list of coders to be loaded
	 */
	public final Parser withCoders(final CoderInterface<?>... coders){
		messageParser.loader.loadCoders(coders);
		return this;
	}

	/**
	 * Load a singe coder that extends {@link CoderInterface}.
	 * <p>If the parser previously contained a coder for the given key, the old coder is replaced by the specified one.</p>
	 *
	 * @param coder	The coder to add
	 */
	public final void addCoder(final CoderInterface<?> coder){
		messageParser.loader.loadCoders(coder);
	}


	/**
	 * Loads all the protocol classes annotated with {@link MessageHeader}.
	 */
	public final Parser withDefaultProtocolMessages(){
		messageParser.loader.loadProtocolMessages();
		return this;
	}

	/**
	 * Loads all the protocol classes annotated with {@link MessageHeader}.
	 *
	 * @param basePackageClasses	Classes to be used ase starting point from which to load annotated classes
	 */
	public final Parser withProtocolMessages(final Class<?>... basePackageClasses){
		messageParser.loader.loadProtocolMessages(basePackageClasses);
		return this;
	}

	/**
	 * Loads all the protocol classes annotated with {@link MessageHeader}.
	 *
	 * @param protocolMessages	The list of protocol messages to be loaded
	 */
	public final Parser withProtocolMessages(final Collection<ProtocolMessage<?>> protocolMessages){
		messageParser.loader.loadProtocolMessages(protocolMessages);
		return this;
	}

	/**
	 * Loads all the protocol classes annotated with {@link MessageHeader}.
	 *
	 * @param protocolMessages	The list of protocol messages to be loaded
	 */
	public final Parser withProtocolMessages(final ProtocolMessage<?>... protocolMessages){
		messageParser.loader.loadProtocolMessages(Arrays.asList(protocolMessages));
		return this;
	}


	/**
	 * Prints every read value.
	 *
	 * @param verbose	Whether to print every read value
	 */
	public final void setVerbose(final boolean verbose){
		messageParser.verbose.set(verbose);
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
		final BitBuffer reader = BitBuffer.wrap(file);
		return parse(reader);
	}

	/**
	 * Parse a message
	 *
	 * @param buffer	The message to be parsed backed by a {@link ByteBuffer}
	 * @return	The parse response
	 */
	public ParseResponse parse(final ByteBuffer buffer){
		final BitBuffer reader = BitBuffer.wrap(buffer);
		return parse(reader);
	}

	/**
	 * Parse a message
	 *
	 * @param payload	The message to be parsed
	 * @return	The parse response
	 */
	public ParseResponse parse(final byte[] payload){
		final BitBuffer reader = BitBuffer.wrap(payload);
		return parse(reader);
	}

	/**
	 * Parse a message
	 *
	 * @param reader	The message to be parsed backed by a {@link BitBuffer}
	 * @return	The parse response
	 */
	public ParseResponse parse(final BitBuffer reader){
		final ParseResponse response = new ParseResponse();

		while(reader.hasRemaining()){
			try{
				//save state of the reader (restored upon a decoding error)
				reader.createFallbackPoint();

				final ProtocolMessage<?> protocolMessage = messageParser.loader.getProtocolMessage(reader);

				final Object partialDecodedMessage = messageParser.decode(protocolMessage, reader);

				response.addParsedMessage(partialDecodedMessage);
			}
			catch(final Throwable t){
				final ParseException pe = createParseException(reader, t);
				response.addError(pe);

				//restore state of the reader
				reader.restoreFallbackPoint();

				final int position = messageParser.loader.findNextMessageIndex(reader);
				if(position < 0)
					//cannot find any protocol message for message
					break;

				reader.position(position);
			}
		}

		//check if there are unread bytes:
		if(!response.hasErrors() && reader.hasRemaining()){
			final IllegalArgumentException error = new IllegalArgumentException("There are remaining bytes");
			final ParseException pe = new ParseException(reader.array(), reader.position(), error);
			response.addError(pe);
		}

		return response;
	}


	/**
	 * Compose a list of messages
	 *
	 * @param data	The messages to be composed
	 * @return	The composition response
	 */
	public ComposeResponse compose(final List<Object> data){
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
				final ProtocolMessage<?> protocolMessage = ProtocolMessage.createFrom(elem.getClass(), messageParser.loader);
				if(!protocolMessage.canBeDecoded())
					throw new ProtocolMessageException("Cannot construct any protocol message for message");

				messageParser.encode(protocolMessage, elem, writer);
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


	private ParseException createParseException(final BitBuffer reader, final Throwable t){
		final byte[] payload = reader.array();
		final int position = reader.position();
		final byte[] subPayload = Arrays.copyOfRange(payload, position, payload.length);
		return new ParseException(subPayload, position, t);
	}

}
