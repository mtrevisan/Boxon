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
package io.github.mtrevisan.boxon.helpers;

import java.lang.reflect.Array;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


/**
 * A collection of convenience methods for simplifying Java capabilities.
 */
public final class JavaHelper{

	/** An empty {@code String}. */
	public static final String EMPTY_STRING = "";

	public static final String ARRAY_VARIABLE = "[]";
	private static final Map<Character, String> DESCRIPTOR_MAP = Map.of(
		'B', "byte",
		'C', "char",
		'D', "double",
		'F', "float",
		'I', "int",
		'J', "long",
		'S', "short",
		'Z', "boolean",
		'L', "class"
	);

	private static final String HEXADECIMAL_PREFIX = "0x";


	private JavaHelper(){}


	/**
	 * Return the given object if non-null, the default object otherwise.
	 *
	 * @param obj	The object.
	 * @param defaultObject	The default object to be returned if the given object is {@code null}.
	 * @param <T>	The class type of the object.
	 * @return	The object, or the default object if {@code null}.
	 */
	public static <T> T nonNullOrDefault(final T obj, final T defaultObject){
		return (obj != null? obj: defaultObject);
	}


	/**
	 * Calculates the size in bytes for a given number of bits.
	 *
	 * @param bits	The number of bits.
	 * @return	The size in bytes.
	 */
	public static int getSizeInBytes(final int bits){
		return (bits + Byte.SIZE - 1) >>> 3;
	}

	/**
	 * Return the length of the text, or {@code 0} if {@code null}.
	 *
	 * @param text	The text.
	 * @return	The length of the text, or {@code 0} if {@code null}.
	 */
	public static int sizeOrZero(final CharSequence text){
		return (text != null? text.length(): 0);
	}

	/**
	 * Return the length of the array, or {@code 0} if {@code null}.
	 *
	 * @param array	The array.
	 * @return	The length of the array, or {@code 0} if {@code null}.
	 */
	static int sizeOrZero(final byte[] array){
		return (array != null? array.length: 0);
	}

	/**
	 * Return the length of the array, or {@code 0} if {@code null}.
	 *
	 * @param array	The array.
	 * @return	The length of the array, or {@code 0} if {@code null}.
	 */
	public static int sizeOrZero(final Object array){
		return (array != null? Array.getLength(array): 0);
	}

	/**
	 * Return the length of the list, or {@code 0} if {@code null}.
	 *
	 * @param array	The list.
	 * @param <T>	The class type of the list.
	 * @return	The length of the list, or {@code 0} if {@code null}.
	 */
	public static <T> int sizeOrZero(final Collection<T> array){
		return (array != null? array.size(): 0);
	}


	/**
	 * Checks if the given number is a multiple of a byte (8 bits).
	 *
	 * @param number	The number to check if it is a multiple of a byte.
	 * @return	Whether the number is a multiple of a byte.
	 */
	public static boolean isMultipleOfByte(final int number){
		return (number != 0 && number % Byte.SIZE == 0);
	}


	/**
	 * Checks if the given text is an integer number, either in decimal or hexadecimal format.
	 *
	 * @param text	The text to check, may be {@code null}.
	 * @return	Whether the text is an integer number.
	 */
	public static boolean isIntegerNumber(final String text){
		return (isDecimalIntegerNumber(text) || isHexadecimalNumber(text));
	}

	/**
	 * <p>Checks if the text contains only Unicode digits.
	 * A decimal point is not a Unicode digit and returns false.</p>
	 *
	 * <p>{@code null} will return {@code false}.
	 * An empty text ({@code length() = 0}) will return {@code false}.</p>
	 *
	 * <p>Note that the method does not allow for a leading sign, either positive or negative.
	 * Also, if a String passes the numeric test, it may still generate a NumberFormatException
	 * when parsed by Integer.parseInt or Long.parseLong, e.g., if the value is outside the range
	 * for int or long respectively.</p>
	 *
	 * <pre>
	 * isNumeric(null)   = false
	 * isNumeric("")     = false
	 * isNumeric("  ")   = false
	 * isNumeric("123")  = true
	 * isNumeric("\u0967\u0968\u0969") = true
	 * isNumeric("12 3") = false
	 * isNumeric("ab2c") = false
	 * isNumeric("12-3") = false
	 * isNumeric("12.3") = false
	 * isNumeric("-123") = false
	 * isNumeric("+123") = false
	 * </pre>
	 *
	 * @param text	The text to check, may be {@code null}.
	 * @return	Whether the given text contains only digits and is non-{@code null}.
	 */
	public static boolean isDecimalIntegerNumber(final String text){
		return isDecimalIntegerNumber(text, 0, 10);
	}

	private static boolean isDecimalIntegerNumber(final String text, final int offset, final int radix){
		final byte[] bytes = text.getBytes(StandardCharsets.US_ASCII);
		for(int i = offset, length = bytes.length; i < length; i ++){
			final byte chr = bytes[i];

			if(Character.digit(chr, radix) < 0)
				return false;
		}
		return true;
	}

	private static boolean isHexadecimalNumber(final String text){
		return (text.startsWith(HEXADECIMAL_PREFIX) && isDecimalIntegerNumber(text, 2, 16));
	}

