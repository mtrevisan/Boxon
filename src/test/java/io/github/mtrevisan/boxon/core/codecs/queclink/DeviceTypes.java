/*
 * Copyright (c) 2020-2024 Mauro Trevisan
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
package io.github.mtrevisan.boxon.core.codecs.queclink;

import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;
import java.util.concurrent.ConcurrentSkipListMap;


public class DeviceTypes{

	private final Map<Byte, String> deviceTypes;


	public static DeviceTypes create(){
		return new DeviceTypes();
	}


	private DeviceTypes(){
		deviceTypes = new ConcurrentSkipListMap<>();
	}


	public DeviceTypes with(final byte deviceTypeCode, final String deviceTypeName){
		deviceTypes.put(deviceTypeCode, deviceTypeName);

		return this;
	}

	boolean has(final byte deviceTypeCode){
		try{
			getDeviceTypeName(deviceTypeCode);
			return true;
		}
		catch(final IllegalArgumentException ignored){}
		return false;
	}

	public Set<Byte> getDeviceTypeCodes(){
		return deviceTypes.keySet();
	}

	public byte getDeviceTypeCode(final String deviceTypeName){
		for(final Map.Entry<Byte, String> deviceType : deviceTypes.entrySet())
			if(deviceType.getValue().equals(deviceTypeName))
				return deviceType.getKey();

		throw new IllegalArgumentException("Given device name is not recognized: " + deviceTypeName);
	}

	public String getDeviceTypeName(final byte deviceTypeCode){
		final String deviceTypeName = deviceTypes.get(deviceTypeCode);

		if(deviceTypeName == null){
			final String actualCode = Integer.toHexString(deviceTypeCode & 0x0000_00FF);
			final StringJoiner sj = new StringJoiner(", 0x", "[0x", "]");
			for(final Map.Entry<Byte, String> deviceType : deviceTypes.entrySet())
				sj.add(Integer.toHexString(deviceType.getKey() & 0x0000_00FF));
			throw new IllegalArgumentException("Cannot parse message from another device, device type is 0x"
				+ actualCode.toUpperCase(Locale.ROOT) + ", should be one of " + sj);
		}
		return deviceTypeName;
	}

	@Override
	public String toString(){
		final StringJoiner sj = new StringJoiner(", ", "[", "]");
		for(final Map.Entry<Byte, String> deviceType : deviceTypes.entrySet())
			sj.add(deviceType.getValue() + " (0x" + Integer.toHexString(deviceType.getKey() & 0x0000_00FF).toUpperCase(Locale.ROOT) + ")");
		return sj.toString();
	}

}
