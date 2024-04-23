package io.github.mtrevisan.boxon.core.codecs.behaviors;

import io.github.mtrevisan.boxon.annotations.bindings.BindObject;
import io.github.mtrevisan.boxon.annotations.bindings.ConverterChoices;
import io.github.mtrevisan.boxon.annotations.bindings.ObjectChoices;
import io.github.mtrevisan.boxon.annotations.bindings.ObjectChoicesList;
import io.github.mtrevisan.boxon.annotations.converters.Converter;
import io.github.mtrevisan.boxon.annotations.validators.Validator;
import io.github.mtrevisan.boxon.core.codecs.CodecHelper;
import io.github.mtrevisan.boxon.io.BitReaderInterface;
import io.github.mtrevisan.boxon.io.BitWriterInterface;

import java.lang.annotation.Annotation;


public final class ObjectBehavior extends CommonBehavior{

	private final Class<?> objectType;
	private final ObjectChoices selectFrom;
	private final Class<?> selectDefault;
	private final ObjectChoicesList objectChoicesList;


	public static ObjectBehavior of(final Annotation annotation){
		final BindObject binding = (BindObject)annotation;

		final Class<?> objectType = binding.type();
		final ObjectChoices selectFrom = binding.selectFrom();
		final Class<?> selectDefault = binding.selectDefault();
		final ObjectChoicesList objectChoicesList = binding.selectFromList();
		final ConverterChoices converterChoices = binding.selectConverterFrom();
		final Class<? extends Converter<?, ?>> defaultConverter = binding.converter();
		final Class<? extends Validator<?>> validator = binding.validator();
		return new ObjectBehavior(objectType, selectFrom, selectDefault, objectChoicesList, converterChoices, defaultConverter, validator);
	}


	ObjectBehavior(final Class<?> objectType, final ObjectChoices selectFrom, final Class<?> selectDefault,
			final ObjectChoicesList objectChoicesList, final ConverterChoices converterChoices,
			final Class<? extends Converter<?, ?>> defaultConverter, final Class<? extends Validator<?>> validator){
		super(converterChoices, defaultConverter, validator);

		this.objectType = objectType;
		this.selectFrom = selectFrom;
		this.selectDefault = selectDefault;
		this.objectChoicesList = objectChoicesList;
	}


	public Class<?> objectType(){
		return objectType;
	}

	public ObjectChoices selectFrom(){
		return selectFrom;
	}

	public Class<?> selectDefault(){
		return selectDefault;
	}

	public ObjectChoicesList objectChoicesList(){
		return objectChoicesList;
	}


	@Override
	public Object createArray(final int arraySize){
		return CodecHelper.createArray(objectType, arraySize);
	}

	@Override
	public Object readValue(final BitReaderInterface reader){
		throw new UnsupportedOperationException();
	}

	@Override
	public void writeValue(final BitWriterInterface writer, final Object value){
		throw new UnsupportedOperationException();
	}

}
