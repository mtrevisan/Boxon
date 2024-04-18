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
package io.github.mtrevisan.boxon.core.codecs;

import io.github.mtrevisan.boxon.annotations.bindings.ByteOrder;
import io.github.mtrevisan.boxon.exceptions.AnnotationException;
import io.github.mtrevisan.boxon.exceptions.FieldException;
import io.github.mtrevisan.boxon.io.BitReader;
import io.github.mtrevisan.boxon.io.BitReaderInterface;
import io.github.mtrevisan.boxon.io.BitWriter;
import io.github.mtrevisan.boxon.io.BitWriterInterface;
import io.github.mtrevisan.boxon.io.CodecInterface;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Array;


class CodecCustomTest{

	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.FIELD)
	@interface VarLengthEncoded{}


	//the number of bytes to read is determined by the leading bit of each individual bytes
	//(if the first bit of a byte is 1, then another byte is expected to follow)
	static class VariableLengthByteArray implements CodecInterface<VarLengthEncoded>{
		@Override
		public Object decode(final BitReaderInterface reader, final Annotation annotation, final Object rootObject){
			final ByteArrayOutputStream baos = new ByteArrayOutputStream();
			boolean continuing = true;
			while(continuing){
				final byte b = reader.getByte();
				baos.write(b & 0x7F);

				continuing = ((b & 0x80) != 0x00);
			}
			return baos.toByteArray();
		}

		@Override
		public void encode(final BitWriterInterface writer, final Annotation annotation, final Object rootObject, final Object value)
				throws AnnotationException{
			final int size = Array.getLength(value);
			for(int i = 0; i < size; i ++)
				writer.put((byte)((byte)Array.get(value, i) | (i < size - 1? (byte)0x80: 0x00)), ByteOrder.BIG_ENDIAN);
		}
	}


	@Test
	void customAnnotation() throws FieldException{
		LoaderCodec loaderCodec = LoaderCodec.create();
		loaderCodec.addCodecs(new VariableLengthByteArray());

		CodecInterface<?> codec = loaderCodec.getCodec(VarLengthEncoded.class);
		byte[] encodedValue = {0x01, 0x02, 0x03};
		VarLengthEncoded annotation = new VarLengthEncoded(){
			@Override
			public Class<? extends Annotation> annotationType(){
				return VarLengthEncoded.class;
			}
		};

		BitWriter writer = BitWriter.create();
		codec.encode(writer, annotation, null, encodedValue);
		writer.flush();

		Assertions.assertArrayEquals(new byte[]{(byte)0x81, (byte)0x82, 0x03}, writer.array());

		BitReaderInterface reader = BitReader.wrap(writer);
		byte[] decoded = (byte[])codec.decode(reader, annotation, null);

		Assertions.assertArrayEquals(encodedValue, decoded);
	}

}
