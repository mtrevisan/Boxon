/*
 * Copyright (c) 2020-2022 Mauro Trevisan
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
import io.github.mtrevisan.boxon.core.codecs.BitReader;
import io.github.mtrevisan.boxon.exceptions.DecodeException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;


/**
 * Declarative data binding parser for binary encoded data.
 */
@SuppressWarnings({"WeakerAccess", "unused"})
public final class Parser{

	private final TemplateParser templateParser;


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
		templateParser = core.getTemplateParser();
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

}
