package io.github.mtrevisan.boxon.annotations.converters;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


class UnsignedByteConverterTest{

	@Test
	void valid(){
		UnsignedByteConverter converter = new UnsignedByteConverter();
		Assertions.assertEquals(Short.valueOf((short)0x0023), converter.decode(Byte.valueOf((byte)0x23)));
		Assertions.assertEquals(Byte.valueOf((byte)0x23), converter.encode(Short.valueOf((short)0x0023)));

		Assertions.assertEquals(Short.valueOf((short)0x00CA), converter.decode(Byte.valueOf((byte)0xCA)));
		Assertions.assertEquals(Byte.valueOf((byte)0xCA), converter.encode(Short.valueOf((short)0x00CA)));
	}

}