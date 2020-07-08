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
package unit731.boxon.coders;

import unit731.boxon.annotations.ByteOrder;
import unit731.boxon.annotations.Evaluate;
import unit731.boxon.annotations.BindArray;
import unit731.boxon.annotations.BindArrayPrimitive;
import unit731.boxon.annotations.BindBits;
import unit731.boxon.annotations.BindByte;
import unit731.boxon.annotations.BindChecksum;
import unit731.boxon.annotations.BindDouble;
import unit731.boxon.annotations.BindFloat;
import unit731.boxon.annotations.BindIf;
import unit731.boxon.annotations.BindInt;
import unit731.boxon.annotations.BindLong;
import unit731.boxon.annotations.BindDecimal;
import unit731.boxon.annotations.BindInteger;
import unit731.boxon.annotations.BindShort;
import unit731.boxon.annotations.BindString;
import unit731.boxon.annotations.BindStringTerminated;
import unit731.boxon.annotations.MessageHeader;
import unit731.boxon.annotations.checksummers.CRC16;
import unit731.boxon.annotations.checksummers.Checksummer;
import unit731.boxon.annotations.converters.Converter;
import unit731.boxon.helpers.BitSet;
import unit731.boxon.helpers.ByteHelper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.lang.annotation.Annotation;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


class CodecTest{

	private class Mask{

		public class MaskConverter implements Converter<Byte, Mask>{
			@Override
			public Mask decode(final Byte value){
				return new Mask(value);
			}

			@Override
			public Byte encode(final Mask value){
				return value.mask;
			}
		}


		private final byte mask;


		public Mask(byte mask){
			this.mask = mask;
		}

		public boolean hasProtocolVersion(){
			return ByteHelper.hasBit(mask, 2);
		}

	}

	private class Version{
		@BindByte
		public byte major;
		@BindByte
		public byte minor;
		public byte build;
	}

	@MessageHeader(start = "+", end = "-")
	private class Message{

		private final Map<Byte, String> MESSAGE_TYPE_MAP = new HashMap<>();

		public class MessageTypeConverter implements Converter<Byte, String>{
			@Override
			public String decode(final Byte value){
				return MESSAGE_TYPE_MAP.get(value);
			}

			@Override
			public Byte encode(final String value){
				for(final Map.Entry<Byte, String> elem : MESSAGE_TYPE_MAP.entrySet()){
					if(elem.getValue().equals(value))
						return elem.getKey();
				}
				return 0x00;
			}
		}

		Message(){
			MESSAGE_TYPE_MAP.put((byte)0, "AT+GTBSI");
			MESSAGE_TYPE_MAP.put((byte)1, "AT+GTSRI");
		}

		@BindByte(converter = Mask.MaskConverter.class)
		public Mask mask;
		@BindArray(size = "2", type = Version.class)
		private Version[] versions;
		@BindIf("mask.hasProtocolVersion()")
		@BindArrayPrimitive(size = "2", type = byte[].class)
		private byte[] protocolVersion;
		@BindBits(size = "2")
		private BitSet bits;
		@BindDouble
		private double numberDouble;
		@BindFloat
		private float numberFloat;
		@BindInt
		private int numberInt;
		@BindLong
		private long numberLong;
		@BindInteger(size = "5")
		private long numberLong2;
		@BindInteger(size = "70")
		private BigInteger numberLong3;
		@BindDecimal(type = Double.class)
		private BigDecimal number;
		@BindShort
		private short numberShort;
		@BindString(size = "4")
		public String text;
		@BindStringTerminated(terminator = ',')
		private String textWithTerminator;

		@BindChecksum(type = short.class, skipStart = 4, skipEnd = 4, algorithm = CRC16.class, startValue = CRC16.START_VALUE_0xFFFF)
		private short checksum;

		@Evaluate("T(java.time.ZonedDateTime).now()")
		private ZonedDateTime receptionTime;

	}

	@MessageHeader(start = "++", end = "--")
	private class MessageChild extends Message{
		@BindInt
		private int anotherNumberInt;
	}

	@Test
	void creation(){
		Codec<Message> codec = Codec.createFrom(Message.class);

		Assertions.assertNotNull(codec);
		Assertions.assertEquals(Message.class, codec.getType());
		MessageHeader header = codec.getHeader();
		Assertions.assertNotNull(header);
		Assertions.assertArrayEquals(new String[]{"+"}, header.start());
		Assertions.assertEquals("-", header.end());
		Assertions.assertTrue(codec.canBeDecoded());
		List<Codec.BoundedField> boundedFields = codec.getBoundedFields();
		Assertions.assertNotNull(boundedFields);
		Assertions.assertEquals(15, boundedFields.size());
		//TODO assert bounded fields
		List<Codec.EvaluatedField> evaluatedFields = codec.getEvaluatedFields();
		Assertions.assertNotNull(evaluatedFields);
		Assertions.assertEquals(1, evaluatedFields.size());
		Codec.EvaluatedField evaluatedField = evaluatedFields.get(0);
		Assertions.assertEquals("receptionTime", evaluatedField.getName());
		Assertions.assertEquals(ZonedDateTime.class, evaluatedField.getType());
		Evaluate evaluate = evaluatedField.getBinding();
		Assertions.assertEquals("T(java.time.ZonedDateTime).now()", evaluate.value());
		Codec.BoundedField checksumField = codec.getChecksum();
		Assertions.assertNotNull(checksumField);
		Assertions.assertEquals("checksum", checksumField.getName());
		Assertions.assertEquals(null, checksumField.getCondition());
		Annotation checksum = checksumField.getBinding();
		Assertions.assertEquals(BindChecksum.class, checksum.annotationType());
		BindChecksum cs = new BindChecksum(){
			@Override
			public Class<? extends Annotation> annotationType(){
				return BindChecksum.class;
			}

			@Override
			public Class<?> type(){
				return short.class;
			}

			@Override
			public int skipStart(){
				return 4;
			}

			@Override
			public int skipEnd(){
				return 4;
			}

			@Override
			public Class<? extends Checksummer> algorithm(){
				return CRC16.class;
			}

			@Override
			public long startValue(){
				return CRC16.START_VALUE_0xFFFF;
			}

			@Override
			public ByteOrder byteOrder(){
				return ByteOrder.BIG_ENDIAN;
			}
		};
		Assertions.assertTrue(checksum.equals(cs));
	}

	@Test
	void inheritance(){
		Codec<MessageChild> codec = Codec.createFrom(MessageChild.class);

		Assertions.assertNotNull(codec);
		Assertions.assertEquals(MessageChild.class, codec.getType());
		MessageHeader header = codec.getHeader();
		Assertions.assertNotNull(header);
		Assertions.assertArrayEquals(new String[]{"++"}, header.start());
		Assertions.assertEquals("--", header.end());
		Assertions.assertTrue(codec.canBeDecoded());
		List<Codec.BoundedField> boundedFields = codec.getBoundedFields();
		Assertions.assertNotNull(boundedFields);
		Assertions.assertEquals(16, boundedFields.size());
		Codec.BoundedField childField = boundedFields.get(boundedFields.size() - 1);
		Assertions.assertNotNull(childField);
		Assertions.assertEquals("anotherNumberInt", childField.getName());
	}

}
