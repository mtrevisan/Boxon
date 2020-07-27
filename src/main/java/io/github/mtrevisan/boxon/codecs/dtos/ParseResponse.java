/**
 * Copyright (c) 2020 Mauro Trevisan
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
package io.github.mtrevisan.boxon.codecs.dtos;

import io.github.mtrevisan.boxon.codecs.exceptions.ParseException;
import io.github.mtrevisan.boxon.helpers.ByteHelper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;


public class ParseResponse{

	/** Whole payload (the index on {@link #parsedMessages} and {@link #errors} point here) */
	private final byte[] payload;

	/** List of successfully parsed messages along with their starting index */
	private final Map<Integer, Object> parsedMessages = new HashMap<>(0);

	/** List of error messages along with their starting index */
	private final Map<Integer, ParseException> errors = new HashMap<>(0);


	public ParseResponse(final byte[] payload){
		this.payload = payload;
	}

	public int getTotalMessageCount(){
		return parsedMessages.size() + errors.size();
	}

	private byte[] getPayloadAt(final int index, final Set<Integer> keySet){
		final List<Integer> keys = new ArrayList<>(keySet);
		Collections.sort(keys);
		final int start = keys.get(index);
		final int end = (index + 1 < keys.size()? keys.get(index + 1): payload.length);
		final byte[] copy = new byte[end - start];
		System.arraycopy(payload, start, copy, 0, end - start);
		return copy;
	}


	public int getParsedMessageCount(){
		return parsedMessages.size();
	}

	public Object getParsedMessageAt(final int index){
		return parsedMessages.get(index);
	}

	public byte[] getParsedMessagePayloadAt(final int index){
		return getPayloadAt(index, parsedMessages.keySet());
	}

	public void addParsedMessage(final int start, final Object decodedMessage){
		parsedMessages.put(start, decodedMessage);
	}


	public int getErrorCount(){
		return errors.size();
	}

	public ParseException getErrorAt(final int index){
		return errors.get(index);
	}

	public String getMessageForError(final int index){
		return "Error decoding message: " + ByteHelper.toHexString(getErrorPayloadAt(index))
			+ System.lineSeparator()
			+ errors.get(index).getMessage();
	}

	public byte[] getErrorPayloadAt(final int index){
		return getPayloadAt(index, errors.keySet());
	}

	public boolean hasErrors(){
		return !errors.isEmpty();
	}

	public void addError(final int start, final ParseException exception){
		errors.put(start, exception);
	}

}
