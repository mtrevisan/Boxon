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

import io.github.mtrevisan.boxon.exceptions.DecodeException;
import io.github.mtrevisan.boxon.helpers.JavaHelper;
import io.github.mtrevisan.boxon.helpers.StringHelper;

import java.util.Map;
import java.util.concurrent.ConcurrentSkipListMap;


/**
 * Response class for the decoding phase.
 */
@SuppressWarnings({"WeakerAccess", "unused"})
public final class ParseResponse{

	/** Whole payload (the index on {@link #parsedMessages} and {@link #errors} point here). */
	private final byte[] payload;

	/** List of successfully parsed messages along with their starting index. */
	private final Map<Integer, Object> parsedMessages = new ConcurrentSkipListMap<>(Integer::compareTo);

	/** List of error messages along with their starting index. */
	private final Map<Integer, DecodeException> errors = new ConcurrentSkipListMap<>(Integer::compareTo);


	/**
	 * Construct a response from a given (possibly multiple-message) payload.
	 *
	 * @param payload	The payload.
	 * @return	The instance.
	 */
	static ParseResponse create(final byte[] payload){
		return new ParseResponse(payload);
	}


	/**
	 * Construct a response from a given (possibly multiple-message) payload.
	 *
	 * @param payload	The payload.
	 */
	private ParseResponse(final byte[] payload){
		this.payload = JavaHelper.cloneOrDefault(payload, null);
	}


	/**
	 * The number of total parsed (concatenated) messages.
	 *
	 * @return	The number of total parsed (concatenated) messages.
	 */
	public int getTotalMessageCount(){
		return parsedMessages.size() + errors.size();
	}

	private byte[] getPayloadAt(final int index, final Iterable<Integer> keys){
		//extract limits of payload starting at a given index on a given map:
		int start = -1;
		int end = payload.length;
		int keysToSkip = index - 1;
		for(final int key : keys){
			if(keysToSkip > 0){
				keysToSkip --;
				continue;
			}

			if(start >= 0){
				end = key;
				break;
			}
			start = key;
		}

		final byte[] copy = new byte[end - start];
		System.arraycopy(payload, start, copy, 0, end - start);
		return copy;
	}


	/**
	 * The number of successfully parsed messages.
	 *
	 * @return	The number of successfully parsed messages.
	 */
	public int getParsedMessageCount(){
		return parsedMessages.size();
	}

	/**
	 * The successfully parsed message at a given index.
	 *
	 * @param index	The index of a message in a group of concatenated messages for which to extract the parsed message.
	 * @return	The successfully parsed message at a given index.
	 * @param <T>	The class of the response.
	 */
	@SuppressWarnings("unchecked")
	public <T> T getParsedMessageAt(final int index){
		return (T)parsedMessages.get(index);
	}

	/**
	 * The payload of a successfully parsed message at a given index.
	 *
	 * @param index	The index of a message in a group of concatenated messages for which to extract the payload.
	 * @return	The payload of a successfully parsed message at a given index.
	 */
	public byte[] getParsedMessagePayloadAt(final int index){
		return getPayloadAt(index, parsedMessages.keySet());
	}

	void addParsedMessage(final int start, final Object decodedMessage){
		parsedMessages.put(start, decodedMessage);
	}


	/**
	 * The number of errors occurred while parsing a message.
	 *
	 * @return	The number of errors occurred while parsing a message.
	 */
	public int getErrorCount(){
		return errors.size();
	}

	/**
	 * Returns whether there are errors once decoded a (possible multiple) message.
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
	public DecodeException getErrorAt(final int index){
		return errors.get(index);
	}

	/**
	 * The error message occurred at a given index.
	 *
	 * @param index	The index of a message in a group of concatenated messages for which to extract the error.
	 * @return	The error message resulting from parsing a message.
	 */
	public String getErrorMessageAt(final int index){
		return "Error decoding message: " + StringHelper.toHexString(getErrorPayloadAt(index))
			+ System.lineSeparator()
			+ errors.get(index).getMessage();
	}

	/**
	 * The payload of an unsuccessfully parsed message at a given index.
	 *
	 * @param index	The index of a message in a group of concatenated messages for which to extract the payload.
	 * @return	The payload of an unsuccessfully parsed message at a given index.
	 */
	public byte[] getErrorPayloadAt(final int index){
		return getPayloadAt(index, errors.keySet());
	}

	void addError(final int start, final DecodeException exception){
		errors.put(start, exception);
	}

}
