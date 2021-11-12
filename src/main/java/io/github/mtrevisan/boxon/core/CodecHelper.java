/**
 * Copyright (c) 2020-2021 Mauro Trevisan
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
package io.github.mtrevisan.boxon.core;

import io.github.mtrevisan.boxon.annotations.bindings.ConverterChoices;
import io.github.mtrevisan.boxon.annotations.bindings.ObjectChoices;
import io.github.mtrevisan.boxon.annotations.configurations.ConfigurationEnum;
import io.github.mtrevisan.boxon.annotations.converters.Converter;
import io.github.mtrevisan.boxon.annotations.validators.Validator;
import io.github.mtrevisan.boxon.exceptions.AnnotationException;
import io.github.mtrevisan.boxon.exceptions.CodecException;
import io.github.mtrevisan.boxon.exceptions.ConfigurationException;
import io.github.mtrevisan.boxon.external.BitReader;
import io.github.mtrevisan.boxon.external.BitSet;
import io.github.mtrevisan.boxon.external.BitWriter;
import io.github.mtrevisan.boxon.external.ByteOrder;
import io.github.mtrevisan.boxon.internal.JavaHelper;
import io.github.mtrevisan.boxon.internal.ParserDataType;
import io.github.mtrevisan.boxon.internal.ReflectionHelper;

import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.nio.charset.Charset;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


final class CodecHelper{

	/** The name of the current object being scanner (used for referencing variables from SpEL). */
	static final String CONTEXT_SELF = "self";
	/** The name of the prefix for the alternative (used for referencing variables from SpEL). */
	private static final String CONTEXT_CHOICE_PREFIX = "prefix";

	private static final Matcher CONTEXT_PREFIXED_CHOICE_PREFIX = Pattern.compile("#" + CONTEXT_CHOICE_PREFIX + "[^a-zA-Z]")
		.matcher("");

	private static final String EMPTY_STRING = "";
	private static final ObjectChoices.ObjectChoice EMPTY_CHOICE = new ObjectChoices.ObjectChoice(){
		@Override
		public Class<? extends Annotation> annotationType(){
			return Annotation.class;
		}

		@Override
		public String condition(){
			return EMPTY_STRING;
		}

		@Override
		public int prefix(){
			return 0;
		}

		@Override
		public Class<?> type(){
			return Object.class;
		}
	};


	private CodecHelper(){}

	static void assertSizePositive(final int size) throws AnnotationException{
		if(size <= 0)
			throw AnnotationException.create("Size must be a positive integer, was {}", size);
	}

	static void assertSizeEquals(final int expectedSize, final int size){
		if(expectedSize != size)
			throw new IllegalArgumentException("Size mismatch, expected " + expectedSize + ", got " + size);
	}

	static void assertValidCharset(final String charsetName) throws AnnotationException{
		try{
			Charset.forName(charsetName);
		}
		catch(final IllegalArgumentException ignored){
			throw AnnotationException.create("Invalid charset: '{}'", charsetName);
		}
	}

	static ObjectChoices.ObjectChoice chooseAlternative(final ObjectChoices.ObjectChoice[] alternatives, final Class<?> type){
		for(int i = 0; i < alternatives.length; i ++){
			final ObjectChoices.ObjectChoice alternative = alternatives[i];
			if(alternative.type().isAssignableFrom(type))
				return alternative;
		}

		throw new IllegalArgumentException("Cannot find a valid codec for type " + type.getSimpleName());
	}

	static ObjectChoices.ObjectChoice chooseAlternative(final BitReader reader, final ObjectChoices selectFrom, final Object rootObject){
		if(selectFrom.prefixSize() > 0){
			final int prefixSize = selectFrom.prefixSize();
			final ByteOrder prefixByteOrder = selectFrom.byteOrder();
			final int prefix = reader.getInteger(prefixSize, prefixByteOrder)
				.intValue();

			Evaluator.addToContext(CONTEXT_CHOICE_PREFIX, prefix);
		}

		final ObjectChoices.ObjectChoice[] alternatives = selectFrom.alternatives();
		return chooseAlternative(alternatives, rootObject);
	}

	private static ObjectChoices.ObjectChoice chooseAlternative(final ObjectChoices.ObjectChoice[] alternatives,
			final Object rootObject){
		for(int i = 0; i < alternatives.length; i ++){
			final ObjectChoices.ObjectChoice alternative = alternatives[i];
			if(Evaluator.evaluate(alternative.condition(), rootObject, boolean.class))
				return alternative;
		}
		return EMPTY_CHOICE;
	}

	static Class<? extends Converter<?, ?>> chooseConverter(final ConverterChoices selectConverterFrom,
			final Class<? extends Converter<?, ?>> defaultConverter, final Object rootObject){
		final ConverterChoices.ConverterChoice[] alternatives = selectConverterFrom.alternatives();
		for(int i = 0; i < alternatives.length; i ++){
			final ConverterChoices.ConverterChoice alternative = alternatives[i];
			if(Evaluator.evaluate(alternative.condition(), rootObject, boolean.class))
				return alternative.converter();
		}
		return defaultConverter;
	}

	static void writePrefix(final BitWriter writer, final ObjectChoices.ObjectChoice chosenAlternative, final ObjectChoices selectFrom){
		//if chosenAlternative.condition() contains '#prefix', then write @ObjectChoice.prefix()
		if(containsPrefixReference(chosenAlternative.condition())){
			final int prefixSize = selectFrom.prefixSize();
			final ByteOrder prefixByteOrder = selectFrom.byteOrder();

			final BitSet bits = BitSet.valueOf(new long[]{chosenAlternative.prefix()});
			if(prefixByteOrder == ByteOrder.LITTLE_ENDIAN)
				bits.reverseBits(prefixSize);

			writer.putBits(bits, prefixSize);
		}
	}

	static boolean containsPrefixReference(final CharSequence condition){
		return CONTEXT_PREFIXED_CHOICE_PREFIX.reset(condition).find();
	}

	@SuppressWarnings("unchecked")
	static <T> void validateData(final Class<? extends Validator<?>> validatorType, final Object data){
		final Validator<T> validator = (Validator<T>)ReflectionHelper.getCreator(validatorType)
			.get();
		if(!validator.isValid((T)data))
			throw new IllegalArgumentException("Validation with " + validatorType.getSimpleName() + " not passed (value is " + data + ")");
	}

	@SuppressWarnings("unchecked")
	static <IN, OUT> OUT converterDecode(final Class<? extends Converter<?, ?>> converterType, final Object data){
		try{
			final Converter<IN, OUT> converter = (Converter<IN, OUT>)ReflectionHelper.getCreator(converterType)
				.get();

			return converter.decode((IN)data);
		}
		catch(final Exception e){
			throw new IllegalArgumentException("Can not input " + data.getClass().getSimpleName() + " to decode method of converter "
				+ converterType.getSimpleName(), e);
		}
	}

	@SuppressWarnings("unchecked")
	static <IN, OUT> IN converterEncode(final Class<? extends Converter<?, ?>> converterType, final Object data){
		final Converter<IN, OUT> converter = (Converter<IN, OUT>)ReflectionHelper.getCreator(converterType)
			.get();
		return converter.encode((OUT)data);
	}

	static void encode(final BitWriter writer, final Class<?> fieldType, Object value, final int radix, final String charsetName)
			throws ConfigurationException{
		final Charset charset = Charset.forName(charsetName);

		value = interpretValue(value, fieldType);
		if(value != null){
			if(String.class.isInstance(value))
				writer.putText((String)value, charset);
			else{
				final Class<?> fieldClass = ParserDataType.toObjectiveTypeOrSelf(value.getClass());
				if(fieldClass == Float.class)
					writer.putFloat((Float)value, ByteOrder.BIG_ENDIAN);
				else if(fieldClass == Double.class)
					writer.putDouble((Double)value, ByteOrder.BIG_ENDIAN);
				else if(Number.class.isAssignableFrom(fieldClass)){
					value = Long.toString(((Number)value).longValue(), radix);
					writer.putText((String)value, charset);
				}
				else
					throw ConfigurationException.create("Cannot handle this type of field: {}, please report to the developer",
						fieldClass);
			}
		}
	}

	private static Object interpretValue(Object value, final Class<?> fieldType){
		value = JavaHelper.getValueOrDefault(fieldType, value);
		if(value != null){
			if(value.getClass().isEnum())
				value = ((ConfigurationEnum)value).getCode();
			else if(value.getClass().isArray()){
				int compositeEnumValue = 0;
				for(int i = 0; i < Array.getLength(value); i ++)
					compositeEnumValue |= ((ConfigurationEnum)Array.get(value, i)).getCode();
				value = compositeEnumValue;
			}
		}
		return value;
	}


	static CodecInterface<?> retrieveCodec(final Class<? extends Annotation> annotationType, final LoaderCodecInterface loaderCodec,
			final LoaderTemplateInterface loaderTemplate, final TemplateParserInterface templateParser) throws CodecException{
		final CodecInterface<?> codec = loaderCodec.getCodec(annotationType);
		if(codec == null)
			throw CodecException.create("Cannot find codec for binding {}", annotationType.getSimpleName());

		setTemplateParser(codec, loaderTemplate, templateParser);
		return codec;
	}

	private static void setTemplateParser(final CodecInterface<?> codec, final LoaderTemplateInterface loaderTemplate,
			final TemplateParserInterface templateParser){
		try{
			ReflectionHelper.setFieldValue(codec, LoaderTemplateInterface.class, loaderTemplate);
		}
		catch(final Exception ignored){}
		try{
			ReflectionHelper.setFieldValue(codec, TemplateParserInterface.class, templateParser);
		}
		catch(final Exception ignored){}
	}

}
