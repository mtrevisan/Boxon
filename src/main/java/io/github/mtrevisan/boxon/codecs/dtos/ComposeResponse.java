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

import io.github.mtrevisan.boxon.codecs.exceptions.ComposeException;

import java.util.ArrayList;
import java.util.List;


public class ComposeResponse{

	private byte[] composedMessage;
	private final List<ComposeException> errors = new ArrayList<>(0);


	public void setComposedMessage(final byte[] composedMessages){
		this.composedMessage = composedMessages;
	}

	public byte[] getComposedMessage(){
		return composedMessage;
	}

	public void addError(final ComposeException exception){
		errors.add(exception);
	}

	public boolean hasErrors(){
		return !errors.isEmpty();
	}

	@SuppressWarnings("unused")
	public List<ComposeException> getErrors(){
		return errors;
	}

}
