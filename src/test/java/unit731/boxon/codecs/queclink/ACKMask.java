package unit731.boxon.codecs.queclink;

import com.fasterxml.jackson.annotation.JsonValue;
import unit731.boxon.annotations.transformers.Transformer;
import unit731.boxon.utils.ByteHelper;


public class ACKMask{

	public static class ACKMaskTransformer implements Transformer<Byte, ACKMask>{
		@Override
		public ACKMask decode(final Byte value){
			return new ACKMask(value);
		}

		@Override
		public Byte encode(final ACKMask value){
			return value.mask;
		}
	}


	@JsonValue
	private final byte mask;


	public ACKMask(byte mask){
		this.mask = mask;
	}

	public int getMaskLength(){
		return 1;
	}

	public int getMessageLengthLength(){
		return 1;
	}

	public boolean hasMessageId(){
		return ByteHelper.hasBit(mask, 6);
	}

	public boolean hasEventTime(){
		return ByteHelper.hasBit(mask, 5);
	}

	public boolean hasIMEI(){
		//NOTE: negated logic!
		return !ByteHelper.hasBit(mask, 4);
	}

	public boolean hasFirmwareVersion(){
		return ByteHelper.hasBit(mask, 3);
	}

	public boolean hasProtocolVersion(){
		return ByteHelper.hasBit(mask, 2);
	}

	public boolean hasDeviceType(){
		return ByteHelper.hasBit(mask, 1);
	}

	public boolean hasLength(){
		return ByteHelper.hasBit(mask, 0);
	}

}
