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

import io.github.mtrevisan.boxon.annotations.converters.Converter;
import io.github.mtrevisan.boxon.annotations.converters.NullConverter;
import io.github.mtrevisan.boxon.annotations.validators.NullValidator;
import io.github.mtrevisan.boxon.annotations.validators.Validator;
import io.github.mtrevisan.boxon.exceptions.CodecException;
import io.github.mtrevisan.boxon.helpers.GenericHelper;
import io.github.mtrevisan.boxon.helpers.JavaHelper;
import io.github.mtrevisan.boxon.helpers.StringHelper;

import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.function.Function;


/**
 * Handles casting operations between types, including conversions between primitive types and numeric objects such as {@link BigInteger}.
 */
public final class DataTypeCaster{

	private static final String METHOD_VALUE_OF = "valueOf";


	private DataTypeCaster(){}



	/**
	 * Retrieves the computed value based on the objective type.
	 *
	 * @param value	The input value to be processed.
	 * @param type	The class type from which to convert.
	 * @return	The computed value based on the objective type.
	 */
	private static Number value(final String value, final Class<?> type){
		return switch(type.getSimpleName()){
			case DataTypeHelper.PRIMITIVE_TYPE_NAME_BYTE, DataTypeHelper.OBJECTIVE_TYPE_NAME_BYTE -> Byte.valueOf(value);
			case DataTypeHelper.PRIMITIVE_TYPE_NAME_SHORT, DataTypeHelper.OBJECTIVE_TYPE_NAME_SHORT -> Short.valueOf(value);
			case DataTypeHelper.PRIMITIVE_TYPE_NAME_INT, DataTypeHelper.OBJECTIVE_TYPE_NAME_INTEGER -> Integer.valueOf(value);
			case DataTypeHelper.PRIMITIVE_TYPE_NAME_LONG, DataTypeHelper.OBJECTIVE_TYPE_NAME_LONG -> Long.valueOf(value);
			case DataTypeHelper.PRIMITIVE_TYPE_NAME_FLOAT, DataTypeHelper.OBJECTIVE_TYPE_NAME_FLOAT -> Float.valueOf(value);
			case DataTypeHelper.PRIMITIVE_TYPE_NAME_DOUBLE, DataTypeHelper.OBJECTIVE_TYPE_NAME_DOUBLE -> Double.valueOf(value);
			default -> null;
		};
	}

	/**
	 * Casts the given {@code value} to the specified {@code inputType}.
	 *
	 * @param value	The value to be cast.
	 * @param targetType	The target data type to cast the value to.
	 * @return	The cast value if successful, otherwise the original value.
	 */
	public static Number cast(final BigInteger value, final Class<?> targetType){
		return castFunction(targetType).apply(value);
	}

	public static Object cast(final Object array, final Class<?> targetType){
		final int length = Array.getLength(array);
		final Class<?> type = elementsType(array, length);
		final Function<BigInteger, Number> fun = (type != null? castFunction(targetType): null);
		final Object convertedArray = Array.newInstance(targetType, length);
		for(int i = 0; i < length; i ++){
			Object element = Array.get(array, i);
			if(element == null)
				continue;

			element = applyCast(element, fun, targetType);

			Array.set(convertedArray, i, element);
		}
		return convertedArray;
	}

	/**
	 * Returns the method that perform the cast to the specified {@code inputType}.
	 *
	 * @param targetType The target data type to cast the value to.
	 * @return The cast value if successful, otherwise the original value.
	 */
	private static Function<BigInteger, Number> castFunction(final Class<?> targetType){
		if(targetType == null)
			return (value -> value);

		return switch(targetType.getSimpleName()){
			case DataTypeHelper.PRIMITIVE_TYPE_NAME_BYTE, DataTypeHelper.OBJECTIVE_TYPE_NAME_BYTE -> Number::byteValue;
			case DataTypeHelper.PRIMITIVE_TYPE_NAME_SHORT, DataTypeHelper.OBJECTIVE_TYPE_NAME_SHORT -> Number::shortValue;
			case DataTypeHelper.PRIMITIVE_TYPE_NAME_INT, DataTypeHelper.OBJECTIVE_TYPE_NAME_INTEGER -> BigInteger::intValue;
			case DataTypeHelper.PRIMITIVE_TYPE_NAME_LONG, DataTypeHelper.OBJECTIVE_TYPE_NAME_LONG -> BigInteger::longValue;
			case DataTypeHelper.PRIMITIVE_TYPE_NAME_FLOAT, DataTypeHelper.OBJECTIVE_TYPE_NAME_FLOAT -> BigInteger::floatValue;
			case DataTypeHelper.PRIMITIVE_TYPE_NAME_DOUBLE, DataTypeHelper.OBJECTIVE_TYPE_NAME_DOUBLE -> BigInteger::doubleValue;
			default -> (value -> value);
		};
	}

