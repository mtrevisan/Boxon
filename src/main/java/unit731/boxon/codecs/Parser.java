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
package unit731.boxon.codecs;

import java.util.Arrays;
import java.util.Map;
import java.util.Objects;


public class Parser{

	private final Loader loader = new Loader();


	public Parser(){
		this(null);
	}

	public Parser(final Map<String, Object> context){
		loader.init();

		copyContext(context);
	}

	public Parser(final Map<String, Object> context, final Map<String, Codec<?>> codecs){
		Objects.requireNonNull(codecs, "Codecs cannot be null");
		if(codecs.isEmpty())
			throw new IllegalArgumentException("Codecs cannot be empty");

		loader.init(codecs);

		copyContext(context);
	}

	public Parser(final Map<String, Object> context, final Class<?>... basePackageClasses){
		Objects.requireNonNull(basePackageClasses, "Base package(s) not found");

		loader.init(basePackageClasses);

		copyContext(context);
	}

	private void copyContext(final Map<String, Object> context){
		if(context != null)
			for(final Map.Entry<String, Object> elem : context.entrySet())
				Evaluator.addToContext(elem.getKey(), elem.getValue());
	}

	public static void setVerbose(final boolean verbose) throws SecurityException{
		MessageParser.setVerbose(verbose);
	}

	/**
	 * Parse a message
	 *
	 * @param payload	The message to be parsed
	 * @return	The parse response
	 */
	public final ParseResponse parse(final byte[] payload){
		final ParseResponse response = new ParseResponse();

		final BitBuffer reader = BitBuffer.wrap(payload);
		while(reader.hasRemaining()){
			try{
				//save state of the reader (restored upon a decoding error)
				reader.createFallbackPoint();

				final Codec<?> codec = loader.getCodec(reader);

				final Object partialDecodedMessage = MessageParser.decode(codec, reader);

				response.addParsedMessage(partialDecodedMessage);
			}
			catch(final Throwable t){
				final ParseException pe = createParseException(reader, t);
				response.addError(pe);

				//restore state of the reader
				reader.restoreFallbackPoint();

				final int position = loader.findNextMessageIndex(reader);
				if(position < 0)
					//cannot find any codec for message
					break;

				reader.position(position);
			}
		}

		//check if there are unread bytes:
		if(!response.hasErrors() && reader.hasRemaining()){
			final IllegalArgumentException error = new IllegalArgumentException("There are remaining bytes");
			final ParseException pe = new ParseException(reader, error);
			response.addError(pe);
		}

		return response;
	}

	private ParseException createParseException(final BitBuffer reader, final Throwable t){
		final byte[] payload = reader.array();
		final byte[] subPayload = Arrays.copyOfRange(payload, reader.position(), payload.length);
		return new ParseException(subPayload, reader.position(), t);
	}

}
