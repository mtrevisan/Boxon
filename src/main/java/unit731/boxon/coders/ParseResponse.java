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
package unit731.boxon.coders;

import java.util.ArrayList;
import java.util.List;


public class ParseResponse{

	private final List<Object> parsedMessages = new ArrayList<>();
	private final List<ParseException> errors = new ArrayList<>(0);


	public void addParsedMessage(final Object decodedMessage){
		parsedMessages.add(decodedMessage);
	}

	public List<Object> getParsedMessages(){
		return parsedMessages;
	}

	public void addError(final ParseException exception){
		errors.add(exception);
	}

	public boolean hasErrors(){
		return !errors.isEmpty();
	}

	@SuppressWarnings("unused")
	public List<ParseException> getErrors(){
		return errors;
	}

}
