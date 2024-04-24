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

import io.github.mtrevisan.boxon.annotations.bindings.BindAsArray;
import io.github.mtrevisan.boxon.annotations.converters.Converter;
import io.github.mtrevisan.boxon.annotations.validators.Validator;
import io.github.mtrevisan.boxon.core.codecs.behaviors.BehaviorBuilder;
import io.github.mtrevisan.boxon.core.codecs.behaviors.CommonBehavior;
import io.github.mtrevisan.boxon.core.codecs.behaviors.IntegerBehavior;
import io.github.mtrevisan.boxon.core.helpers.CodecHelper;
import io.github.mtrevisan.boxon.exceptions.AnnotationException;
import io.github.mtrevisan.boxon.helpers.Evaluator;
import io.github.mtrevisan.boxon.helpers.Injected;
import io.github.mtrevisan.boxon.io.BitReaderInterface;
import io.github.mtrevisan.boxon.io.BitWriterInterface;
import io.github.mtrevisan.boxon.io.CodecInterface;

import java.lang.annotation.Annotation;
import java.lang.reflect.Array;


/**
 * Manages `BindBitSet`, `BindInteger`, `BindString`, and `BindStringTerminated`.
 */
final class CodecDefault implements CodecInterface{

	/**
	 * Identifies the default codec.
	 */
	static class DefaultCodecIdentifier{}


	@Injected
	private Evaluator evaluator;


	@Override
	public Class<?> identifier(){
		return DefaultCodecIdentifier.class;
	}

	@Override
	public Object decode(final BitReaderInterface reader, final Annotation annotation, final Annotation collectionBinding,
			final Object rootObject) throws AnnotationException{
		final CommonBehavior behavior = BehaviorBuilder.of(annotation, evaluator, rootObject);

		Object instance = null;
		if(collectionBinding == null)
			instance = behavior.readValue(reader);
		else if(collectionBinding instanceof final BindAsArray superBinding){
			final int arraySize = CodecHelper.evaluateSize(superBinding.size(), evaluator, rootObject);
			instance = behavior.readArrayWithoutAlternatives(reader, arraySize);
		}

		final Class<? extends Converter<?, ?>> chosenConverter = behavior.getChosenConverter(evaluator, rootObject);
		final Object convertedValue;
		if(behavior instanceof IntegerBehavior){
			final Object convertedValueType = behavior.convertValueType(instance, chosenConverter, collectionBinding);
			convertedValue = CodecHelper.converterDecode(chosenConverter, convertedValueType);
		}
		else
			convertedValue = CodecHelper.converterDecode(chosenConverter, instance);

		final Class<? extends Validator<?>> validator = behavior.validator();
		CodecHelper.validate(convertedValue, validator);

		return convertedValue;
	}


	@Override
	public void encode(final BitWriterInterface writer, final Annotation annotation, final Annotation collectionBinding,
			final Object rootObject, final Object value) throws AnnotationException{
		final CommonBehavior behavior = BehaviorBuilder.of(annotation, evaluator, rootObject);

		final Class<? extends Validator<?>> validator = behavior.validator();
		CodecHelper.validate(value, validator);

		final Class<? extends Converter<?, ?>> chosenConverter = behavior.getChosenConverter(evaluator, rootObject);
		final Object convertedValue = CodecHelper.converterEncode(chosenConverter, value);

		if(collectionBinding == null)
			behavior.writeValue(writer, convertedValue);
		else if(collectionBinding instanceof final BindAsArray superBinding){
			final int arraySize = CodecHelper.evaluateSize(superBinding.size(), evaluator, rootObject);
			CodecHelper.assertSizeEquals(arraySize, Array.getLength(convertedValue));

			behavior.writeArrayWithoutAlternatives(writer, convertedValue);
		}
	}

}
