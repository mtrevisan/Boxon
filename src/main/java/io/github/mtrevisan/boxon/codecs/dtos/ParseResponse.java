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
import java.util.List;


public class ParseResponse{

	/** List of all payloads ({@link #parsedMessageIndexes} and {@link #errorIndexes} point here) */
	private final List<byte[]> payloads = new ArrayList<>(0);

	/** List of successfully parsed messages */
	private final List<Object> parsedMessages = new ArrayList<>(0);
	/** List of indexes of successfully parsed messages on the {@link #payloads} variable */
	private final List<Integer> parsedMessageIndexes = new ArrayList<>(0);

	/** List of error messages */
	private final List<ParseException> errors = new ArrayList<>(0);
	/** List of indexes of error messages on the {@link #payloads} variable */
	private final List<Integer> errorIndexes = new ArrayList<>(0);


	public int getPayloadCount(){
		return payloads.size();
	}

	public byte[] getPayloadAt(final int index){
		return payloads.get(index);
	}

	public void addParsedMessage(final byte[] payload, final Object decodedMessage){
		parsedMessageIndexes.add(payloads.size());

		payloads.add(payload);
		parsedMessages.add(decodedMessage);
	}

	public int getParsedMessageCount(){
		return parsedMessages.size();
	}

	public Object getParsedMessageAt(final int index){
		return parsedMessages.get(index);
	}

	public int getParsedMessageIndexAt(final int index){
		return parsedMessageIndexes.get(index);
	}


	public int getErrorCount(){
		return errors.size();
	}

	public ParseException getErrorAt(final int index){
		return errors.get(index);
	}

	public boolean hasErrors(){
		return !errors.isEmpty();
	}

	public void addError(final byte[] payload, final ParseException exception){
		errorIndexes.add(payloads.size());

		payloads.add(payload);
		errors.add(exception);
	}

	public String getMessageForError(final int index){
		return "Error decoding message: " + ByteHelper.toHexString(payloads.get(errorIndexes.get(index)))
			+ System.lineSeparator()
			+ errors.get(index).getMessage();
	}

	public int getErrorIndexAt(final int index){
		return errorIndexes.get(index);
	}

}
