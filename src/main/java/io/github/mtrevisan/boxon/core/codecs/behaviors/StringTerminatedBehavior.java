package io.github.mtrevisan.boxon.core.codecs.behaviors;

import io.github.mtrevisan.boxon.annotations.bindings.BindStringTerminated;
import io.github.mtrevisan.boxon.annotations.bindings.ConverterChoices;
import io.github.mtrevisan.boxon.annotations.converters.Converter;
import io.github.mtrevisan.boxon.annotations.validators.Validator;
import io.github.mtrevisan.boxon.core.codecs.CodecHelper;
import io.github.mtrevisan.boxon.helpers.CharsetHelper;
import io.github.mtrevisan.boxon.io.BitReaderInterface;
import io.github.mtrevisan.boxon.io.BitWriterInterface;
import io.github.mtrevisan.boxon.io.ParserDataType;

import java.lang.annotation.Annotation;
import java.nio.charset.Charset;


record StringTerminatedBehavior(byte terminator, boolean consumeTerminator, Charset charset, ConverterChoices converterChoices,
		Class<? extends Converter<?, ?>> defaultConverter, Class<? extends Validator<?>> validator){

	public static StringTerminatedBehavior of(final Annotation annotation){
		final BindStringTerminated binding = (BindStringTerminated)annotation;

		final byte terminator = binding.terminator();
		final boolean consumeTerminator = binding.consumeTerminator();
		final Charset charset = CharsetHelper.lookup(binding.charset());
		final ConverterChoices converterChoices = binding.selectConverterFrom();
		final Class<? extends Converter<?, ?>> defaultConverter = binding.converter();
		final Class<? extends Validator<?>> validator = binding.validator();
		return new StringTerminatedBehavior(terminator, consumeTerminator, charset, converterChoices, defaultConverter, validator);
	}

	private Object createArray(final int arraySize){
		return CodecHelper.createArray(String.class, arraySize);
	}

	private Object readValue(final BitReaderInterface reader){
		final String text = reader.getTextUntilTerminator(terminator, charset);
		if(consumeTerminator){
			final int length = ParserDataType.getSize(terminator);
			reader.skip(length);
		}
		return text;
	}

	private void writeValue(final BitWriterInterface writer, final Object value){
		writer.putText((String)value, charset);
		if(consumeTerminator)
			writer.putByte(terminator);
	}

}
