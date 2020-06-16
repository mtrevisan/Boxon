package unit731.boxon.other;

import org.apache.commons.lang3.ArrayUtils;

import java.nio.ByteBuffer;
import java.util.Arrays;


/**
 * Lettore di messaggi per il protocollo esadecimale.
 */
public class HexMessageReader{

	private static final int HEADER_LENGTH = 4;


	private ByteBuffer buffer;


	public static String extractHeader(final ByteBuffer payloadBuffer){
		return new String(Arrays.copyOfRange(payloadBuffer.array(), payloadBuffer.position(), payloadBuffer.position() + HEADER_LENGTH));
	}

	public void skip(final int length){
		buffer.position(buffer.position() + length);
	}

	public void skipUntilTerminator(final byte... terminators){
		while(!ArrayUtils.contains(terminators, buffer.get())){}
	}

}
