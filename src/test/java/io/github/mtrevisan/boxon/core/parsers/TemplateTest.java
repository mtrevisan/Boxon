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
package io.github.mtrevisan.boxon.core.parsers;

import io.github.mtrevisan.boxon.annotations.Checksum;
import io.github.mtrevisan.boxon.annotations.Evaluate;
import io.github.mtrevisan.boxon.annotations.TemplateHeader;
import io.github.mtrevisan.boxon.annotations.bindings.BindArrayPrimitive;
import io.github.mtrevisan.boxon.annotations.bindings.BindAsArray;
import io.github.mtrevisan.boxon.annotations.bindings.BindBitSet;
import io.github.mtrevisan.boxon.annotations.bindings.BindInteger;
import io.github.mtrevisan.boxon.annotations.bindings.BindObject;
import io.github.mtrevisan.boxon.annotations.bindings.BindString;
import io.github.mtrevisan.boxon.annotations.bindings.BindStringTerminated;
import io.github.mtrevisan.boxon.annotations.bindings.ByteOrder;
import io.github.mtrevisan.boxon.annotations.checksummers.CRC16CCITT_FALSE;
import io.github.mtrevisan.boxon.annotations.checksummers.Checksummer;
import io.github.mtrevisan.boxon.annotations.converters.Converter;
import io.github.mtrevisan.boxon.annotations.converters.IntegerToFloatConverter;
import io.github.mtrevisan.boxon.annotations.converters.LongToDoubleConverter;
import io.github.mtrevisan.boxon.core.codecs.LoaderCodec;
import io.github.mtrevisan.boxon.core.helpers.templates.EvaluatedField;
import io.github.mtrevisan.boxon.core.helpers.templates.Template;
import io.github.mtrevisan.boxon.core.helpers.templates.TemplateField;
import io.github.mtrevisan.boxon.exceptions.AnnotationException;
import io.github.mtrevisan.boxon.helpers.JavaHelper;
import io.github.mtrevisan.boxon.utils.TestHelper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.lang.annotation.Annotation;
import java.math.BigInteger;
import java.time.ZonedDateTime;
import java.util.BitSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


class TemplateTest{

	private record Mask(byte mask){
			static class MaskConverter implements Converter<Byte, Mask>{
				@Override
				public Mask decode(final Byte value){
					return new Mask(value);
				}

				@Override
				public Byte encode(final Mask value){
					return value.mask;
				}
			}

		boolean hasProtocolVersion(){
				return TestHelper.hasBit(mask, 2);
			}
	}

	private static class Version{
		@BindInteger(size = "8")
		byte major;
		@BindInteger(size = "8")
		byte minor;
		byte build;
	}

	@TemplateHeader(start = "+", end = "-")
	static class Message{

		private final Map<Byte, String> messageTypeMap = new HashMap<>(2);

		class MessageTypeConverter implements Converter<Byte, String>{
			@Override
			public String decode(final Byte value){
				return messageTypeMap.get(value);
			}

			@Override
			public Byte encode(final String value){
				for(final Map.Entry<Byte, String> elem : messageTypeMap.entrySet()){
					if(elem.getValue().equals(value))
						return elem.getKey();
				}
				return 0x00;
			}
		}

		Message(){
			messageTypeMap.put((byte)0, "AT+GTBSI");
			messageTypeMap.put((byte)1, "AT+GTSRI");
		}

		@BindInteger(size = "8", converter = Mask.MaskConverter.class)
		Mask mask;
		@BindObject(type = Version.class)
		@BindAsArray(size = "2")
		private Version[] versions;
		@BindArrayPrimitive(condition = "mask.hasProtocolVersion()", size = "2", type = byte.class)
		@BindAsArray(size = "2")
		private byte[] protocolVersion;
		@BindBitSet(size = "2")
		private BitSet bitmap;
		@BindInteger(size = "64", converter = LongToDoubleConverter.class)
		private double numberDouble;
		@BindInteger(size = "32", converter = IntegerToFloatConverter.class)
		private float numberFloat;
		@BindInteger(size = "32")
		private int numberInt;
		@BindInteger(size = "64")
		private long numberLong;
		@BindInteger(size = "5")
		private long numberLong2;
		@BindInteger(size = "70")
		private BigInteger numberLong3;
		@BindInteger(size = "16")
		private short numberShort;
		@BindString(size = "4")
		String text;
		@BindStringTerminated(terminator = ',')
		private String textWithTerminator;

