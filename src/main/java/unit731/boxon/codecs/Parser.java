package unit731.boxon.codecs;

import unit731.boxon.dto.ParseResponse;

import java.util.Arrays;
import java.util.Map;
import java.util.Objects;


public class Parser{

	private final Loader loader = new Loader();


	protected Parser(final Map<String, Object> context){
		loader.init();

		copyContext(context);
	}

	protected Parser(final Map<String, Codec<?>> codecs, final Map<String, Object> context){
		Objects.requireNonNull(codecs, "Codecs cannot be null");
		if(codecs.isEmpty())
			throw new IllegalArgumentException("Codecs cannot be empty");

		loader.init(codecs);

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
