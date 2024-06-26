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
package io.github.mtrevisan.boxon.core.helpers;

import io.github.mtrevisan.boxon.annotations.bindings.ByteOrder;
import io.github.mtrevisan.boxon.annotations.bindings.ConverterChoices;
import io.github.mtrevisan.boxon.annotations.bindings.ObjectChoices;
import io.github.mtrevisan.boxon.annotations.configurations.ConfigurationEnum;
import io.github.mtrevisan.boxon.annotations.converters.Converter;
import io.github.mtrevisan.boxon.annotations.validators.Validator;
import io.github.mtrevisan.boxon.exceptions.AnnotationException;
import io.github.mtrevisan.boxon.exceptions.CodecException;
import io.github.mtrevisan.boxon.exceptions.DataException;
import io.github.mtrevisan.boxon.helpers.ContextHelper;
import io.github.mtrevisan.boxon.io.BitReaderInterface;
import io.github.mtrevisan.boxon.io.BitWriterInterface;
import io.github.mtrevisan.boxon.io.Evaluator;
import org.springframework.expression.EvaluationException;

import java.lang.reflect.Array;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;


/**
 * A collection of convenience methods for working with codecs.
 */
public final class CodecHelper{

	private CodecHelper(){}


	/**
	 * Validate the value passed using the configured validator.
	 *
	 * @param value	The value.
	 * @param <T>	The class type of the value.
	 * @throws DataException	If the value does not pass validation.
	 */
	public static <T> void validate(final T value, final Class<? extends Validator<?>> validator){
		final Validator<T> validatorCreator = (Validator<T>)ConstructorHelper.getEmptyCreator(validator)
			.get();
		if(!validatorCreator.isValid(value))
			throw DataException.create("Validation of {} didn't passed (value is {})", validator.getSimpleName(), value);
	}


	/**
	 * Convenience method to fast evaluate a positive integer.
	 *
	 * @param size	The size to be evaluated.
	 * @param evaluator	The evaluator.
	 * @param rootObject	Root object for the evaluator.
	 * @return	The size, or a negative number if the expression is not a valid positive integer.
	 * @throws EvaluationException	If an error occurs during the evaluation of an expression.
	 */
	public static int evaluateSize(final String size, final Evaluator evaluator, final Object rootObject) throws AnnotationException{
		final int evaluatedSize = evaluator.evaluateSize(size, rootObject);
		if(evaluatedSize < 0)
			throw AnnotationException.create("Size must be a non-negative integer, was {}", size);

		return evaluatedSize;
	}

	/**
	 * Asserts that the expected size matches the given size.
	 *
	 * @param expectedSize	The expected size.
	 * @param size	The actual size.
	 * @throws DataException	If the sizes do not match.
	 */
	public static void assertSizeEquals(final int expectedSize, final int size){
		if(expectedSize != size)
			throw DataException.create("Size mismatch, expected {}, got {}", expectedSize, size);
	}


	/**
	 * Creates a new array of the specified type and length.
	 *
	 * @param type	The class type of the array elements.
	 * @param length	The length of the array.
	 * @return	A new array of the specified type and length.
	 */
	public static Object createArray(final Class<?> type, final int length){
		return Array.newInstance(type, length);
	}

	/**
	 * Creates a new list of the specified type.
	 *
	 * @param type	The class type of the list elements.
	 * @param <T>	The type of the list elements.
	 * @return	A new list of the specified type.
	 * @throws AnnotationException	If the type is not a primitive type.
	 */
	public static <T> List<T> createList(final Class<? extends T> type) throws AnnotationException{
		if(ParserDataType.isPrimitive(type))
			throw AnnotationException.createNotPrimitiveValue(type);

		return new ArrayList<>(0);
	}


	/**
	 * Get the first converter that matches the condition.
	 *
	 * @param converterChoices	The converter choices annotation.
	 * @param defaultConverter	The default converter.
	 * @param evaluator	The evaluator.
	 * @param rootObject	Root object for the evaluator.
	 * @return	The converter class.
	 */
	public static Class<? extends Converter<?, ?>> getChosenConverter(final ConverterChoices converterChoices,
			final Class<? extends Converter<?, ?>> defaultConverter, final Evaluator evaluator, final Object rootObject){
		final ConverterChoices.ConverterChoice[] alternatives = converterChoices.alternatives();
		for(int i = 0, length = alternatives.length; i < length; i ++){
			final ConverterChoices.ConverterChoice alternative = alternatives[i];

			if(evaluator.evaluateBoolean(alternative.condition(), rootObject))
				return alternative.converter();
		}
		return defaultConverter;
	}


