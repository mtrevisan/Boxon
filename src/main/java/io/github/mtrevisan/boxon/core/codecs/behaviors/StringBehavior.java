package io.github.mtrevisan.boxon.core.codecs.behaviors;

import io.github.mtrevisan.boxon.annotations.bindings.BindString;
import io.github.mtrevisan.boxon.annotations.bindings.ConverterChoices;
import io.github.mtrevisan.boxon.annotations.converters.Converter;
import io.github.mtrevisan.boxon.annotations.validators.Validator;
import io.github.mtrevisan.boxon.core.codecs.CodecHelper;
import io.github.mtrevisan.boxon.exceptions.AnnotationException;
import io.github.mtrevisan.boxon.helpers.CharsetHelper;
import io.github.mtrevisan.boxon.helpers.Evaluator;
import io.github.mtrevisan.boxon.io.BitReaderInterface;
import io.github.mtrevisan.boxon.io.BitWriterInterface;

import java.lang.annotation.Annotation;
import java.nio.charset.Charset;


record StringBehavior(int size, Charset charset, ConverterChoices converterChoices, Class<? extends Converter<?, ?>> defaultConverter,
		Class<? extends Validator<?>> validator){

	public static StringBehavior of(final Annotation annotation, final Evaluator evaluator, final Object rootObject)
			throws AnnotationException{
		final BindString binding = (BindString)annotation;

		final int size = CodecHelper.evaluateSize(binding.size(), evaluator, rootObject);
		final Charset charset = CharsetHelper.lookup(binding.charset());
		final ConverterChoices converterChoices = binding.selectConverterFrom();
		final Class<? extends Converter<?, ?>> defaultConverter = binding.converter();
		final Class<? extends Validator<?>> validator = binding.validator();
		return new StringBehavior(size, charset, converterChoices, defaultConverter, validator);
	}

	private Object createArray(final int arraySize){
		return CodecHelper.createArray(String.class, arraySize);
	}

	private Object readValue(final BitReaderInterface reader){
		return reader.getText(size, charset);
	}

	private void writeValue(final BitWriterInterface writer, Object value){
		String text = (String)value;
		text = text.substring(0, Math.min(text.length(), size));
		writer.putText(text, charset);
	}

}
