/*
 * Copyright (c) 2024 Mauro Trevisan
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
package io.github.mtrevisan.outsidepackage;

import io.github.mtrevisan.boxon.annotations.TemplateHeader;
import io.github.mtrevisan.boxon.annotations.bindings.BindStringTerminated;
import io.github.mtrevisan.boxon.annotations.bindings.ByteOrder;
import io.github.mtrevisan.boxon.annotations.bindings.ConverterChoices;
import io.github.mtrevisan.boxon.annotations.converters.Converter;
import io.github.mtrevisan.boxon.annotations.converters.NullConverter;
import io.github.mtrevisan.boxon.annotations.validators.NullValidator;
import io.github.mtrevisan.boxon.annotations.validators.Validator;
import io.github.mtrevisan.boxon.core.Composer;
import io.github.mtrevisan.boxon.core.Core;
import io.github.mtrevisan.boxon.core.CoreBuilder;
import io.github.mtrevisan.boxon.core.Parser;
import io.github.mtrevisan.boxon.core.Response;
import io.github.mtrevisan.boxon.exceptions.BoxonException;
import io.github.mtrevisan.boxon.io.BitReaderInterface;
import io.github.mtrevisan.boxon.io.BitSetHelper;
import io.github.mtrevisan.boxon.io.BitWriterInterface;
import io.github.mtrevisan.boxon.io.CodecInterface;
import io.github.mtrevisan.boxon.io.Evaluator;
import io.github.mtrevisan.boxon.io.Injected;
import io.github.mtrevisan.boxon.utils.TestHelper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.lang.annotation.Annotation;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.math.BigInteger;
import java.util.BitSet;
import java.util.List;


class CustomCodecTest{

	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.FIELD)
	@Documented
	public @interface BindCustomData{
		String condition() default "";

		String charset() default "UTF-8";

		String size() default "0";

		Class<? extends Validator<?>> validator() default NullValidator.class;

		Class<? extends Converter<?, ?>> converter() default NullConverter.class;

		ConverterChoices selectConverterFrom() default @ConverterChoices;
	}

	@TemplateHeader(start = "tcc", end = "/")
	private static class TestCustomCodec{
		@BindStringTerminated(terminator = ',')
		String header;
		@BindCustomData(size = "4")
		String customData;
	}


	@Test
	void test() throws BoxonException{
		CodecInterface codec = new CodecInterface(){
			@Injected
			private Evaluator evaluator;

			@Override
			public Class<? extends Annotation> annotationType(){
				return BindCustomData.class;
			}

			@Override
			public Object decode(BitReaderInterface reader, Annotation annotation, Annotation collectionBinding, Object rootObject){
				BindCustomData binding = (BindCustomData)annotation;

				int size = evaluator.evaluateSize(binding.size(), rootObject);
				BigInteger value = reader.getBigInteger(size * Byte.SIZE, ByteOrder.BIG_ENDIAN);

				return bigIntegerToAscii(value);
			}

			private static String bigIntegerToAscii(BigInteger number){
				String hex = number.toString(16);

				StringBuilder sb = new StringBuilder();
				for(int i = 0, length = hex.length(); i < length; i += 2){
					String hexDigit = hex.substring(i, Math.min(i + 2, hex.length()));
					int asciiValue = Integer.parseInt(hexDigit, 16);
					sb.append((char)asciiValue);
				}
				return sb.toString();
			}

			@Override
			public void encode(BitWriterInterface writer, Annotation annotation, Annotation collectionBinding, Object rootObject,
					Object value){
				BindCustomData binding = (BindCustomData)annotation;

				int size = evaluator.evaluateSize(binding.size(), rootObject);

				final BigInteger v = asciiToBigDecimal((String)value);
				final BitSet bitmap = BitSetHelper.createBitSet(size * Byte.SIZE, v, ByteOrder.BIG_ENDIAN);

				writer.putBitSet(bitmap, size * Byte.SIZE);
			}

			public static BigInteger asciiToBigDecimal(String asciiString){
				StringBuilder hexBuilder = new StringBuilder();
				for(int i = 0, length = asciiString.length(); i < length; i ++){
					char asciiChar = asciiString.charAt(i);
					String hexDigit = String.format("%02X", (int)asciiChar);
					hexBuilder.append(hexDigit);
				}
				return new BigInteger(hexBuilder.toString(), 16);
			}
		};

		Core core = CoreBuilder.builder()
			.withDefaultCodecs()
			.withCodec(codec)
			.withTemplate(TestCustomCodec.class)
			.create();
		Parser parser = Parser.create(core);
		Composer composer = Composer.create(core);

		byte[] payload = TestHelper.toByteArray("tcc,1234/");
		List<Response<byte[], Object>> result = parser.parse(payload);

		Assertions.assertNotNull(result);
		Assertions.assertEquals(1, result.size());
		Response<byte[], Object> response = result.getFirst();
		if(response.hasError())
			Assertions.fail(response.getError());
		Assertions.assertEquals(TestCustomCodec.class, response.getMessage().getClass());
		final TestCustomCodec message = (TestCustomCodec)response.getMessage();
		Assertions.assertEquals("tcc", message.header);
		Assertions.assertEquals("1234", message.customData);

		Response<TestCustomCodec, byte[]> composeResult = composer.compose(message);

		if(composeResult.hasError())
			Assertions.fail(composeResult.getError());
		Assertions.assertArrayEquals(payload, composeResult.getMessage());
	}

}