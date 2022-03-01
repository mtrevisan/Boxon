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

import io.github.mtrevisan.boxon.io.BitWriter;


/**
 * Response class for a single encoding/decoding phase.
 *
 * @param <O>	The originator class.
 * @param <M>	The message class.
 */
@SuppressWarnings({"WeakerAccess", "unused"})
public final class Response<O, M>{

	/** The originator for the message. */
	private final O originator;

	/** Successfully composed message. */
	private final M message;

	/** Error message. */
	private final Exception error;


	/**
	 * Construct a response from a given object and composed message or error.
	 *
	 * @param originator	The data that originates the message.
	 * @param writer	The writer to read the composed message from.
	 * @param error	The error.
	 * @param <O>	The originator class.
	 * @return	The instance.
	 */
	static <O> Response<O, byte[]> create(final O originator, final BitWriter writer, final Exception error){
		writer.flush();
		return new Response<>(originator, writer.array(), error);
	}

	/**
	 * Construct a response from a given object and composed message.
	 *
	 * @param originator	The data that originates the message.
	 * @param composedMessage	The composed message.
	 * @param <O>	The originator class.
	 * @param <M>	The message class.
	 * @return	The instance.
	 */
	static <O, M> Response<O, M> create(final O originator, final M composedMessage){
		return new Response<>(originator, composedMessage, null);
	}

	/**
	 * Construct a response from a given object and error.
	 *
	 * @param originator	The data that originates the message.
	 * @param error	The error.
	 * @param <O>	The originator class.
	 * @param <M>	The message class.
	 * @return	The instance.
	 */
	static <O, M> Response<O, M> create(final O originator, final Exception error){
		return new Response<>(originator, null, error);
	}

	/**
	 * Construct a response from an error.
	 *
	 * @param error	The error.
	 * @param <O>	The originator class.
	 * @param <M>	The message class.
	 * @return	The instance.
	 */
	static <O, M> Response<O, M> create(final Exception error){
		return new Response<>(null, null, error);
	}


	/**
	 * Construct a response from a given object and composed message.
	 *
	 * @param originator	The data that originates the message.
	 * @param message	The composed message.
	 * @param error	The error.
	 */
	private Response(final O originator, final M message, final Exception error){
		this.originator = originator;
		this.message = message;
		this.error = error;
	}


	/**
	 * The originator for the composed message.
	 *
	 * @return	The originator for the composed message.
	 */
	public O getOriginator(){
		return originator;
	}

	/**
	 * The message composed by the given {@link #originator}.
	 *
	 * @return	The message composed by the given originator.
	 */
	public M getMessage(){
		return message;
	}

	/**
	 * Returns whether there is an error once encoded a message.
	 *
	 * @return	Whether there is an error.
	 */
	public boolean hasError(){
		return (error != null);
	}

	/**
	 * The error occurred.
	 *
	 * @return	The exception resulting from parsing a message.
	 */
	public Exception getError(){
		return error;
	}

}
