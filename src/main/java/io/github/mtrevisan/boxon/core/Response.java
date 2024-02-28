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

import io.github.mtrevisan.boxon.io.BitReader;
import io.github.mtrevisan.boxon.io.BitWriter;


/**
 * Response class for a single encoding/decoding phase.
 *
 * @param <S>	The source class.
 * @param <M>	The message class.
 */
@SuppressWarnings({"WeakerAccess", "unused"})
public final class Response<S, M>{

	/** The source data for the message. */
	private final S source;

	/** Successfully processed message. */
	private final M message;

	/** Error message. */
	private final Exception error;


	/**
	 * Construct a response from a given source and processed message (as a byte array) or error.
	 *
	 * @param source	The source data that originates the message.
	 * @param writer	The writer to read the processed message from.
	 * @param error	The error.
	 * @param <S>	The source class.
	 * @return	The instance.
	 */
	static <S> Response<S, byte[]> create(final S source, final BitWriter writer, final Exception error){
		writer.flush();
		return new Response<>(source, writer.array(), error);
	}

	/**
	 * Construct a response from a given source and processed message.
	 *
	 * @param reader	The reader to read the source data from.
	 * @param message	The processed message.
	 * @param <M>	The message class.
	 * @return	The instance.
	 */
	static <M> Response<byte[], M> create(final BitReader reader, final M message){
		return new Response<>(reader.array(), message, null);
	}

	/**
	 * Construct a response from a given object and error.
	 *
	 * @param reader	The reader to read the source data from.
	 * @param error	The error.
	 * @param <M>	The message class.
	 * @return	The instance.
	 */
	static <M> Response<byte[], M> create(final BitReader reader, final Exception error){
		return new Response<>(reader.array(), null, error);
	}

	/**
	 * Construct a response from an error.
	 *
	 * @param error	The error.
	 * @param <S>	The source class.
	 * @param <M>	The message class.
	 * @return	The instance.
	 */
	static <S, M> Response<S, M> create(final Exception error){
		return new Response<>(null, null, error);
	}


	/**
	 * Construct a response from a given object and processed message.
	 *
	 * @param source	The source data that originates the message.
	 * @param message	The processed message.
	 * @param error	The error.
	 */
	private Response(final S source, final M message, final Exception error){
		this.source = source;
		this.message = message;
		this.error = error;
	}


	/**
	 * The source for the processed message.
	 *
	 * @return	The source for the processed message.
	 */
	public S getSource(){
		return source;
	}

	/**
	 * The message processed from the given {@link #source}.
	 *
	 * @return	The message processed from the given source.
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