	/**
	 * Whether the select-object-from binding has any alternatives.
	 *
	 * @return	Whether the select-object-from binding has any alternatives.
	 */
	public static <T> boolean hasSelectAlternatives(final T[] alternatives){
		return (alternatives.length > 0);
	}

	/**
	 * Chooses the alternative type based on the given alternatives, default alternative, evaluator, and root object.
	 * <p>
	 * It evaluates the condition of each alternative using the evaluator with the root object and returns the type of the first alternative
	 * that evaluates to true.
	 * </p>
	 *
	 * @param alternatives	An array of ObjectChoices.ObjectChoice representing the alternatives.
	 * @param defaultAlternative	The default alternative type.
	 * @param evaluator	An instance of the Evaluator interface for evaluating the condition.
	 * @param rootObject	The root object for the evaluator.
	 * @return	The chosen alternative type. If none of the alternatives evaluate to {@code true}, it returns the default alternative type.
	 */
	public static Class<?> chooseAlternativeType(final ObjectChoices.ObjectChoice[] alternatives, final Class<?> defaultAlternative,
			final Evaluator evaluator, final Object rootObject){
		Class<?> chosenAlternativeType = defaultAlternative;
		for(int i = 0, length = alternatives.length; chosenAlternativeType == defaultAlternative && i < length; i ++){
			final ObjectChoices.ObjectChoice alternative = alternatives[i];

			final String condition = alternative.condition();
			if(evaluator.evaluateBoolean(condition, rootObject))
				chosenAlternativeType = alternative.type();
		}
		return chosenAlternativeType;
	}

	/**
	 * Chooses the alternative from an array of {@link ObjectChoices.ObjectChoice} based on the given type.
	 *
	 * @param alternatives	An array of {@link ObjectChoices.ObjectChoice} representing the alternatives.
	 * @param type	The type to match against the alternatives.
	 * @return	The matching {@link ObjectChoices.ObjectChoice} if found.
	 * @throws CodecException	If no matching alternative is found.
	 */
	public static ObjectChoices.ObjectChoice chooseAlternative(final ObjectChoices.ObjectChoice[] alternatives, final Class<?> type)
			throws CodecException{
		for(int i = 0, length = alternatives.length; i < length; i ++){
			final ObjectChoices.ObjectChoice alternative = alternatives[i];

			if(alternative.type().isAssignableFrom(type))
				return alternative;
		}

		throw CodecException.create("Cannot find a valid codec for type {}", type.getSimpleName());
	}

	/**
	 * Gets the alternative class type that parses the next data.
	 *
	 * @param reader	The reader from which to read the data from.
	 * @param bindingType	Bind annotation type.
	 * @param objectChoices	Choices annotation.
	 * @param defaultAlternativeType	Default alternative type.
	 * @param evaluator	The evaluator.
	 * @param rootObject	Root object for the evaluator.
	 * @return	The class type of the chosen alternative.
	 * @throws CodecException	If a codec cannot be found for the chosen alternative.
	 */
	public static Class<?> chooseAlternativeType(final BitReaderInterface reader, final Class<?> bindingType,
			final ObjectChoices objectChoices, final Class<?> defaultAlternativeType, final Evaluator evaluator, final Object rootObject)
			throws CodecException{
		final ObjectChoices.ObjectChoice[] alternatives = objectChoices.alternatives();
		if(!hasSelectAlternatives(alternatives))
			return bindingType;

		addPrefixToContext(reader, objectChoices, evaluator);

		final Class<?> chosenAlternativeType = chooseAlternativeType(alternatives, defaultAlternativeType, evaluator, rootObject);
		if(chosenAlternativeType != void.class)
			return chosenAlternativeType;

		throw CodecException.createNoCodecForAlternatives(rootObject.getClass());
	}


