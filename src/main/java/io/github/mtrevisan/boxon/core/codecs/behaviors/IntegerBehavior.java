package io.github.mtrevisan.boxon.core.codecs.behaviors;

import io.github.mtrevisan.boxon.annotations.bindings.BindAsArray;
import io.github.mtrevisan.boxon.annotations.bindings.BindInteger;
import io.github.mtrevisan.boxon.annotations.bindings.ByteOrder;
import io.github.mtrevisan.boxon.annotations.bindings.ConverterChoices;
import io.github.mtrevisan.boxon.annotations.converters.Converter;
import io.github.mtrevisan.boxon.annotations.validators.Validator;
import io.github.mtrevisan.boxon.core.codecs.CodecHelper;
import io.github.mtrevisan.boxon.exceptions.AnnotationException;
import io.github.mtrevisan.boxon.helpers.Evaluator;
import io.github.mtrevisan.boxon.io.BitReaderInterface;
import io.github.mtrevisan.boxon.io.BitSetHelper;
import io.github.mtrevisan.boxon.io.BitWriterInterface;
import io.github.mtrevisan.boxon.io.ParserDataType;

import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.math.BigInteger;
import java.util.BitSet;


public final class IntegerBehavior extends BitSetBehavior{

	private final ByteOrder byteOrder;


	public static IntegerBehavior of(final Annotation annotation, final Evaluator evaluator, final Object rootObject)
			throws AnnotationException{
		final BindInteger binding = (BindInteger)annotation;

		final int size = CodecHelper.evaluateSize(binding.size(), evaluator, rootObject);
		final ByteOrder byteOrder = binding.byteOrder();
		final ConverterChoices converterChoices = binding.selectConverterFrom();
		final Class<? extends Converter<?, ?>> defaultConverter = binding.converter();
		final Class<? extends Validator<?>> validator = binding.validator();
		return new IntegerBehavior(size, byteOrder, converterChoices, defaultConverter, validator);
	}


	IntegerBehavior(final int size, final ByteOrder byteOrder, final ConverterChoices converterChoices,
			final Class<? extends Converter<?, ?>> defaultConverter, final Class<? extends Validator<?>> validator){
		super(size, converterChoices, defaultConverter, validator);;

		this.byteOrder = byteOrder;
	}


	public ByteOrder byteOrder(){
		return byteOrder;
	}


	public static Object convertValueType(final Annotation collectionBinding, final Class<? extends Converter<?, ?>> converterType,
			final Class<? extends Validator<?>> validator, Object instance){
		//convert value type into converter/validator input type
		Class<?> inputType = ParserDataType.resolveInputType(converterType, validator);
		if(collectionBinding == null)
			instance = ParserDataType.castValue((BigInteger)instance, inputType);
		else if(collectionBinding instanceof BindAsArray && inputType != null){
			inputType = inputType.getComponentType();
			if(inputType != instance.getClass().getComponentType()){
				final int length = Array.getLength(instance);
				final Object array = CodecHelper.createArray(inputType, length);

				for(int i = 0; i < length; i++){
					Object element = Array.get(instance, i);
					element = ParserDataType.castValue((BigInteger)element, inputType);
					Array.set(array, i, element);
				}

				instance = array;
			}
		}
		return instance;
	}


	@Override
	public Object createArray(final int arraySize){
		return CodecHelper.createArray(BigInteger.class, arraySize);
	}

	@Override
	public Object readValue(final BitReaderInterface reader){
		return reader.getBigInteger(size, byteOrder);
	}

	@Override
	public void writeValue(final BitWriterInterface writer, final Object value){
		final BigInteger v = ParserDataType.reinterpretToBigInteger((Number)value);
		final BitSet bitmap = BitSetHelper.createBitSet(size, v, byteOrder);

		writer.putBitSet(bitmap, size);
	}

}
