package unit731.boxon.codecs;

import unit731.boxon.utils.ByteHelper;
import unit731.boxon.utils.ExceptionHelper;

import java.util.StringJoiner;


public class ParseException extends Exception{

	private static final long serialVersionUID = -7230533024483622086L;


	private final byte[] wholeMessage;
	private final int errorIndex;


	public ParseException(final BitBuffer reader, final Throwable cause){
		this(reader.array(), reader.position(), cause);
	}

	public ParseException(final byte[] wholeMessage, final int errorIndex, final Throwable cause){
		super(cause);

		this.wholeMessage = wholeMessage;
		this.errorIndex = errorIndex;
	}

	@Override
	public String getMessage(){
		final StringJoiner sj = new StringJoiner(System.lineSeparator());
		sj.add("Error decoding message: " + ByteHelper.byteArrayToHexString(wholeMessage));
		if(getCause() != null)
			sj.add(ExceptionHelper.getMessageNoLineNumber(getCause()));
		if(errorIndex >= 0){
			sj.add("   at index " + errorIndex);
		}
		return sj.toString();
	}

}
