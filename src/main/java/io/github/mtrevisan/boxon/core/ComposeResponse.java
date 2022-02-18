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
import io.github.mtrevisan.boxon.io.BitWriter;
import io.github.mtrevisan.boxon.helpers.JavaHelper;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;


/**
 * Response class for the encoding phase.
 */
@SuppressWarnings({"WeakerAccess", "unused"})
public final class ComposeResponse{

	private static final byte[] EMPTY_ARRAY = new byte[0];


	/** The originators for the message. */
	private final Object[] originator;

	/** Successfully composed message. */
	private final byte[] composedMessage;

	/** List of error messages. */
	private final List<EncodeException> errors = new CopyOnWriteArrayList<>();


	/**
	 * Construct a response from a given object and composed message.
	 *
	 * @param originator	The data that originates the message.
	 * @param writer	The writer to read the composed message from.
	 * @return	The instance.
	 */
	public static ComposeResponse create(final Object[] originator, final BitWriter writer){
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
	public static ComposeResponse create(final Object[] originator, final byte[] composedMessage){
		return new ComposeResponse(originator, composedMessage);
	}


	/**
	 * Construct a response from a given object.
	 *
	 * @param originator	The data that originates the message.
	 */
	private ComposeResponse(final Object[] originator, final byte[] composedMessage){
		this.originator = JavaHelper.cloneOrDefault(originator, null);
		this.composedMessage = JavaHelper.cloneOrDefault(composedMessage, null);
	}


	/**
	 * The originators for the composed message.
	 *
	 * @return	The originators for the composed message.
	 */
	private Object[] getOriginator(){
		return originator;
	}

	/**
	 * The message composed by the given {@link #originator}.
	 *
	 * @return	The message composed by the given originator.
	 */
	public byte[] getComposedMessage(){
		return JavaHelper.cloneOrDefault(composedMessage, EMPTY_ARRAY);
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

	ComposeResponse withErrors(final List<EncodeException> exceptions){
		Objects.requireNonNull(exceptions, "Exception cannot be null");

		for(int i = 0; i < exceptions.size(); i ++){
			final EncodeException exception = exceptions.get(i);
			if(exception != null)
				errors.add(exception);
		}

		return this;
	}

}
