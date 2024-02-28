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

import io.github.mtrevisan.boxon.annotations.MessageHeader;
import io.github.mtrevisan.boxon.annotations.bindings.BindByte;
import io.github.mtrevisan.boxon.annotations.bindings.BindListSeparated;
import io.github.mtrevisan.boxon.annotations.bindings.BindString;
import io.github.mtrevisan.boxon.annotations.bindings.BindStringTerminated;
import io.github.mtrevisan.boxon.annotations.bindings.ConverterChoices;
import io.github.mtrevisan.boxon.annotations.bindings.ObjectSeparatedChoices;
import io.github.mtrevisan.boxon.annotations.converters.Converter;
import io.github.mtrevisan.boxon.annotations.converters.NullConverter;
import io.github.mtrevisan.boxon.annotations.validators.NullValidator;
import io.github.mtrevisan.boxon.annotations.validators.Validator;
import io.github.mtrevisan.boxon.core.Core;
import io.github.mtrevisan.boxon.core.CoreBuilder;
import io.github.mtrevisan.boxon.core.Parser;
import io.github.mtrevisan.boxon.core.Response;
import io.github.mtrevisan.boxon.core.parsers.TemplateParser;
import io.github.mtrevisan.boxon.exceptions.AnnotationException;
import io.github.mtrevisan.boxon.exceptions.ConfigurationException;
import io.github.mtrevisan.boxon.exceptions.FieldException;
import io.github.mtrevisan.boxon.exceptions.TemplateException;
import io.github.mtrevisan.boxon.helpers.Evaluator;
import io.github.mtrevisan.boxon.helpers.ReflectionHelper;
import io.github.mtrevisan.boxon.helpers.StringHelper;
import io.github.mtrevisan.boxon.io.BitReader;
import io.github.mtrevisan.boxon.io.BitReaderInterface;
import io.github.mtrevisan.boxon.io.BitWriter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.lang.annotation.Annotation;
import java.nio.charset.StandardCharsets;
import java.util.List;


@SuppressWarnings("ALL")
class CodecListSeparatedTest{

	private static class Version{
		@BindStringTerminated(terminator = ',')
		private final String major;
		@BindStringTerminated(terminator = ',')
		private final String minor;
		@BindStringTerminated(terminator = ',')
		private final String build;

		private Version(final String major, final String minor, final String build){
			this.major = major;
			this.minor = minor;
			this.build = build;
		}
	}

	@MessageHeader(start = "tl4")
	static class TestChoice4{
		@BindString(size = "3")
		String header;
		@BindListSeparated(type = CodecObjectTest.TestType0.class, selectFrom = @ObjectSeparatedChoices(terminator = ',',
			alternatives = {
				@ObjectSeparatedChoices.ObjectSeparatedChoice(condition = "#header == '1'", header = "1", type = CodecObjectTest.TestType1.class),
				@ObjectSeparatedChoices.ObjectSeparatedChoice(condition = "#header == '2'", header = "2", type = CodecObjectTest.TestType2.class)
			}))
		CodecObjectTest.TestType0[] value;
	}

	@MessageHeader(start = "tl5")
	static class TestChoice5{
		@BindString(size = "3")
		String header;
		@BindByte
		byte type;
		@BindListSeparated(selectFrom = @ObjectSeparatedChoices(terminator = ',',
			alternatives = {
				@ObjectSeparatedChoices.ObjectSeparatedChoice(condition = "type == '1'", header = "1", type = CodecObjectTest.TestType1.class),
				@ObjectSeparatedChoices.ObjectSeparatedChoice(condition = "type == '2'", header = "2", type = CodecObjectTest.TestType2.class)
			}))
		List<CodecObjectTest.TestType0> value;
	}


