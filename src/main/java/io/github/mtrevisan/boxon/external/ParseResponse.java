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
package io.github.mtrevisan.boxon.external;

import io.github.mtrevisan.boxon.exceptions.DecodeException;
import io.github.mtrevisan.boxon.internal.JavaHelper;

import java.util.Map;
import java.util.Set;
import java.util.TreeMap;


public class ParseResponse{

	/** Whole payload (the index on {@link #parsedMessages} and {@link #errors} point here). */
	private final byte[] payload;

	/** List of successfully parsed messages along with their starting index. */
	private final Map<Integer, Object> parsedMessages = new TreeMap<>(Integer::compareTo);

	/** List of error messages along with their starting index. */
	private final Map<Integer, DecodeException> errors = new TreeMap<>(Integer::compareTo);


	public ParseResponse(final byte[] payload){
		this.payload = payload;
	}

	public int getTotalMessageCount(){
		return parsedMessages.size() + errors.size();
	}

	private byte[] getPayloadAt(final int index, final Set<Integer> keys){
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

	public boolean hasErrors(){
		return !errors.isEmpty();
	}

	public DecodeException getErrorAt(final int index){
		return errors.get(index);
	}

	public String getMessageForError(final int index){
		return "Error decoding message: " + JavaHelper.toHexString(getErrorPayloadAt(index))
			+ System.lineSeparator()
			+ errors.get(index).getMessage();
	}

	public byte[] getErrorPayloadAt(final int index){
		return getPayloadAt(index, errors.keySet());
	}

	public void addError(final int start, final DecodeException exception){
		errors.put(start, exception);
	}

}
