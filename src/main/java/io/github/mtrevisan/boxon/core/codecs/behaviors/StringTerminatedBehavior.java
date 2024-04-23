package io.github.mtrevisan.boxon.core.codecs.behaviors;

import io.github.mtrevisan.boxon.annotations.bindings.BindStringTerminated;
import io.github.mtrevisan.boxon.annotations.bindings.ConverterChoices;
import io.github.mtrevisan.boxon.annotations.converters.Converter;
import io.github.mtrevisan.boxon.annotations.validators.Validator;
import io.github.mtrevisan.boxon.helpers.CharsetHelper;
import io.github.mtrevisan.boxon.io.BitReaderInterface;
import io.github.mtrevisan.boxon.io.BitWriterInterface;
import io.github.mtrevisan.boxon.io.ParserDataType;

import java.lang.annotation.Annotation;
import java.nio.charset.Charset;


public final class StringTerminatedBehavior extends StringCommonBehavior{

	private final byte terminator;
	private final boolean consumeTerminator;


	public static StringTerminatedBehavior of(final Annotation annotation){
		final BindStringTerminated binding = (BindStringTerminated)annotation;

		final byte terminator = binding.terminator();
		final boolean consumeTerminator = binding.consumeTerminator();
		final Charset charset = CharsetHelper.lookup(binding.charset());
		final ConverterChoices converterChoices = binding.selectConverterFrom();
		final Class<? extends Converter<?, ?>> defaultConverter = binding.converter();
		final Class<? extends Validator<?>> validator = binding.validator();
		return new StringTerminatedBehavior(binding.annotationType(), terminator, consumeTerminator, charset, converterChoices,
			defaultConverter, validator);
	}


	StringTerminatedBehavior(final Class<? extends Annotation> bindingType, final byte terminator, final boolean consumeTerminator,
			final Charset charset, final ConverterChoices converterChoices, final Class<? extends Converter<?, ?>> defaultConverter,
			final Class<? extends Validator<?>> validator){
		super(charset, converterChoices, defaultConverter, validator);

		this.terminator = terminator;
		this.consumeTerminator = consumeTerminator;
	}

	@Override
	public Object readValue(final BitReaderInterface reader){
		final String text = reader.getTextUntilTerminator(terminator, charset);
		if(consumeTerminator){
			final int length = ParserDataType.getSize(terminator);
			reader.skip(length);
		}
		return text;
	}

	@Override
	public void writeValue(final BitWriterInterface writer, final Object value){
		writer.putText((String)value, charset);
		if(consumeTerminator)
			writer.putByte(terminator);
	}

}
