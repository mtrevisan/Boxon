/*
 * Copyright (c) 2020-2022 Mauro Trevisan
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
package io.github.mtrevisan.boxon.codecs;

import io.github.mtrevisan.boxon.annotations.Checksum;
import io.github.mtrevisan.boxon.annotations.checksummers.CRC16CCITT;
import io.github.mtrevisan.boxon.annotations.checksummers.Checksummer;
import io.github.mtrevisan.boxon.exceptions.FieldException;
import io.github.mtrevisan.boxon.external.codecs.BitReader;
import io.github.mtrevisan.boxon.external.codecs.BitWriter;
import io.github.mtrevisan.boxon.external.codecs.ByteOrder;
import io.github.mtrevisan.boxon.external.codecs.CodecInterface;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.lang.annotation.Annotation;
import java.util.Locale;
import java.util.Random;


@SuppressWarnings("ALL")
class CodecChecksumTest{

	private static final Random RANDOM = new Random();


	@Test
	void checksumShort() throws FieldException{
		CodecInterface<Checksum> codec = new CodecChecksum();
		short encodedValue = (short)RANDOM.nextInt(0x0000_FFFF);
		Checksum annotation = new Checksum(){
			@Override
			public Class<? extends Annotation> annotationType(){
				return Checksum.class;
			}

			@Override
			public Class<?> type(){
				return short.class;
			}

			@Override
			public ByteOrder byteOrder(){
				return ByteOrder.BIG_ENDIAN;
			}

			@Override
			public int skipStart(){
				return 2;
			}

			@Override
			public int skipEnd(){
				return 0;
			}

			@Override
			public Class<? extends Checksummer> algorithm(){
				return CRC16CCITT.class;
			}

			@Override
			public short startValue(){
				return CRC16CCITT.START_VALUE_0xFFFF;
			}
		};

		BitWriter writer = BitWriter.create();
		codec.encode(writer, annotation, null, encodedValue);
		writer.flush();

		Assertions.assertEquals(StringUtils.leftPad(Integer.toHexString(encodedValue & 0x0000_FFFF).toUpperCase(Locale.ROOT), 4, '0'), writer.toString());

		BitReader reader = BitReader.wrap(writer);
		short decoded = (short)codec.decode(reader, annotation, null);

		Assertions.assertEquals(encodedValue, decoded);
	}

}
