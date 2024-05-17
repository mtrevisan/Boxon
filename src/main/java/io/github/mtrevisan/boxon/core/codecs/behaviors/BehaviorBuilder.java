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

import io.github.mtrevisan.boxon.annotations.bindings.BindBitSet;
import io.github.mtrevisan.boxon.annotations.bindings.BindInteger;
import io.github.mtrevisan.boxon.annotations.bindings.BindString;
import io.github.mtrevisan.boxon.annotations.bindings.BindStringTerminated;
import io.github.mtrevisan.boxon.annotations.bindings.ByteOrder;
import io.github.mtrevisan.boxon.annotations.bindings.ConverterChoices;
import io.github.mtrevisan.boxon.annotations.converters.Converter;
import io.github.mtrevisan.boxon.annotations.validators.Validator;
import io.github.mtrevisan.boxon.core.helpers.CodecHelper;
import io.github.mtrevisan.boxon.exceptions.AnnotationException;
import io.github.mtrevisan.boxon.helpers.CharsetHelper;
import io.github.mtrevisan.boxon.io.Evaluator;

import java.lang.annotation.Annotation;
import java.nio.charset.Charset;


/**
 * The BehaviorBuilder class is responsible for creating instances of {@link CommonBehavior} based on the provided {@link Annotation}.
 * <p>
 * It provides static methods to handle different types of {@link Annotation}s and create the corresponding {@link CommonBehavior} objects.
 * </p>
 *
 * @see CommonBehavior
 */
public final class BehaviorBuilder{

	private BehaviorBuilder(){}


	/**
	 * Creates and returns a {@link CommonBehavior} instance based on the given {@link Annotation} and parameters.
	 *
	 * @param annotation	The {@link Annotation} to be used for creating the {@link CommonBehavior} instance.
	 * @param evaluator	The {@link Evaluator} instance to be used for evaluating the annotation.
	 * @param rootObject	The root object for which the {@link CommonBehavior} instance is created.
	 * @return	The {@link CommonBehavior} instance created based on the given parameters.
	 * @throws AnnotationException	If an exception occurs while creating the {@link CommonBehavior} instance.
	 */
	public static CommonBehavior of(final Annotation annotation, final Evaluator evaluator, final Object rootObject)
			throws AnnotationException{
		return switch(annotation){
			case final BindBitSet bindBitSet -> ofBitSet(bindBitSet, evaluator, rootObject);
			case final BindInteger bindInteger -> ofInteger(bindInteger, evaluator, rootObject);
			case final BindString bindString -> ofString(bindString, evaluator, rootObject);
			case final BindStringTerminated bindStringTerminated -> ofStringTerminated(bindStringTerminated);
			case null, default -> null;
		};
	}

	private static BitSetBehavior ofBitSet(final BindBitSet binding, final Evaluator evaluator, final Object rootObject)
			throws AnnotationException{
		final int size = CodecHelper.evaluateSize(binding.size(), evaluator, rootObject);
		final ConverterChoices converterChoices = binding.selectConverterFrom();
		final Class<? extends Converter<?, ?>> defaultConverter = binding.converter();
		final Class<? extends Validator<?>> validator = binding.validator();
		return new BitSetBehavior(size, converterChoices, defaultConverter, validator);
	}

	private static IntegerBehavior ofInteger(final BindInteger binding, final Evaluator evaluator, final Object rootObject)
			throws AnnotationException{
		final int size = CodecHelper.evaluateSize(binding.size(), evaluator, rootObject);
		final ByteOrder byteOrder = binding.byteOrder();
		final ConverterChoices converterChoices = binding.selectConverterFrom();
		final Class<? extends Converter<?, ?>> defaultConverter = binding.converter();
		final Class<? extends Validator<?>> validator = binding.validator();
		return new IntegerBehavior(size, byteOrder, converterChoices, defaultConverter, validator);
	}

	private static StringBehavior ofString(final BindString binding, final Evaluator evaluator, final Object rootObject)
			throws AnnotationException{
		final int size = CodecHelper.evaluateSize(binding.size(), evaluator, rootObject);
		final Charset charset = CharsetHelper.lookup(binding.charset());
		final ConverterChoices converterChoices = binding.selectConverterFrom();
		final Class<? extends Converter<?, ?>> defaultConverter = binding.converter();
		final Class<? extends Validator<?>> validator = binding.validator();
		return new StringBehavior(size, charset, converterChoices, defaultConverter, validator);
	}

	private static StringTerminatedBehavior ofStringTerminated(final BindStringTerminated binding){
		final byte terminator = binding.terminator();
		final boolean consumeTerminator = binding.consumeTerminator();
		final Charset charset = CharsetHelper.lookup(binding.charset());
		final ConverterChoices converterChoices = binding.selectConverterFrom();
		final Class<? extends Converter<?, ?>> defaultConverter = binding.converter();
		final Class<? extends Validator<?>> validator = binding.validator();
		return new StringTerminatedBehavior(terminator, consumeTerminator, charset, converterChoices, defaultConverter, validator);
	}

}
