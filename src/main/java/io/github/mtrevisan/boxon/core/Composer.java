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

import io.github.mtrevisan.boxon.core.managers.templates.Template;
import io.github.mtrevisan.boxon.core.parsers.TemplateParser;
import io.github.mtrevisan.boxon.exceptions.EncodeException;
import io.github.mtrevisan.boxon.exceptions.FieldException;
import io.github.mtrevisan.boxon.io.BitWriter;
import io.github.mtrevisan.boxon.io.BitWriterInterface;

import java.util.ArrayList;
import java.util.List;


/**
 * Handles the composition of a message.
 */
@SuppressWarnings({"WeakerAccess", "unused"})
public final class Composer{

	private final TemplateParser templateParser;


	/**
	 * Create a composer.
	 *
	 * @param boxonCore	The parser core.
	 * @return	A composer.
	 */
	public static Composer create(final BoxonCore boxonCore){
		return new Composer(boxonCore);
	}


	private Composer(final BoxonCore boxonCore){
		templateParser = boxonCore.getTemplateParser();
	}


	/**
	 * Compose a message.
	 *
	 * @param data	The message to be composed.
	 * @param <T>	The class of the composed message.
	 * @return	The composition response.
	 */
	public <T> ComposeResponse<T> composeMessage(final T data){
		final BitWriter writer = BitWriter.create();
		final EncodeException error = composeMessage(writer, data);

		final List<EncodeException> errors = new ArrayList<>(1);
		errors.add(error);

		return ComposeResponse.create(data, writer)
			.withErrors(errors);
	}

	/**
	 * Compose a single message.
	 *
	 * @param data	The message to be composed.
	 * @return	The error, if any.
	 */
	private EncodeException composeMessage(final BitWriterInterface writer, final Object data){
		try{
			final Template<?> template = templateParser.getTemplate(data.getClass());

			templateParser.encode(template, writer, null, data);

			return null;
		}
		catch(final FieldException e){
			return EncodeException.create(e);
		}
	}

}
