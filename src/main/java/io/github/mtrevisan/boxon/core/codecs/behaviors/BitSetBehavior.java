package io.github.mtrevisan.boxon.core.codecs.behaviors;

import io.github.mtrevisan.boxon.annotations.bindings.BindBitSet;
import io.github.mtrevisan.boxon.annotations.bindings.ConverterChoices;
import io.github.mtrevisan.boxon.annotations.converters.Converter;
import io.github.mtrevisan.boxon.annotations.validators.Validator;
import io.github.mtrevisan.boxon.core.codecs.CodecHelper;
import io.github.mtrevisan.boxon.exceptions.AnnotationException;
import io.github.mtrevisan.boxon.helpers.Evaluator;
import io.github.mtrevisan.boxon.io.BitReaderInterface;
import io.github.mtrevisan.boxon.io.BitWriterInterface;

import java.lang.annotation.Annotation;
import java.util.BitSet;


public class BitSetBehavior extends CommonBehavior{

	protected final int size;


	public static BitSetBehavior of(final Annotation annotation, final Evaluator evaluator, final Object rootObject)
			throws AnnotationException{
		final BindBitSet binding = (BindBitSet)annotation;

		final int size = CodecHelper.evaluateSize(binding.size(), evaluator, rootObject);
		final ConverterChoices converterChoices = binding.selectConverterFrom();
		final Class<? extends Converter<?, ?>> defaultConverter = binding.converter();
		final Class<? extends Validator<?>> validator = binding.validator();
		return new BitSetBehavior(size, converterChoices, defaultConverter, validator);
	}


	BitSetBehavior(final int size, final ConverterChoices converterChoices, final Class<? extends Converter<?, ?>> defaultConverter,
			final Class<? extends Validator<?>> validator){
		super(converterChoices, defaultConverter, validator);

		this.size = size;
	}


	public int size(){
		return size;
	}


	@Override
	public Object createArray(final int arraySize){
		return CodecHelper.createArray(BitSet.class, arraySize);
	}

	@Override
	public Object readValue(final BitReaderInterface reader){
		return reader.getBitSet(size);
	}

	@Override
	public void writeValue(final BitWriterInterface writer, final Object value){
		writer.putBitSet((BitSet)value, size);
	}

}
