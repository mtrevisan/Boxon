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
import java.util.Arrays;
import java.util.List;


public class ParseResponse{

	/** Whole payload ({@link #parsedMessageStartIndexes} and {@link #errorStartIndexes} point here) */
	private final byte[] payload;

	/** List of successfully parsed messages */
	private final List<Object> parsedMessages = new ArrayList<>(0);
	/** List of starting index for each message */
	private final List<Integer> parsedMessageStartIndexes = new ArrayList<>(0);

	/** List of error messages */
	private final List<ParseException> errors = new ArrayList<>(0);
	private final List<Integer> errorStartIndexes = new ArrayList<>(0);


	public ParseResponse(final byte[] payload){
		this.payload = payload;
	}

	public int getTotalMessageCount(){
		return parsedMessages.size() + errors.size();
	}

	private byte[] getPayloadBetween(final int start, final int end){
		return Arrays.copyOfRange(payload, start, end);
	}


	public int getParsedMessageCount(){
		return parsedMessages.size();
	}

	public Object getParsedMessageAt(final int index){
		return parsedMessages.get(index);
	}

	public byte[] getParsedMessagePayloadAt(final int index){
		final int start = parsedMessageStartIndexes.get(index);
		final int end = index + 1 < parsedMessageStartIndexes.size()? parsedMessageStartIndexes.get(index + 1): payload.length;
		return getPayloadBetween(start, end);
	}

	public void addParsedMessage(final int start, final Object decodedMessage){
		parsedMessageStartIndexes.add(start);

		parsedMessages.add(decodedMessage);
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
		final int start = errorStartIndexes.get(index);
		final int end = index + 1 < errorStartIndexes.size()? errorStartIndexes.get(index + 1): payload.length;
		return getPayloadBetween(start, end);
	}

	public boolean hasErrors(){
		return !errors.isEmpty();
	}

	public void addError(final int start, final ParseException exception){
		errorStartIndexes.add(start);

		errors.add(exception);
	}

}
