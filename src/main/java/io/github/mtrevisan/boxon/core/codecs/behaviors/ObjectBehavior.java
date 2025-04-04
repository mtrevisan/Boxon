/*
 * Copyright (c) 2024 Mauro Trevisan
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
package io.github.mtrevisan.boxon.core.codecs.behaviors;

import io.github.mtrevisan.boxon.annotations.bindings.BindObject;
import io.github.mtrevisan.boxon.annotations.bindings.ConverterChoices;
import io.github.mtrevisan.boxon.annotations.bindings.ObjectChoices;
import io.github.mtrevisan.boxon.annotations.bindings.ObjectChoicesList;
import io.github.mtrevisan.boxon.annotations.converters.Converter;
import io.github.mtrevisan.boxon.annotations.validators.Validator;
import io.github.mtrevisan.boxon.core.helpers.CodecHelper;
import io.github.mtrevisan.boxon.io.BitReaderInterface;
import io.github.mtrevisan.boxon.io.BitWriterInterface;

import java.lang.annotation.Annotation;


/**
 * A concrete implementation of {@link CommonBehavior} tailored to handle object-specific behavior based on annotations.
 */
public final class ObjectBehavior extends CommonBehavior{

	private final Class<?> objectType;
	private final ObjectChoices selectFrom;
	private final Class<?> selectDefault;
	private final ObjectChoicesList objectChoicesList;


	/**
	 * Creates an instance of this class based on the given annotation.
	 *
	 * @param annotation	The annotation to create the behavior from.
	 * @return	An instance of this class.
	 */
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


	private ObjectBehavior(final Class<?> objectType, final ObjectChoices selectFrom, final Class<?> selectDefault,
			final ObjectChoicesList objectChoicesList, final ConverterChoices converterChoices,
			final Class<? extends Converter<?, ?>> defaultConverter, final Class<? extends Validator<?>> validator){
		super(converterChoices, defaultConverter, validator);

		this.objectType = objectType;
		this.selectFrom = selectFrom;
		this.selectDefault = selectDefault;
		this.objectChoicesList = objectChoicesList;
	}


	/**
	 * Returns the type of the object, as defined in the annotation.
	 *
	 * @return	The type of the object.
	 */
	public Class<?> objectType(){
		return objectType;
	}

	/**
	 * Retrieves the selected choices from the {@link ObjectChoices} annotation.
	 *
	 * @return	The selected choices from the {@link ObjectChoices} annotation.
	 */
	public ObjectChoices selectFrom(){
		return selectFrom;
	}

	/**
	 * Retrieves the default class type.
	 *
	 * @return	The default class type.
	 */
	public Class<?> selectDefault(){
		return selectDefault;
	}

	/**
	 * Returns the {@link ObjectChoicesList} annotation object associated with this method.
	 *
	 * @return	The {@link ObjectChoicesList} annotation object.
	 */
	public ObjectChoicesList objectChoicesList(){
		return objectChoicesList;
	}


	@Override
	public Object createArray(final int arraySize){
		return CodecHelper.createArray(objectType, arraySize);
	}

	@Override
	public Object readValue(final BitReaderInterface reader){
		throw new UnsupportedOperationException("Unimplemented method");
	}

	@Override
	public void writeValue(final BitWriterInterface writer, final Object value){
		throw new UnsupportedOperationException("Unimplemented method");
	}

}
