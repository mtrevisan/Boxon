package unit731.boxon.dto;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;


public class DeviceTypes{

	private final List<DeviceType> deviceTypes = new ArrayList<>();


	public void add(final String deviceTypeName, final byte deviceTypeCode){
		final DeviceType deviceType = new DeviceType(deviceTypeName, deviceTypeCode);
		deviceTypes.add(deviceType);
	}

	public boolean has(final byte deviceTypeCode){
		try{
			getDeviceTypeName(deviceTypeCode);
			return true;
		}
		catch(final IllegalArgumentException ignored){}
		return false;
	}

	public String getDeviceTypeName(final byte deviceTypeCode){
		for(final DeviceType dt : deviceTypes)
			if(dt.getCode() == deviceTypeCode)
				return dt.getName();

		final String actualCode = Integer.toHexString(deviceTypeCode & 0x0000_00FF);
		final String availableCodes = deviceTypes.stream()
			.map(DeviceType::getCode)
			.map(code -> Integer.toHexString(code & 0x0000_00FF))
			.collect(Collectors.joining(", 0x", "[0x", "]"));
		throw new IllegalArgumentException("Cannot parse message from another device, device type is 0x" + actualCode
			+ ", should be one of " + availableCodes);
	}

}
