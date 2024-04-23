package io.github.mtrevisan.boxon.core.codecs.behaviors;

import io.github.mtrevisan.boxon.annotations.bindings.ConverterChoices;
import io.github.mtrevisan.boxon.annotations.converters.Converter;
import io.github.mtrevisan.boxon.annotations.validators.Validator;
import io.github.mtrevisan.boxon.io.BitReaderInterface;
import io.github.mtrevisan.boxon.io.BitWriterInterface;


public abstract class CommonBehavior{

	private final ConverterChoices converterChoices;
	private final Class<? extends Converter<?, ?>> defaultConverter;
	private final Class<? extends Validator<?>> validator;


	CommonBehavior(final ConverterChoices converterChoices, final Class<? extends Converter<?, ?>> defaultConverter,
			final Class<? extends Validator<?>> validator){
		this.converterChoices = converterChoices;
		this.defaultConverter = defaultConverter;
		this.validator = validator;
	}


	public ConverterChoices converterChoices(){
		return converterChoices;
	}

	public Class<? extends Converter<?, ?>> defaultConverter(){
		return defaultConverter;
	}


	abstract Object createArray(int arraySize);

	public abstract Object readValue(BitReaderInterface reader);

	public abstract void writeValue(BitWriterInterface writer, Object value);

	public ConverterChoices selectConverterFrom(){
		return converterChoices;
	}

	public Class<? extends Converter<?, ?>> converter(){
		return defaultConverter;
	}

	public Class<? extends Validator<?>> validator(){
		return validator;
	}

}
