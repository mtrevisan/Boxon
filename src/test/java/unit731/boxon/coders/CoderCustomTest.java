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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import unit731.boxon.annotations.ByteOrder;

import java.io.ByteArrayOutputStream;
import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Array;


class CoderCustomTest{

	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.FIELD)
	@interface VarLengthEncoded{}


	//the number of bytes to read is determined by the leading bit of each individual bytes
	//(if the first bit of a byte is 1, then another byte is expected to follow)
	class VariableLengthByteArray implements CoderInterface<VarLengthEncoded>{
		@Override
		public Object decode(final BitBuffer reader, final Annotation annotation, final Object data){
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
		public void encode(final BitWriter writer, final Annotation annotation, final Object data, final Object value){
			final int size = Array.getLength(value);
			for(int i = 0; i < size; i ++)
				writer.put((byte)((byte)Array.get(value, i) | (i < size - 1? (byte)0x80: 0x00)), ByteOrder.BIG_ENDIAN);
		}

		@Override
		public Class<VarLengthEncoded> coderType(){
			return VarLengthEncoded.class;
		}
	}


	@Test
	void customAnnotation(){
		MessageParser messageParser = new MessageParser();
		messageParser.loader.addCoder(new VariableLengthByteArray());

		CoderInterface coder = messageParser.loader.getCoder(VarLengthEncoded.class);
		byte[] encodedValue = new byte[]{0x01, 0x02, 0x03};
		VarLengthEncoded annotation = new VarLengthEncoded(){
			@Override
			public Class<? extends Annotation> annotationType(){
				return VarLengthEncoded.class;
			}
		};

		BitWriter writer = new BitWriter();
		coder.encode(writer, annotation, null, encodedValue);
		writer.flush();

		Assertions.assertArrayEquals(new byte[]{(byte)0x81, (byte)0x82, 0x03}, writer.array());

		BitBuffer reader = BitBuffer.wrap(writer);
		byte[] decoded = (byte[])coder.decode(reader, annotation, null);

		Assertions.assertArrayEquals(encodedValue, decoded);
	}

}
