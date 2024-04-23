package io.github.mtrevisan.boxon.core.codecs.behaviors;

import io.github.mtrevisan.boxon.annotations.bindings.BindBitSet;
import io.github.mtrevisan.boxon.annotations.bindings.BindInteger;
import io.github.mtrevisan.boxon.annotations.bindings.BindString;
import io.github.mtrevisan.boxon.annotations.bindings.BindStringTerminated;
import io.github.mtrevisan.boxon.annotations.bindings.ByteOrder;
import io.github.mtrevisan.boxon.annotations.bindings.ConverterChoices;
import io.github.mtrevisan.boxon.annotations.converters.Converter;
import io.github.mtrevisan.boxon.annotations.validators.Validator;
import io.github.mtrevisan.boxon.core.codecs.CodecHelper;
import io.github.mtrevisan.boxon.exceptions.AnnotationException;
import io.github.mtrevisan.boxon.helpers.CharsetHelper;
import io.github.mtrevisan.boxon.helpers.Evaluator;

import java.lang.annotation.Annotation;
import java.nio.charset.Charset;


public class BehaviorBuilder{

	public static CommonBehavior of(final Annotation annotation, final Evaluator evaluator, final Object rootObject)
			throws AnnotationException{
		return switch(annotation){
			case BindBitSet bindBitSet -> ofBitSet(bindBitSet, evaluator, rootObject);
			case BindInteger bindInteger -> ofInteger(bindInteger, evaluator, rootObject);
			case BindString bindString -> ofString(bindString, evaluator, rootObject);
			case BindStringTerminated bindStringTerminated -> ofStringTerminated(bindStringTerminated);
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

	public static IntegerBehavior ofInteger(final BindInteger binding, final Evaluator evaluator, final Object rootObject)
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
