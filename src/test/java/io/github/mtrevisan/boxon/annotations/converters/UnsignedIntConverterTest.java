package io.github.mtrevisan.boxon.annotations.converters;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


class UnsignedIntConverterTest{

	@Test
	void valid(){
		UnsignedIntConverter converter = new UnsignedIntConverter();
		Assertions.assertEquals(Long.valueOf(0x0000_0000_2300_0000l), converter.decode(Integer.valueOf(0x2300_0000)));
		Assertions.assertEquals(Integer.valueOf(0x2300_0000), converter.encode(Long.valueOf(0x0000_0000_2300_0000l)));

		Assertions.assertEquals(Long.valueOf(0x0000_0000_CA00_0000l), converter.decode(Integer.valueOf(0xCA00_0000)));
		Assertions.assertEquals(Integer.valueOf(0xCA00_0000), converter.encode(Long.valueOf(0x0000_0000_CA00_0000l)));
	}

}