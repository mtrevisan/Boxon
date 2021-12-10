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
package io.github.mtrevisan.boxon.core;

import io.github.mtrevisan.boxon.codecs.TemplateParser;
import io.github.mtrevisan.boxon.codecs.managers.Template;
import io.github.mtrevisan.boxon.exceptions.DecodeException;
import io.github.mtrevisan.boxon.exceptions.EncodeException;
import io.github.mtrevisan.boxon.external.codecs.BitReader;
import io.github.mtrevisan.boxon.external.codecs.BitWriter;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Collection;


/**
 * Declarative data binding parser for binary encoded data with configuration capabilities.
 */
@SuppressWarnings({"WeakerAccess", "unused"})
public final class Parser{

	private final ParserCore core;


	/**
	 * Create an empty parser (context, codecs and templates MUST BE manually loaded! -- templates MUST BE loaded AFTER
	 * the codecs).
	 *
	 * @param core	The core of the parser.
	 * @return	A basic empty parser.
	 */
	public static Parser create(final ParserCore core){
		return new Parser(core);
	}


	private Parser(final ParserCore core){
		this.core = core;
	}


	/**
	 * Parse a message from a file containing a binary stream.
	 *
	 * @param file	The file containing the binary stream.
	 * @return	The parse response.
	 * @throws IOException	If an I/O error occurs.
	 * @throws FileNotFoundException	If the file does not exist, is a directory rather than a regular file,
	 * 	or for some other reason cannot be opened for reading.
	 * @throws SecurityException	If a security manager exists and its {@code checkRead} method denies read access to the file.
	 */
	public ParseResponse parse(final File file) throws IOException, FileNotFoundException{
		final BitReader reader = BitReader.wrap(file);
		return parse(reader);
	}

	/**
	 * Parse a message.
	 *
	 * @param payload	The message to be parsed.
	 * @return	The parse response.
	 */
	public ParseResponse parse(final byte[] payload){
		final BitReader reader = BitReader.wrap(payload);
		return parse(reader);
	}

	/**
	 * Parse a message.
	 *
	 * @param buffer	The message to be parsed backed by a {@link ByteBuffer}.
	 * @return	The parse response.
	 */
	public ParseResponse parse(final ByteBuffer buffer){
		final BitReader reader = BitReader.wrap(buffer);
		return parse(reader);
	}

	/**
	 * Parse a message.
	 *
	 * @param reader	The message to be parsed backed by a {@link BitReader}.
	 * @return	The parse response.
	 */
	public ParseResponse parse(final BitReader reader){
		final byte[] array = reader.array();
		final ParseResponse response = new ParseResponse(array);

		int start = 0;
		while(reader.hasRemaining()){
			start = reader.position();

			//save state of the reader (restored upon a decoding error)
			reader.createFallbackPoint();

			if(parse(reader, start, response))
				break;
		}

		//check if there are unread bytes
		assertNoLeftBytes(reader, start, response);

		return response;
	}

	private boolean parse(final BitReader reader, final int start, final ParseResponse response){
		final TemplateParser templateParser = core.getTemplateParser();
		try{
			final Template<?> template = templateParser.getTemplate(reader);

			final Object partialDecodedMessage = templateParser.decode(template, reader, null);

			response.addParsedMessage(start, partialDecodedMessage);
		}
		catch(final Exception e){
			final DecodeException de = DecodeException.create(reader.position(), e);
			response.addError(start, de);

			//restore state of the reader
			reader.restoreFallbackPoint();

			final int position = templateParser.findNextMessageIndex(reader);
			if(position < 0)
				//cannot find any template for message
				return true;

			reader.position(position);
		}
		return false;
	}

	private static void assertNoLeftBytes(final BitReader reader, final int start, final ParseResponse response){
		if(!response.hasErrors() && reader.hasRemaining()){
			final int position = reader.position();
			final IllegalArgumentException error = new IllegalArgumentException("There are remaining unread bytes");
			final DecodeException pe = DecodeException.create(position, error);
			response.addError(start, pe);
		}
	}


	//-- Composer section --

	/**
	 * Compose a list of messages.
	 *
	 * @param data	The messages to be composed.
	 * @return	The composition response.
	 */
	public ComposeResponse composeMessage(final Collection<Object> data){
		return composeMessage(data.toArray(Object[]::new));
	}

	/**
	 * Compose a list of messages.
	 *
	 * @param data	The message(s) to be composed.
	 * @return	The composition response.
	 */
	public ComposeResponse composeMessage(final Object... data){
		final ComposeResponse response = new ComposeResponse(data);

		final BitWriter writer = BitWriter.create();
		for(int i = 0; i < data.length; i ++)
			composeMessage(writer, data[i], response);

		response.setComposedMessage(writer);

		return response;
	}

	/**
	 * Compose a single message.
	 *
	 * @param data	The message to be composed.
	 */
	private void composeMessage(final BitWriter writer, final Object data, final ComposeResponse response){
		try{
			final TemplateParser templateParser = core.getTemplateParser();
			final Template<?> template = templateParser.getTemplate(data.getClass());

			templateParser.encode(template, writer, null, data);
		}
		catch(final Exception e){
			response.addError(EncodeException.create(e));
		}
	}

}
