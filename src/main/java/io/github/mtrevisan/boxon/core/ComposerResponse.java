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

import io.github.mtrevisan.boxon.exceptions.EncodeException;
import io.github.mtrevisan.boxon.helpers.JavaHelper;
import io.github.mtrevisan.boxon.io.BitWriter;


/**
 * Response class for the encoding phase.
 *
 * @param <T>	The response class.
 */
@SuppressWarnings({"WeakerAccess", "unused"})
public final class ComposerResponse<T>{

	private static final byte[] EMPTY_ARRAY = new byte[0];


	/** The originator for the message. */
	private final T originator;

	/** Successfully composed message. */
	private final byte[] message;

	/** Error message. */
	private EncodeException error;


	/**
	 * Construct a response from a given object and composed message.
	 *
	 * @param originator	The data that originates the message.
	 * @param writer	The writer to read the composed message from.
	 * @param <T>	The response class.
	 * @return	The instance.
	 */
	static <T> ComposerResponse<T> create(final T originator, final BitWriter writer){
		writer.flush();

		return create(originator, writer.array());
	}

	/**
	 * Construct a response from a given object and composed message.
	 *
	 * @param originator	The data that originates the message.
	 * @param composedMessage	The composed message.
	 * @return	The instance.
	 */
	static <T> ComposerResponse<T> create(final T originator, final byte[] composedMessage){
		return new ComposerResponse<>(originator, composedMessage);
	}


	/**
	 * Construct a response from a given object and composed message.
	 *
	 * @param originator	The data that originates the message.
	 * @param message	The composed message.
	 */
	private ComposerResponse(final T originator, final byte[] message){
		this.originator = originator;
		this.message = JavaHelper.cloneOrDefault(message, null);
	}


	/**
	 * The originator for the composed message.
	 *
	 * @return	The originator for the composed message.
	 */
	private T getOriginator(){
		return originator;
	}

	/**
	 * The message composed by the given {@link #originator}.
	 *
	 * @return	The message composed by the given originator.
	 */
	public byte[] getMessage(){
		return JavaHelper.cloneOrDefault(message, EMPTY_ARRAY);
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
	public EncodeException getError(){
		return error;
	}

	void addError(final EncodeException exception){
		error = exception;
	}

	ComposerResponse<T> withError(final EncodeException exception){
		addError(exception);

		return this;
	}

}
