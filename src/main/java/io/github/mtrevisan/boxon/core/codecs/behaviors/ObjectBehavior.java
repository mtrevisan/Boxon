package io.github.mtrevisan.boxon.core.codecs.behaviors;

import io.github.mtrevisan.boxon.annotations.bindings.BindObject;
import io.github.mtrevisan.boxon.annotations.bindings.ConverterChoices;
import io.github.mtrevisan.boxon.annotations.bindings.ObjectChoices;
import io.github.mtrevisan.boxon.annotations.bindings.ObjectChoicesList;
import io.github.mtrevisan.boxon.annotations.converters.Converter;
import io.github.mtrevisan.boxon.annotations.validators.Validator;
import io.github.mtrevisan.boxon.core.codecs.CodecHelper;

import java.lang.annotation.Annotation;


record ObjectBehavior(Class<?> bindingType, ObjectChoices selectFrom, Class<?> selectDefault, ObjectChoicesList objectChoicesList,
		ConverterChoices converterChoices, Class<? extends Converter<?, ?>> defaultConverter, Class<? extends Validator<?>> validator){

	public static ObjectBehavior of(final Annotation annotation){
		final BindObject binding = (BindObject)annotation;

		final Class<?> bindingType = binding.type();
		final ObjectChoices selectFrom = binding.selectFrom();
		final Class<?> selectDefault = binding.selectDefault();
		final ObjectChoicesList objectChoicesList = binding.selectFromList();
		final ConverterChoices converterChoices = binding.selectConverterFrom();
		final Class<? extends Converter<?, ?>> defaultConverter = binding.converter();
		final Class<? extends Validator<?>> validator = binding.validator();
		return new ObjectBehavior(bindingType, selectFrom, selectDefault, objectChoicesList, converterChoices, defaultConverter, validator);
	}

	private Object createArray(final int arraySize){
		return CodecHelper.createArray(bindingType, arraySize);
	}

}
