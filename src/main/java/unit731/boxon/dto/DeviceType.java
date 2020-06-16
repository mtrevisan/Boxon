package unit731.boxon.dto;

import unit731.boxon.utils.ByteHelper;
import org.apache.commons.lang3.StringUtils;


public class DeviceType{

	private final String name;
	private final byte code;


	public DeviceType(final String name, final byte code){
		if(StringUtils.isBlank(name))
			throw new IllegalArgumentException("Device type name cannot be null or empty");

		this.name = name;
		this.code = code;
	}

	String getName(){
		return name;
	}

	byte getCode(){
		return code;
	}

	void validateDeviceTypeCode(final byte deviceTypeCode){
		if(deviceTypeCode != code)
			throw new IllegalArgumentException("Cannot parse message from another device, device type is 0x"
				+ ByteHelper.byteArrayToHexString(new byte[]{(byte)(deviceTypeCode & 0x0000_00FF)}) + ", should be " + code);
	}

}