	/**
	 * Writes the header based on the chosen alternative, selectFrom choices, evaluator, and root object.
	 * <p>
	 * The method checks if the chosen alternative condition contains the '{@code #prefix}' token. If it does, it writes the prefix
	 * value to the writer. The prefix value is evaluated using the evaluator with the root object, and the size of the prefix
	 * is determined by {@code selectFrom.prefixLength()} in bits.
	 * </p>
	 *
	 * @param writer	The {@link BitWriterInterface} object to write the header to.
	 * @param chosenAlternative	The {@link ObjectChoices.ObjectChoice} representing the chosen alternative.
	 * @param selectFrom	The {@link ObjectChoices} representing the choices to select from.
	 * @param evaluator	The {@link Evaluator} interface for evaluating expressions.
	 * @param rootObject	The root object for the evaluator.
	 */
	public static void writeHeader(final BitWriterInterface writer, final ObjectChoices.ObjectChoice chosenAlternative,
			final ObjectChoices selectFrom, final Evaluator evaluator, final Object rootObject){
		//if `chosenAlternative.condition()` contains '#prefix', then write `@ObjectChoice.prefix()`
		if(ContextHelper.containsHeaderReference(chosenAlternative.condition())){
			final byte prefixSize = selectFrom.prefixLength();

			final int prefix = evaluator.evaluateSize(chosenAlternative.prefix(), rootObject);
			if(prefixSize == Byte.SIZE)
				writer.writeByte((byte)prefix);
			else{
				final BitSet bitmap = BitSetHelper.createBitSet(prefixSize, prefix);

				writer.writeBitSet(bitmap, prefixSize);
			}
		}
	}

	/**
	 * Add the prefix to the evaluator context if needed.
	 *
	 * @param reader	The reader from which to read the prefix.
	 */
	private static void addPrefixToContext(final BitReaderInterface reader, final ObjectChoices objectChoices, final Evaluator evaluator){
		final byte prefixSize = objectChoices.prefixLength();
		if(prefixSize > 0){
			final BitSet bitmap = reader.readBitSet(prefixSize);
			final ByteOrder byteOrder = objectChoices.byteOrder();
			final BigInteger prefix = BitSetHelper.toObjectiveType(bitmap, prefixSize, byteOrder);

			evaluator.putToContext(ContextHelper.CONTEXT_CHOICE_PREFIX, prefix);
		}
	}


	/**
	 * Decodes data using the specified converter type.
	 *
	 * @param <IN>	The type of the input data.
	 * @param <OUT>	The type of the decoded data.
	 * @param converterType	The class type of the converter.
	 * @param data	The input data to be decoded.
	 * @return	The decoded data.
	 * @throws DataException	If an error occurs during decoding.
	 */
	public static <IN, OUT> OUT converterDecode(final Class<? extends Converter<?, ?>> converterType, final IN data){
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
	 * Encodes the given data using the specified converter type.
	 *
	 * @param <IN>	The type of the input data.
	 * @param <OUT>	The type of the encoded data.
	 * @param converterType	The class type of the converter.
	 * @param data	The data to be encoded.
	 * @return	The encoded data.
	 * @throws DataException	If an error occurs during encoding.
	 */
	public static <IN, OUT> IN converterEncode(final Class<? extends Converter<?, ?>> converterType, final OUT data){
		try{
			final Converter<IN, OUT> converter = (Converter<IN, OUT>)ConstructorHelper.getEmptyCreator(converterType)
				.get();
			return converter.encode(data);
		}
		catch(final Exception e){
			throw DataException.create("Can not input {} ({}) to encode method of converter {}",
				data.getClass().getSimpleName(), data, converterType.getSimpleName(), e);
		}
	}

	/**
	 * Interprets the given value based on the specified field type.
	 *
	 * @param value	The value to be interpreted.
	 * @param fieldType	The class type of the field.
	 * @return	The interpreted value.
	 * @throws CodecException	If an error occurs during the interpretation.
	 */
	public static Object interpretValue(Object value, final Class<?> fieldType) throws CodecException{
		value = ParserDataType.getValueOrSelf(fieldType, value);
		if(value != null){
			if(value instanceof final ConfigurationEnum v)
				value = v.getCode();
			else if(value.getClass().isArray())
				value = calculateCompositeValue(value);
		}
		return value;
	}

	private static int calculateCompositeValue(final Object value){
		int compositeEnumValue = 0;
		for(int i = 0, length = Array.getLength(value); i < length; i ++)
			compositeEnumValue |= ((ConfigurationEnum)Array.get(value, i)).getCode();
		return compositeEnumValue;
	}

}
