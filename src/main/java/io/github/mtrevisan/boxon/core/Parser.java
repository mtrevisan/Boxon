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
package io.github.mtrevisan.boxon.core;

import io.github.mtrevisan.boxon.core.helpers.BitReader;
import io.github.mtrevisan.boxon.core.helpers.ConstructorHelper;
import io.github.mtrevisan.boxon.core.helpers.templates.Template;
import io.github.mtrevisan.boxon.core.parsers.TemplateLoader;
import io.github.mtrevisan.boxon.core.parsers.TemplateParser;
import io.github.mtrevisan.boxon.exceptions.DataException;
import io.github.mtrevisan.boxon.exceptions.DecodeException;
import io.github.mtrevisan.boxon.helpers.GenericHelper;
import io.github.mtrevisan.boxon.io.Evaluator;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;


/**
 * Declarative data binding parser for binary encoded data.
 */
public final class Parser{

	private final TemplateParser templateParser;


	/**
	 * Create a parser.
	 *
	 * @param core	The core of the parser.
	 * @return	A parser.
	 */
	public static Parser create(final Core core){
		return new Parser(core);
	}


	private Parser(final Core core){
		templateParser = core.getTemplateParser();
	}


	/**
	 * Loads the maximum memoizer size for the constructor.
	 *
	 * @param maxEmptyConstructorMemoizerSize	The maximum number of elements for the empty constructor memoizer.
	 * @param maxNonEmptyConstructorMemoizerSize	The maximum number of elements for the non-empty constructor memoizer.
	 * @return	This instance, used for chaining.
	 */
	public Parser withMaxConstructorMemoizerSize(final int maxEmptyConstructorMemoizerSize, final int maxNonEmptyConstructorMemoizerSize){
		ConstructorHelper.initialize(maxEmptyConstructorMemoizerSize, maxNonEmptyConstructorMemoizerSize);

		return this;
	}

	/**
	 * Loads the maximum memoizer size for the templates.
	 *
	 * @param maxSize	The maximum number of elements for the memoizer.
	 * @return	This instance, used for chaining.
	 */
	public Parser withMaxTemplateMemoizerSize(final int maxSize){
		TemplateLoader.initialize(maxSize);

		return this;
	}

	/**
	 * Loads the maximum memoizer size for the generics offsprings.
	 *
	 * @param maxSize	The maximum number of elements for the memoizer.
	 * @return	This instance, used for chaining.
	 */
	public Parser withMaxGenericsOffspringsMemoizerSize(final int maxSize){
		GenericHelper.initialize(maxSize);

		return this;
	}

	/**
	 * Loads the maximum memoizer size for the <a href="https://docs.spring.io/spring-framework/reference/core/expressions.html">SpEL</a>
	 * expressions.
	 *
	 * @param maxSize	The maximum number of elements for the memoizer.
	 * @return	This instance, used for chaining.
	 */
	public Parser withMaxSpELMemoizerSize(final int maxSize){
		Evaluator.initialize(maxSize);

		return this;
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
	public List<Response<byte[], Object>> parse(final File file) throws IOException{
		final BitReader reader = BitReader.wrap(file);
		return parse(reader);
	}

	/**
	 * Parse a message.
	 *
	 * @param payload	The message to be parsed.
	 * @return	The parse response.
	 */
	public List<Response<byte[], Object>> parse(final byte[] payload){
		final BitReader reader = BitReader.wrap(payload);
		return parse(reader);
	}

	/**
	 * Parse a message.
	 *
	 * @param buffer	The message to be parsed backed by a {@link ByteBuffer}.
	 * @return	The parse response.
	 */
	public List<Response<byte[], Object>> parse(final ByteBuffer buffer){
		final BitReader reader = BitReader.wrap(buffer);
		return parse(reader);
	}

	/**
	 * Parse a message.
	 *
	 * @param reader	The message to be parsed backed by a {@link BitReader}.
	 * @return	The operation result.
	 */
	private List<Response<byte[], Object>> parse(final BitReader reader){
		final List<Response<byte[], Object>> response = new ArrayList<>(1);

		while(reader.hasRemaining()){
			//save the state of the reader (restored upon a decoding error)
			reader.createSavepoint();

			if(parse(reader, response))
				break;
		}

		//check if there are unread bytes
		assertNoLeftBytes(reader, response);

		return response;
	}

	private boolean parse(final BitReader reader, final Collection<Response<byte[], Object>> response){
		try{
			final Template<?> template = templateParser.getTemplate(reader);

			final Object partialDecodedMessage = templateParser.decode(template, reader, null);

			final Response<byte[], Object> partialResponse = Response.create(reader, partialDecodedMessage);
			response.add(partialResponse);
		}
		catch(final Exception e){
			final DecodeException de = DecodeException.create(reader.position(), e);
			final Response<byte[], Object> partialResponse = Response.create(reader, de);
			response.add(partialResponse);

			//restore the state of the reader
			reader.restoreSavepoint();

			final int position = templateParser.findNextMessageIndex(reader);
			if(position < 0)
				//cannot find any template for the message
				return true;

			reader.position(position);
		}
		return false;
	}

	private static void assertNoLeftBytes(final BitReader reader, final Collection<Response<byte[], Object>> response){
		if(reader.hasRemaining()){
			final int position = reader.position();
			final Exception error = DataException.create("There are remaining unread bytes");
			final DecodeException pe = DecodeException.create(position, error);
			response.add(Response.create(pe));
		}
	}

}
