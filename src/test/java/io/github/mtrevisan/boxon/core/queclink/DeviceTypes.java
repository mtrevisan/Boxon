/**
 * Copyright (c) 2020-2021 Mauro Trevisan
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
package io.github.mtrevisan.boxon.core.queclink;

import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;


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
		for(int i = 0; i < deviceTypes.size(); i ++){
			final DeviceType deviceType = deviceTypes.get(i);
			if(deviceType.getCode() == deviceTypeCode)
				return deviceType.getName();
		}

		final String actualCode = Integer.toHexString(deviceTypeCode & 0x0000_00FF);
		final StringJoiner sj = new StringJoiner(", 0x", "[0x", "]");
		for(int i = 0; i < deviceTypes.size(); i ++)
			sj.add(Integer.toHexString(deviceTypes.get(i).getCode() & 0x0000_00FF));
		throw new IllegalArgumentException("Cannot parse message from another device, device type is 0x" + actualCode
			+ ", should be one of " + sj.toString());
	}

}
