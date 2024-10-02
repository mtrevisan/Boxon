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

import io.github.mtrevisan.boxon.exceptions.DataException;

import java.math.BigInteger;
import java.util.Comparator;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;
import java.util.concurrent.ConcurrentSkipListMap;


public final class DeviceTypes<T extends Number>{

	private final Map<T, String> deviceTypes;


	public static <T extends Number> DeviceTypes<T> create(){
		return new DeviceTypes<>();
	}


	private DeviceTypes(){
		final Comparator<T> comparator = (code1, code2) -> {
			final BigInteger bd1 = convertToBigInteger(code1);
			final BigInteger bd2 = convertToBigInteger(code2);
			return bd1.compareTo(bd2);
		};
		deviceTypes = new ConcurrentSkipListMap<>(comparator);
	}

	private static BigInteger convertToBigInteger(final Number number){
		return (number instanceof BigInteger? (BigInteger)number: BigInteger.valueOf(number.longValue()));
	}


	public DeviceTypes<T> with(final T deviceTypeCode, final String deviceTypeName){
		deviceTypes.put(deviceTypeCode, deviceTypeName);

		return this;
	}

	boolean has(final T deviceTypeCode){
		try{
			getDeviceTypeName(deviceTypeCode);
			return true;
		}
		catch(final IllegalArgumentException ignored){}
		return false;
	}

	public Set<T> getDeviceTypeCodes(){
		return deviceTypes.keySet();
	}

	public T getDeviceTypeCode(final String deviceTypeName){
		for(final Map.Entry<T, String> deviceType : deviceTypes.entrySet())
			if(deviceType.getValue().equals(deviceTypeName))
				return deviceType.getKey();

		throw DataException.create("Given device name is not recognized: {}", deviceTypeName);
	}

	public String getDeviceTypeName(final T deviceTypeCode){
		final String deviceTypeName = deviceTypes.get(deviceTypeCode);

		if(deviceTypeName == null){
			final String actualCode = convertToBigInteger(deviceTypeCode).toString(16);
			final StringJoiner sj = new StringJoiner(", 0x", "[0x", "]");
			for(final Map.Entry<T, String> deviceType : deviceTypes.entrySet())
				sj.add(convertToBigInteger(deviceType.getKey()).toString(16));
			throw DataException.create("Cannot decode message from another device, device type is 0x{}, should be one of {}",
				actualCode, sj);
		}
		return deviceTypeName;
	}

	@Override
	public String toString(){
		final StringJoiner sj = new StringJoiner(", ", "[", "]");
		for(final Map.Entry<T, String> deviceType : deviceTypes.entrySet())
			sj.add(deviceType.getValue() + "(0x" + convertToBigInteger(deviceType.getKey()).toString(16) + ")");
		return sj.toString();
	}

}