	private static Class<?> elementsType(final Object array, final int length){
		for(int i = 0; i < length; i ++){
			final Object element = Array.get(array, i);

			if(element != null)
				return element.getClass();
		}
		return null;
	}

	private static Object applyCast(final Object element, final Function<BigInteger, Number> fun, final Class<?> targetType){
		return (fun != null
			? fun.apply((BigInteger)element)
			: cast((BigInteger)element, targetType));
	}


	/**
	 * Returns the primitive or objective type data stored as a string value, or the value itself if not a string or the field type is a
	 * string.
	 *
	 * @param fieldType	The type of the field that will hold the value represented as a string.
	 * @param value	The string value to be interpreted.
	 * @return	The primitive or objective value, or the value passed if not a string or the field type is a string.
	 * @throws CodecException	If the value cannot be interpreted as primitive or objective.
	 */
	public static Object getValueOrSelf(final Class<?> fieldType, final Object value) throws CodecException{
		if(fieldType == String.class || !(value instanceof final String valueAsString))
			return value;
		if(StringHelper.isBlank(valueAsString))
			return null;

		final Class<?> objectiveType = DataTypeMapper.toObjectiveTypeOrSelf(fieldType);
		return convertStringValue(valueAsString, objectiveType);
	}

	private static Object convertStringValue(final String value, final Class<?> objectiveType) throws CodecException{
		//try convert to a number...
		final Object valueAsNumber = toNumber(value, objectiveType);
		//... otherwise convert it to an object
		return (valueAsNumber == null
			? toObjectValue(value, objectiveType)
			: valueAsNumber
		);
	}

	private static Object toNumber(final String text, final Class<?> objectiveType){
		Object result = null;
		final BigInteger value = JavaHelper.convertToBigInteger(text);
		if(value != null){
			final int size = DataTypeHelper.getSize(objectiveType);
			if(size > 0 && value.bitCount() <= size)
				//convert value to `objectiveType` class
				result = cast(value, objectiveType);
		}
		return result;
	}

	private static Object toObjectValue(final String value, final Class<?> objectiveType) throws CodecException{
		Object result;
		if(BigDecimal.class.isAssignableFrom(objectiveType))
			result = new BigDecimal(value);
		else if(BigInteger.class.isAssignableFrom(objectiveType))
			result = JavaHelper.convertToBigInteger(value);
		else{
			result = value(value, objectiveType);
			if(result == null){
				//try with `.valueOf()`
				try{
					result = MethodHelper.invokeStaticMethod(objectiveType, METHOD_VALUE_OF, value);
				}
				catch(final Exception ignored){
					throw CodecException.create("Cannot interpret {} as {}", value, objectiveType.getSimpleName());
				}
			}
		}
		return result;
	}


	/**
	 * Resolves the input type for a sequence of a converter, if given, and/or a following validator.
	 *
	 * @param converterType	The type of the converter.
	 * @param validatorType	The type of the validator.
	 * @return	The resolved input type.
	 */
	public static Class<?> resolveInputType(final Class<? extends Converter<?, ?>> converterType,
			final Class<? extends Validator<?>> validatorType){
		Class<?> inputType = null;
		if(converterType != NullConverter.class)
			inputType = (Class<?>)GenericHelper.resolveGenericTypes(converterType)
				.getFirst();
		if(inputType == null && validatorType != NullValidator.class)
			inputType = (Class<?>)GenericHelper.resolveGenericTypes(validatorType)
				.getFirst();
		return inputType;
	}

	/**
	 * Reinterprets a `Number` as a `BigInteger`.
	 *
	 * @param value	The `Number` to reinterpret as a `BigInteger`.
	 * @return	A `BigInteger` representing the same numerical value as the given `Number`.
	 * @see #cast(BigInteger, Class)
	 */
	public static BigInteger reinterpretToBigInteger(final Number value){
		return (value instanceof final BigInteger bi
			? bi
			: BigInteger.valueOf(value.longValue())
		);
	}

}