	/**
	 * Convert a string value (decimal or hexadecimal) to {@link BigInteger}.
	 *
	 * @param value	The value.
	 * @return	The converted {@link BigInteger}.
	 */
	public static BigInteger convertToBigInteger(final String value){
		try{
			if(isHexadecimalNumber(value))
				return new BigInteger(value.substring(2), 16);

			return new BigInteger(value);
		}
		catch(final NumberFormatException ignored){
			return null;
		}
	}

	/**
	 * Convert a string value (decimal or hexadecimal if it's an integer) to {@link BigDecimal}.
	 *
	 * @param value	The value.
	 * @return	The converted {@link BigDecimal}.
	 */
	public static BigDecimal convertToBigDecimal(final String value){
		try{
			if(isHexadecimalNumber(value))
				return new BigDecimal(new BigInteger(value.substring(2), 16));

			return new BigDecimal(value);
		}
		catch(final NumberFormatException ignored){
			return null;
		}
	}


	/**
	 * Pretty prints the name of the given method.
	 * <p>
	 * The format is "ClassName.methodName(parameterTypes)".
	 * </p>
	 *
	 * @param type	The class type containing the method.
	 * @param methodName	The name of the method.
	 * @param parameterTypes	The parameter types of the method.
	 * @return	The pretty printed method name.
	 */
	public static String prettyPrintMethodName(final Class<?> type, final String methodName, final Class<?>... parameterTypes){
		return type.getName() + '.' + methodName
			+ (parameterTypes == null || parameterTypes.length == 0
			? "()"
			: Arrays.stream(parameterTypes).map(c -> c == null
			? "null"
			: c.getName()).collect(Collectors.joining(",", "(", ")"))
		);
	}

	/**
	 * Pretty prints the name of the given class.
	 * <p>
	 * If the class represents an array, the brackets are properly formatted.
	 * </p>
	 *
	 * @param cls	The class type.
	 * @return	The pretty printed class name.
	 */
	public static String prettyPrintClassName(final Class<?> cls){
		final String className = cls.getName();
		final int count = countLeadingSquareBrackets(className);
		if(count > 0){
			final StringBuilder sb = new StringBuilder(className);
			//remove semicolon
			sb.deleteCharAt(sb.length() - 1);
			sb.delete(0, count + 1);
			sb.append(ARRAY_VARIABLE.repeat(count));
			return sb.toString();
		}
		return className;
	}

	/**
	 * Counts the number of leading square brackets in a given text.
	 * <p>See {@link Class#getName()}.</p>
	 *
	 * @param text	The input text.
	 * @return	The count of leading square brackets.
	 */
	private static int countLeadingSquareBrackets(final String text){
		int count = 0;
		final char[] array = text.toCharArray();
		for(int i = 0, length = array.length; i < length; i ++){
			final char chr = array[i];

			if(chr == '['){
				count ++;
				continue;
			}
			//NOTE: 'L' is the descriptor of a class
			if(chr != 'L')
				count = 0;
			break;
		}
		return count;
	}

	/**
	 * Pretty prints the name of the given variable.
	 * <p>
	 * If the class represents an array, the brackets are properly formatted.
	 * </p>
	 *
	 * @param cls	The variable type.
	 * @return	The pretty printed variable name.
	 */
	public static String prettyPrintVariableName(final Class<?> cls){
		if(isClassOrRecord(cls))
			return prettyPrintClassName(cls);

		final String variableName = cls.getName();
		final int length = variableName.length();
		final String descriptor = DESCRIPTOR_MAP.get(variableName.charAt(length - 1));
		final String array = ARRAY_VARIABLE.repeat(length - 1);
		return descriptor + array;
	}

	/**
	 * Check if the class is not an interface, an anonymous class, or a primitive data type.
	 *
	 * @param cls	The class.
	 * @return	Whether the class is not an interface, an anonymous class, or a primitive data type.
	 */
	public static boolean isUserDefinedClass(final Class<?> cls){
		return (!cls.isInterface() && !cls.isAnonymousClass() && !cls.isPrimitive());
	}

	private static boolean isClassOrRecord(final Class<?> type){
		return (type.isRecord() || !type.isInterface() && !Modifier.isAbstract(type.getModifiers()));
	}


	public static String commonPrefix(final Class<?>... strings){
		final int length = sizeOrZero(strings);
		String prefix = (length > 0? strings[0].getSimpleName(): EMPTY_STRING);
		for(int i = 1; !prefix.isEmpty() && i < length; i ++)
			prefix = findCommonPrefix(prefix, strings[i].getSimpleName());
		return prefix;
	}

	private static String findCommonPrefix(final String str1, final String str2){
		final int minLength = Math.min(str1.length(), str2.length());
		int index = 0;
		while(index < minLength && str1.charAt(index) == str2.charAt(index))
			index ++;
		return str1.substring(0, index);
	}



	public static <V> List<V> createListOrEmpty(final int initialCapacity){
		return (initialCapacity > 0? new ArrayList<>(initialCapacity): Collections.emptyList());
	}

	public static <K, V> Map<K, V> createMapOrEmpty(final int initialCapacity){
		return (initialCapacity > 0? new HashMap<>(initialCapacity): Collections.emptyMap());
	}

	public static <K, V> Map<V, K> reverseMap(final Map<K, V> map){
		final Map<V, K> reverseMap = new HashMap<>(map.size());
		for(final Map.Entry<K, V> entry : map.entrySet())
			reverseMap.put(entry.getValue(), entry.getKey());
		return reverseMap;
	}

}
