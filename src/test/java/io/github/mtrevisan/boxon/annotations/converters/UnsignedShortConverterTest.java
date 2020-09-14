package io.github.mtrevisan.boxon.annotations.converters;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


class UnsignedShortConverterTest{

	@Test
	void valid(){
		UnsignedShortConverter converter = new UnsignedShortConverter();
		Assertions.assertEquals(Integer.valueOf(0x0000_2300), converter.decode(Short.valueOf((short)0x2300)));
		Assertions.assertEquals(Short.valueOf((short)0x2300_0000), converter.encode(Integer.valueOf(0x2300_0000)));

		Assertions.assertEquals(Integer.valueOf(0x0000_CA00), converter.decode(Short.valueOf((short)0xCA00)));
		Assertions.assertEquals(Short.valueOf((short)0xCA00_0000), converter.encode(Integer.valueOf(0x00CA_0000)));
	}

}