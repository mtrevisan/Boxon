package unit731.boxon.annotations.checksummers;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;


class CRC16Test{

	@Test
	void calculateCRC16(){
		CRC16 crc = new CRC16();
		short crc16 = crc.calculateCRC(new byte[]{0x01, 0x02, 0x03, 0x04}, 0, 4);

		assertEquals((short)0x89C3, crc16);
	}

}
