package io.github.mtrevisan.boxon.annotations.checksummers;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;


class CRC16CCITTXMODEMTest{

	@Test
	void oneToFour(){
		CRC16CCITT_XMODEM crc = new CRC16CCITT_XMODEM();
		short crc16 = crc.calculateChecksum(new byte[]{0x01, 0x02, 0x03, 0x04}, 0, 4);

		Assertions.assertEquals((short)0x0D03, crc16);
	}

	@Test
	void test(){
		CRC16CCITT_XMODEM crc = new CRC16CCITT_XMODEM();
		short crc16 = crc.calculateChecksum("9142656".getBytes(StandardCharsets.US_ASCII), 0, 7);

		Assertions.assertEquals((short)0x87F4, crc16);
	}

}