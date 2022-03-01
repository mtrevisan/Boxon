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

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;


/**
 * Response class for the decoding phase.
 */
@SuppressWarnings({"WeakerAccess", "unused"})
public final class MultipleResponse{

	/** Map of index-response pairs. The index refers to the position of the message in the given multi-message input. */
	private final List<SingleResponse<byte[], Object>> responses = new CopyOnWriteArrayList<>();


	/**
	 * Construct a response from a given (possibly multiple-message) payload.
	 *
	 * @return	The instance.
	 */
	static MultipleResponse create(){
		return new MultipleResponse();
	}


	private MultipleResponse(){}


	/**
	 * The number of total parsed (concatenated) messages.
	 *
	 * @return	The number of total parsed (concatenated) messages.
	 */
	public int getTotalMessageCount(){
		return responses.size();
	}


	/**
	 * The decoding response at a given index.
	 *
	 * @param index	The index of a message in a group of concatenated messages for which to extract the parsed message.
	 * @return	The successfully parsed message at a given index.
	 */
	public SingleResponse<byte[], Object> getResponseAt(final int index){
		return responses.get(index);
	}


	void addResponse(final SingleResponse<byte[], Object> response){
		responses.add(response);
	}

}
