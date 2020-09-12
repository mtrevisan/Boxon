/**
 * Copyright (c) 2020 Mauro Trevisan
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
package io.github.mtrevisan.boxon.codecs;

import io.github.mtrevisan.boxon.annotations.bindings.ConverterChoices;
import io.github.mtrevisan.boxon.annotations.bindings.ObjectChoices;
import io.github.mtrevisan.boxon.annotations.converters.Converter;
import io.github.mtrevisan.boxon.annotations.validators.Validator;
import io.github.mtrevisan.boxon.exceptions.AnnotationException;
import io.github.mtrevisan.boxon.external.BitReader;
import io.github.mtrevisan.boxon.external.BitSet;
import io.github.mtrevisan.boxon.external.BitWriter;
import io.github.mtrevisan.boxon.external.ByteOrder;
import io.github.mtrevisan.boxon.internal.ReflectionHelper;

import java.nio.charset.Charset;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


final class CodecHelper{

	/** The name of the current object being scanner (used for referencing variables from SpEL). */
	static final String CONTEXT_SELF = "self";
	/** The name of the prefix for the alternative (used for referencing variables from SpEL). */
	private static final String CONTEXT_CHOICE_PREFIX = "prefix";

	private static final Matcher CONTEXT_PREFIXED_CHOICE_PREFIX = Pattern.compile("#" + CONTEXT_CHOICE_PREFIX + "[^a-zA-Z]")
		.matcher("");


	private CodecHelper(){}

	static void assertSizePositive(final int size) throws AnnotationException{
		if(size <= 0)
			throw new AnnotationException("Size must be a positive integer, was {}", size);
	}

	static void assertSizeEquals(final int expectedSize, final int size){
		if(expectedSize != size)
			throw new IllegalArgumentException("Size mismatch, expected " + expectedSize + ", got " + size);
	}

	static void assertCharset(final String charsetName) throws AnnotationException{
		try{
			Charset.forName(charsetName);
		}
		catch(final IllegalArgumentException ignored){
			throw new AnnotationException("Invalid charset: '{}'", charsetName);
		}
	}

	static ObjectChoices.ObjectChoice chooseAlternative(final ObjectChoices.ObjectChoice[] alternatives, final Class<?> type){
		for(final ObjectChoices.ObjectChoice alternative : alternatives)
			if(alternative.type().isAssignableFrom(type))
				return alternative;

		throw new IllegalArgumentException("Cannot find a valid codec for type " + type.getSimpleName());
	}

	static ObjectChoices.ObjectChoice chooseAlternative(final BitReader reader, final ObjectChoices selectFrom, final Object rootObject){
		if(selectFrom.prefixSize() > 0){
			final int prefixSize = selectFrom.prefixSize();
			final ByteOrder prefixByteOrder = selectFrom.byteOrder();
			final int prefix = reader.getInteger(prefixSize, prefixByteOrder)
				.intValue();

			Evaluator.addToContext(CONTEXT_CHOICE_PREFIX, prefix);
		}

		final ObjectChoices.ObjectChoice[] alternatives = selectFrom.alternatives();
		return chooseAlternative(alternatives, rootObject);
	}

	private static ObjectChoices.ObjectChoice chooseAlternative(final ObjectChoices.ObjectChoice[] alternatives,
			final Object rootObject){
		for(final ObjectChoices.ObjectChoice alternative : alternatives)
			if(Evaluator.evaluate(alternative.condition(), rootObject, boolean.class))
				return alternative;
		return null;
	}

	static Class<? extends Converter<?, ?>> chooseConverter(final ConverterChoices selectConverterFrom,
			final Class<? extends Converter<?, ?>> defaultConverter, final Object rootObject){
		final ConverterChoices.ConverterChoice[] alternatives = selectConverterFrom.alternatives();
		for(final ConverterChoices.ConverterChoice alternative : alternatives)
			if(Evaluator.evaluate(alternative.condition(), rootObject, boolean.class))
				return alternative.converter();
		return defaultConverter;
	}

	static Class<?> inferBindingType(final ConverterChoices selectConverterFrom,
		final Class<? extends Converter<?, ?>> defaultConverter, final Class<?> fieldType){
		//get input type from `variable`
		Class<?> type = fieldType;

		//get input type from `converter`
		final ConverterChoices.ConverterChoice[] alternatives = selectConverterFrom.alternatives();
		if(alternatives.length > 0){
			//infer supertype of all types accepted by the converters
			final Set<Class<?>> supertypes = new HashSet<>();
			for(final ConverterChoices.ConverterChoice alternative : alternatives){
				final Class<?> converterType = ReflectionHelper.resolveGenericTypes(alternative.converter(), Converter.class)[0];
				supertypes.add(converterType);
			}
			type = reduceTypes(supertypes);
		}

		return type;
	}

	private static Class<?> reduceTypes(final Set<Class<?>> types){
		Class<?> type = null;
		if(!types.isEmpty()){
			final Map<Integer, Class<?>> map = new TreeMap<>(Collections.reverseOrder(Integer::compareTo));
			for(final Class<?> t : types){
				//calculate number of classes to reach Object
				int num = 0;
				Class<?> cls = t;
				while(cls != Object.class){
					num ++;
					cls = cls.getSuperclass();
				}
				map.put(num, t);
			}

			//FIXME refactor
			Iterator<Map.Entry<Integer, Class<?>>> itr = map.entrySet().iterator();
			while(map.size() > 1){
				final Map.Entry<Integer, Class<?>> elem = itr.next();
				final Class<?> value = elem.getValue();
				if(value != Object.class){
					itr.remove();

					final int newKey = elem.getKey() - 1;
					final Class<?> newValue = value.getSuperclass();
					final Class<?> oldValue = map.get(newKey);
					if(oldValue == null)
						map.put(newKey, newValue);
					else if(newValue != oldValue){
						if(newValue.isAssignableFrom(oldValue))
							map.put(newKey, newValue);
						else if(!oldValue.isAssignableFrom(newValue))
							throw new IllegalArgumentException("Non-coherent converter inputs: " + oldValue.getSimpleName() + " and "
								+ newValue.getSimpleName());
					}

					itr = map.entrySet().iterator();
				}
			}
			type = map.values().iterator()
				.next();
		}
		return type;
	}

	static void writePrefix(final BitWriter writer, final ObjectChoices.ObjectChoice chosenAlternative, final ObjectChoices selectFrom){
		//if chosenAlternative.condition() contains '#prefix', then write @ObjectChoice.prefix()
		if(containsPrefixReference(chosenAlternative.condition())){
			final int prefixSize = selectFrom.prefixSize();
			final ByteOrder prefixByteOrder = selectFrom.byteOrder();

			final BitSet bits = BitSet.valueOf(new long[]{chosenAlternative.prefix()});
			if(prefixByteOrder == ByteOrder.LITTLE_ENDIAN)
				bits.reverseBits(prefixSize);

			writer.putBits(bits, prefixSize);
		}
	}

	static boolean containsPrefixReference(final CharSequence condition){
		return CONTEXT_PREFIXED_CHOICE_PREFIX.reset(condition).find();
	}

	@SuppressWarnings("unchecked")
	static <T> void validateData(final Class<? extends Validator<?>> validatorType, final Object data){
		final Validator<T> validator = (Validator<T>)ReflectionHelper.getCreator(validatorType)
			.get();
		if(!validator.isValid((T)data))
			throw new IllegalArgumentException("Validation with " + validatorType.getSimpleName() + " not passed (value is " + data + ")");
	}

	@SuppressWarnings("unchecked")
	static <IN, OUT> OUT converterDecode(final Class<? extends Converter<?, ?>> converterType, final Object data){
		try{
			final Converter<IN, OUT> converter = (Converter<IN, OUT>)ReflectionHelper.getCreator(converterType)
				.get();

			return converter.decode((IN)data);
		}
		catch(final ClassCastException ignored){
			throw new IllegalArgumentException("Can not input " + data.getClass().getSimpleName() + " to decode method of converter "
				+ converterType.getSimpleName());
		}
	}

	@SuppressWarnings("unchecked")
	static <IN, OUT> IN converterEncode(final Class<? extends Converter<?, ?>> converterType, final Object data){
		final Converter<IN, OUT> converter = (Converter<IN, OUT>)ReflectionHelper.getCreator(converterType)
			.get();
		return converter.encode((OUT)data);
	}

}
