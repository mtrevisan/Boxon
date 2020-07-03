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
package unit731.boxon.coders.queclink;

import unit731.boxon.helpers.ByteHelper;
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
				+ ByteHelper.toHexString(new byte[]{(byte)(deviceTypeCode & 0x0000_00FF)}) + ", should be " + code);
	}

}
