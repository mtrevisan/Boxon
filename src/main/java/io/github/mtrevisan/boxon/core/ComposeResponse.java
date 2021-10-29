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

import io.github.mtrevisan.boxon.exceptions.EncodeException;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


/**
 * Response class for the encoding phase.
 */
public final class ComposeResponse{

	/** The originators for the message. */
	private final Object[] originator;

	/** Successfully composed message. */
	private byte[] composedMessage;

	/** List of error messages. */
	private final List<EncodeException> errors = new ArrayList<>();


	/**
	 * Construct a response from a given object.
	 *
	 * @param originator	The data that originates the message.
	 */
	ComposeResponse(final Object[] originator){
		this.originator = originator;
	}

	/**
	 * The originators for the composed message.
	 *
	 * @return	The originators for the composed message.
	 */
	private Object[] getOriginator(){
		return originator;
	}

	void setComposedMessage(final byte[] composedMessage){
		this.composedMessage = composedMessage;
	}

	/**
	 * The message composed by the given {@link #originator}.
	 *
	 * @return	The message composed by the given originator.
	 */
	public byte[] getComposedMessage(){
		return composedMessage;
	}

	/**
	 * The number of errors occurred while composing a message.
	 *
	 * @return	The number of errors occurred while composing a message.
	 */
	public int getErrorCount(){
		return errors.size();
	}

	/**
	 * Returns whether there are errors once encoded a message.
	 *
	 * @return	Whether there are errors.
	 */
	public boolean hasErrors(){
		return !errors.isEmpty();
	}

	/**
	 * The error occurred at a given index.
	 *
	 * @param index	The index of a message in a group of concatenated messages for which to extract the error.
	 * @return	The exception resulting from parsing a message.
	 */
	public EncodeException getErrorAt(final int index){
		return errors.get(index);
	}

	void addError(final EncodeException exception){
		Objects.requireNonNull(exception, "Exception cannot be null");

		errors.add(exception);
	}

}
