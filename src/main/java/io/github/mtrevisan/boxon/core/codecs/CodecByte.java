/*
 * Copyright (c) 2020-2024 Mauro Trevisan
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
package io.github.mtrevisan.boxon.core.codecs;

import io.github.mtrevisan.boxon.annotations.bindings.BindByte;
import io.github.mtrevisan.boxon.annotations.bindings.ConverterChoices;
import io.github.mtrevisan.boxon.annotations.converters.Converter;
import io.github.mtrevisan.boxon.annotations.validators.Validator;
import io.github.mtrevisan.boxon.exceptions.DataException;
import io.github.mtrevisan.boxon.helpers.ConstructorHelper;
import io.github.mtrevisan.boxon.helpers.Evaluator;
import io.github.mtrevisan.boxon.helpers.Injected;
import io.github.mtrevisan.boxon.io.BitReaderInterface;
import io.github.mtrevisan.boxon.io.BitWriterInterface;
import io.github.mtrevisan.boxon.io.CodecInterface;

import java.lang.annotation.Annotation;


final class CodecByte implements CodecInterface<BindByte>{

	@SuppressWarnings("unused")
	@Injected
	private Evaluator evaluator;


	@Override
	public Object decode(final BitReaderInterface reader, final Annotation annotation, final Object rootObject){
		final BindByte binding = interpretBinding(annotation);

		final byte value = reader.getByte();

		return convertValue(binding, rootObject, value);
	}

	@Override
	public void encode(final BitWriterInterface writer, final Annotation annotation, final Object rootObject, final Object value){
		final BindByte binding = interpretBinding(annotation);

		validate(value, binding.validator());

		final Class<? extends Converter<?, ?>> chosenConverter = getChosenConverter(binding, rootObject);
		final byte v = CodecHelper.converterEncode(chosenConverter, value);

		writer.putByte(v);
	}


	private <IN, OUT> OUT convertValue(final BindByte binding, final Object rootObject, final IN value){
		final Class<? extends Converter<?, ?>> converterType = getChosenConverter(binding, rootObject);
		final OUT convertedValue = converterDecode(converterType, value);
		validate(convertedValue, binding.validator());
		return convertedValue;
	}

	/**
	 * Get the first converter that matches the condition.
	 *
	 * @return	The converter class.
	 */
	private Class<? extends Converter<?, ?>> getChosenConverter(final BindByte binding, final Object rootObject){
		final ConverterChoices.ConverterChoice[] alternatives = binding.selectConverterFrom().alternatives();
		for(int i = 0, length = alternatives.length; i < length; i ++){
			final ConverterChoices.ConverterChoice alternative = alternatives[i];

			if(evaluator.evaluateBoolean(alternative.condition(), rootObject))
				return alternative.converter();
		}
		return binding.converter();
	}

	private static <IN, OUT> OUT converterDecode(final Class<? extends Converter<?, ?>> converterType, final IN data){
		try{
			final Converter<IN, OUT> converter = (Converter<IN, OUT>)ConstructorHelper.getEmptyCreator(converterType)
				.get();

			return converter.decode(data);
		}
		catch(final Exception e){
			throw DataException.create("Can not input {} ({}) to decode method of converter {}",
				data.getClass().getSimpleName(), data, converterType.getSimpleName(), e);
		}
	}

	/**
	 * Validate the value passed using the configured validator.
	 *
	 * @param value	The value.
	 * @param <T>	The class type of the value.
	 * @throws DataException	If the value does not pass validation.
	 */
	private static <T> void validate(final T value, final Class<? extends Validator<?>> validator){
		final Validator<T> validatorCreator = (Validator<T>)ConstructorHelper.getEmptyCreator(validator)
			.get();
		if(!validatorCreator.isValid(value))
			throw DataException.create("Validation of {} didn't passed (value is {})", validator.getSimpleName(), value);
	}

}