	@Test
	void listOfSameObject() throws FieldException{
		CodecListSeparated codec = new CodecListSeparated();
		Version[] encodedValue = new Version[]{new Version("0", "1", "12"), new Version("1", "2", "0")};
		BindListSeparated annotation = new BindListSeparated(){
			@Override
			public Class<? extends Annotation> annotationType(){
				return BindListSeparated.class;
			}

			@Override
			public String condition(){
				return null;
			}

			@Override
			public Class<?> type(){
				return CodecListSeparatedTest.Version.class;
			}

			@Override
			public ObjectSeparatedChoices selectFrom(){
				return new ObjectSeparatedChoices(){
					@Override
					public String charset(){
						return StandardCharsets.US_ASCII.name();
					}

					@Override
					public Class<? extends Annotation> annotationType(){
						return null;
					}

					@Override
					public byte terminator(){
						return ',';
					}

					@Override
					public ObjectSeparatedChoice[] alternatives(){
						return new ObjectSeparatedChoice[]{
							new ObjectSeparatedChoice(){
								@Override
								public Class<? extends Annotation> annotationType(){
									return ObjectSeparatedChoice.class;
								}

								@Override
								public String condition(){
									return "#header == '666'";
								}

								@Override
								public String header(){
									return "2";
								}

								@Override
								public Class<?> type(){
									return Version.class;
								}
							}
						};
					}
				};
			}

			@Override
			public Class<? extends Validator<?>> validator(){
				return NullValidator.class;
			}

			@Override
			public Class<? extends Converter<?, ?>> converter(){
				return NullConverter.class;
			}

			@Override
			public ConverterChoices selectConverterFrom(){
				return new ConverterChoices(){
					@Override
					public Class<? extends Annotation> annotationType(){
						return ConverterChoices.class;
					}

					@Override
					public ConverterChoice[] alternatives(){
						return new ConverterChoice[0];
					}
				};
			}
		};

		LoaderCodec loaderCodec = LoaderCodec.create();
		Evaluator evaluator = Evaluator.create();
		TemplateParserInterface templateParser = TemplateParser.create(loaderCodec, evaluator);
		loaderCodec.loadDefaultCodecs();
		ReflectionHelper.injectValue(codec, TemplateParserInterface.class, templateParser);
		ReflectionHelper.injectValue(codec, Evaluator.class, Evaluator.create());
		BitWriter writer = BitWriter.create();
		codec.encode(writer, annotation, null, encodedValue);
		writer.flush();

		Assertions.assertEquals("2,0,1,12,2,1,2,0,", new String(writer.array(), StandardCharsets.UTF_8));

		BitReaderInterface reader = BitReader.wrap(writer);
		List<Version> decoded = (List<Version>)codec.decode(reader, annotation, null);

		Assertions.assertEquals(encodedValue.length, decoded.size());
		Assertions.assertEquals(encodedValue[0].major, decoded.get(0).major);
		Assertions.assertEquals(encodedValue[0].minor, decoded.get(0).minor);
		Assertions.assertEquals(encodedValue[1].major, decoded.get(1).major);
		Assertions.assertEquals(encodedValue[1].minor, decoded.get(1).minor);
	}

	@Test
	void listOfDifferentObjects() throws AnnotationException, TemplateException, ConfigurationException{
		Core core = CoreBuilder.builder()
			.withCodecsFrom(CodecChecksum.class, CodecCustomTest.VariableLengthByteArray.class)
			.withTemplatesFrom(TestChoice4.class)
			.create();
		Parser parser = Parser.create(core);

		byte[] payload = StringHelper.toByteArray("7463340112340211223344010666");
		List<Response<byte[], Object>> result = parser.parse(payload);

		Assertions.assertNotNull(result);
		Assertions.assertEquals(1, result.size());
		Response<byte[], Object> response = result.get(0);
		Assertions.assertFalse(response.hasError());
		Assertions.assertEquals(TestChoice4.class, response.getMessage().getClass());
		TestChoice4 parsedMessage = (TestChoice4)response.getMessage();
//		List<CodecListSeparatedTest.TestType0> values = parsedMessage.value;
//		Assertions.assertEquals(CodecListSeparatedTest.TestType1.class, values.get(0).getClass());
//		Assertions.assertEquals(0x1234, ((CodecListSeparatedTest.TestType1)values.get(0)).value);
//		Assertions.assertEquals(CodecListSeparatedTest.TestType2.class, values.get(1).getClass());
//		Assertions.assertEquals(0x1122_3344, ((CodecListSeparatedTest.TestType2)values.get(1)).value);
//		Assertions.assertEquals(CodecListSeparatedTest.TestType1.class, values.get(2).getClass());
//		Assertions.assertEquals(0x0666, ((CodecListSeparatedTest.TestType1)values.get(2)).value);
	}

}
