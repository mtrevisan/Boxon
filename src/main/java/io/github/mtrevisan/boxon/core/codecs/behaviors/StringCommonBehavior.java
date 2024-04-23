package io.github.mtrevisan.boxon.core.codecs.behaviors;

import io.github.mtrevisan.boxon.annotations.bindings.ConverterChoices;
import io.github.mtrevisan.boxon.annotations.converters.Converter;
import io.github.mtrevisan.boxon.annotations.validators.Validator;
import io.github.mtrevisan.boxon.core.codecs.CodecHelper;

import java.nio.charset.Charset;


public abstract class StringCommonBehavior extends CommonBehavior{

	protected final Charset charset;


	StringCommonBehavior(final Charset charset, final ConverterChoices converterChoices,
			final Class<? extends Converter<?, ?>> defaultConverter, final Class<? extends Validator<?>> validator){
		super(converterChoices, defaultConverter, validator);

		this.charset = charset;
	}


	@Override
	public Object createArray(int arraySize){
		return CodecHelper.createArray(String.class, arraySize);
	}

}