		@Checksum(skipStart = 4, skipEnd = 4, algorithm = CRC16CCITT_FALSE.class)
		private short checksum;

		@Evaluate("T(java.time.ZonedDateTime).now()")
		private ZonedDateTime receptionTime;

	}

	@TemplateHeader(start = "++", end = "--")
	private static class MessageChild extends Message{
		@BindInteger(size = "32")
		private int anotherNumberInt;
	}

	@Test
	void creation() throws AnnotationException{
		LoaderCodec loaderCodec = LoaderCodec.create();
		loaderCodec.loadDefaultCodecs();
		LoaderTemplate loaderTemplate = LoaderTemplate.create(loaderCodec);
		Template<Message> template = loaderTemplate.createTemplate(Message.class);

		Assertions.assertNotNull(template);
		Assertions.assertEquals(Message.class, template.getType());
		TemplateHeader header = template.getHeader();
		Assertions.assertNotNull(header);
		Assertions.assertArrayEquals(new String[]{"+"}, header.start());
		Assertions.assertEquals("-", header.end());
		Assertions.assertTrue(template.canBeCoded());
		List<TemplateField> templateFields = template.getTemplateFields();
		Assertions.assertNotNull(templateFields);
		Assertions.assertEquals(14, templateFields.size());
		List<EvaluatedField<Evaluate>> evaluatedFields = template.getEvaluatedFields();
		Assertions.assertNotNull(evaluatedFields);
		Assertions.assertEquals(1, evaluatedFields.size());
		EvaluatedField<Evaluate> evaluatedField = evaluatedFields.getFirst();
		Assertions.assertEquals("receptionTime", evaluatedField.getFieldName());
		Assertions.assertEquals(ZonedDateTime.class, evaluatedField.getFieldType());
		Evaluate evaluate = evaluatedField.getBinding();
		Assertions.assertEquals("T(java.time.ZonedDateTime).now()", evaluate.value());
		TemplateField checksumField = template.getChecksum();
		Assertions.assertNotNull(checksumField);
		Assertions.assertEquals("checksum", checksumField.getFieldName());
		Annotation checksum = checksumField.getBinding();
		Assertions.assertEquals(Checksum.class, checksum.annotationType());
		Checksum cs = new Checksum(){
			@Override
			public Class<? extends Annotation> annotationType(){
				return Checksum.class;
			}

			@Override
			public String condition(){
				return JavaHelper.EMPTY_STRING;
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
				return CRC16CCITT_FALSE.class;
			}

			@Override
			public ByteOrder byteOrder(){
				return ByteOrder.BIG_ENDIAN;
			}
		};
		Assertions.assertEquals(checksum, cs);
	}

	@Test
	void inheritance() throws AnnotationException{
		LoaderCodec loaderCodec = LoaderCodec.create();
		loaderCodec.loadDefaultCodecs();
		LoaderTemplate loaderTemplate = LoaderTemplate.create(loaderCodec);
		Template<MessageChild> template = loaderTemplate.createTemplate(MessageChild.class);

		Assertions.assertNotNull(template);
		Assertions.assertEquals(MessageChild.class, template.getType());
		TemplateHeader header = template.getHeader();
		Assertions.assertNotNull(header);
		Assertions.assertArrayEquals(new String[]{"++"}, header.start());
		Assertions.assertEquals("--", header.end());
		Assertions.assertTrue(template.canBeCoded());
		List<TemplateField> templateFields = template.getTemplateFields();
		Assertions.assertNotNull(templateFields);
		Assertions.assertEquals(15, templateFields.size());
		TemplateField childField = templateFields.getLast();
		Assertions.assertNotNull(childField);
		Assertions.assertEquals("anotherNumberInt", childField.getFieldName());
	}

}
