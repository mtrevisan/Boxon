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
import io.github.mtrevisan.boxon.annotations.bindings.BindAsList;
import io.github.mtrevisan.boxon.annotations.converters.Converter;
import io.github.mtrevisan.boxon.annotations.validators.Validator;
import io.github.mtrevisan.boxon.core.codecs.behaviors.BehaviorBuilder;
import io.github.mtrevisan.boxon.core.codecs.behaviors.CommonBehavior;
import io.github.mtrevisan.boxon.core.codecs.behaviors.IntegerBehavior;
import io.github.mtrevisan.boxon.core.helpers.CodecHelper;
import io.github.mtrevisan.boxon.exceptions.AnnotationException;
import io.github.mtrevisan.boxon.helpers.JavaHelper;
import io.github.mtrevisan.boxon.io.BitReaderInterface;
import io.github.mtrevisan.boxon.io.BitWriterInterface;
import io.github.mtrevisan.boxon.io.Codec;

import java.lang.annotation.Annotation;
import java.lang.reflect.Array;


/**
 * Provides a default implementation of the Codec interface for encoding and decoding data based on annotations.
 * <p>
 * It leverages behaviors to read and write values, validate data, and convert between different types.
 * </p>
 * Manages {@link io.github.mtrevisan.boxon.annotations.bindings.BindBitSet BindBitSet},
 * {@link io.github.mtrevisan.boxon.annotations.bindings.BindInteger BindInteger},
 * {@link io.github.mtrevisan.boxon.annotations.bindings.BindString BindString}, and
 * {@link io.github.mtrevisan.boxon.annotations.bindings.BindStringTerminated BindStringTerminated}.
 */
final class CodecDefault implements Codec{

	/**
	 * Identifies the default codec.
	 */
	static class DefaultCodecIdentifier implements Annotation{
		@Override
		public final Class<? extends Annotation> annotationType(){
			return DefaultCodecIdentifier.class;
		}
	}



	@Override
	public Class<? extends Annotation> annotationType(){
		return DefaultCodecIdentifier.class;
	}

	@Override
	public Object decode(final BitReaderInterface reader, final Annotation annotation, final Annotation collectionBinding,
			final Object rootObject) throws AnnotationException{
		final CommonBehavior behavior = BehaviorBuilder.of(annotation, rootObject);
		if(behavior == null)
			throw AnnotationException.create("Cannot handle this type of annotation: {}, please report to the developer",
				JavaHelper.prettyPrintClassName(annotation.getClass()));

		Object instance;
		if(collectionBinding == null)
			instance = behavior.readValue(reader);
		else if(collectionBinding instanceof final BindAsArray superBinding){
			final int arraySize = CodecHelper.evaluateSize(superBinding.size(), rootObject);
			instance = behavior.readArrayWithoutAlternatives(reader, arraySize);
		}
		else{
			final String annotationType = JavaHelper.prettyPrintClassName(collectionBinding.annotationType());
			if(collectionBinding instanceof BindAsList)
				throw AnnotationException.create("Cannot handle this type of collection annotation: {}, use `@{}` instead",
					annotationType, BindAsArray.class.getSimpleName());
			else
				throw AnnotationException.create("Cannot handle this type of collection annotation: {}, please report to the developer",
					annotationType);
		}

		final Class<? extends Converter<?, ?>> chosenConverter = behavior.getChosenConverter(rootObject);
		if(behavior instanceof IntegerBehavior)
			instance = behavior.convertValueType(instance, chosenConverter, collectionBinding);
		final Object convertedValue = CodecHelper.converterDecode(chosenConverter, instance);

		final Class<? extends Validator<?>> validator = behavior.validator();
		CodecHelper.validate(convertedValue, validator);

		return convertedValue;
	}


	@Override
	public void encode(final BitWriterInterface writer, final Annotation annotation, final Annotation collectionBinding,
			final Object rootObject, final Object value) throws AnnotationException{
		final CommonBehavior behavior = BehaviorBuilder.of(annotation, rootObject);
		if(behavior == null)
			throw AnnotationException.create("Cannot handle this type of annotation: {}, please report to the developer",
				JavaHelper.prettyPrintClassName(annotation.getClass()));

		final Class<? extends Validator<?>> validator = behavior.validator();
		CodecHelper.validate(value, validator);

		final Class<? extends Converter<?, ?>> chosenConverter = behavior.getChosenConverter(rootObject);
		final Object convertedValue = CodecHelper.converterEncode(chosenConverter, value);

		if(collectionBinding == null)
			behavior.writeValue(writer, convertedValue);
		else if(collectionBinding instanceof final BindAsArray superBinding){
			final int arraySize = CodecHelper.evaluateSize(superBinding.size(), rootObject);
			CodecHelper.assertSizeEquals(arraySize, Array.getLength(convertedValue));

			behavior.writeArrayWithoutAlternatives(writer, convertedValue);
		}
		else{
			final String annotationType = JavaHelper.prettyPrintClassName(collectionBinding.annotationType());
			if(collectionBinding instanceof BindAsList)
				throw AnnotationException.create("Cannot handle this type of collection annotation: {}, use `@{}` instead",
					annotationType, BindAsArray.class.getSimpleName());
			else
				throw AnnotationException.create("Cannot handle this type of collection annotation: {}, please report to the developer",
					annotationType);
		}
	}

}
