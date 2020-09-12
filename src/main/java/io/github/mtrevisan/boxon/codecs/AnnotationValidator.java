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

import io.github.mtrevisan.boxon.annotations.Checksum;
import io.github.mtrevisan.boxon.annotations.bindings.BindArray;
import io.github.mtrevisan.boxon.annotations.bindings.BindArrayPrimitive;
import io.github.mtrevisan.boxon.annotations.bindings.BindBits;
import io.github.mtrevisan.boxon.annotations.bindings.BindByte;
import io.github.mtrevisan.boxon.annotations.bindings.BindDecimal;
import io.github.mtrevisan.boxon.annotations.bindings.BindDouble;
import io.github.mtrevisan.boxon.annotations.bindings.BindFloat;
import io.github.mtrevisan.boxon.annotations.bindings.BindInt;
import io.github.mtrevisan.boxon.annotations.bindings.BindInteger;
import io.github.mtrevisan.boxon.annotations.bindings.BindLong;
import io.github.mtrevisan.boxon.annotations.bindings.BindObject;
import io.github.mtrevisan.boxon.annotations.bindings.BindShort;
import io.github.mtrevisan.boxon.annotations.bindings.BindString;
import io.github.mtrevisan.boxon.annotations.bindings.BindStringTerminated;
import io.github.mtrevisan.boxon.annotations.bindings.ConverterChoices;
import io.github.mtrevisan.boxon.annotations.bindings.ObjectChoices;
import io.github.mtrevisan.boxon.annotations.converters.Converter;
import io.github.mtrevisan.boxon.exceptions.AnnotationException;
import io.github.mtrevisan.boxon.internal.ParserDataType;
import io.github.mtrevisan.boxon.internal.ReflectionHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Annotation;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;


enum AnnotationValidator{

	OBJECT(BindObject.class){
		@Override
		void validate(final Annotation annotation) throws AnnotationException{
			final BindObject binding = (BindObject)annotation;
			final ObjectChoices selectFrom = binding.selectFrom();
			final Class<?> type = binding.type();
			if(ParserDataType.isPrimitive(type))
				throw new AnnotationException("Bad annotation used for {}, should have been used one of the primitive type's annotations",
					BindObject.class.getSimpleName());

			validateObjectChoice(selectFrom, binding.selectDefault(), type);

			validateConverter(binding.type(), binding.selectConverterFrom(), binding.converter());
		}
	},

	ARRAY_PRIMITIVE(BindArrayPrimitive.class){
		@Override
		void validate(final Annotation annotation) throws AnnotationException{
			final BindArrayPrimitive binding = (BindArrayPrimitive)annotation;
			final Class<?> type = binding.type();
			if(!ParserDataType.isPrimitive(type))
				throw new AnnotationException("Bad annotation used for {}, should have been used the type `{}.class`",
					BindArray.class.getSimpleName(), ParserDataType.toObjectiveTypeOrSelf(type).getSimpleName());

			final Class<?> bindingType = ReflectionHelper.addArrayType(binding.type(), 1);
			validateConverter(bindingType, binding.selectConverterFrom(), binding.converter());
		}
	},

	ARRAY(BindArray.class){
		@Override
		void validate(final Annotation annotation) throws AnnotationException{
			final BindArray binding = (BindArray)annotation;
			final ObjectChoices selectFrom = binding.selectFrom();
			final Class<?> type = binding.type();
			if(ParserDataType.isPrimitive(type))
				throw new AnnotationException("Bad annotation used for {}, should have been used the type `{}.class`",
					BindArrayPrimitive.class.getSimpleName(), ParserDataType.toPrimitiveTypeOrSelf(type).getSimpleName());

			validateObjectChoice(selectFrom, binding.selectDefault(), type);

			final Class<?> bindingType = ReflectionHelper.addArrayType(binding.type(), 1);
			validateConverter(bindingType, binding.selectConverterFrom(), binding.converter());
		}
	},

	BITS(BindBits.class){
		@Override
		void validate(final Annotation annotation) throws AnnotationException{
			final BindBits binding = (BindBits)annotation;
			validateConverter(BitSet.class, binding.selectConverterFrom(), binding.converter());
		}
	},

	BYTE(BindByte.class){
		@Override
		void validate(final Annotation annotation) throws AnnotationException{
			final BindByte binding = (BindByte)annotation;
			validateConverter(byte.class, binding.selectConverterFrom(), binding.converter());
		}
	},

	SHORT(BindShort.class){
		@Override
		void validate(final Annotation annotation) throws AnnotationException{
			final BindShort binding = (BindShort)annotation;
			validateConverter(short.class, binding.selectConverterFrom(), binding.converter());
		}
	},

	INT(BindInt.class){
		@Override
		void validate(final Annotation annotation) throws AnnotationException{
			final BindInt binding = (BindInt)annotation;
			validateConverter(int.class, binding.selectConverterFrom(), binding.converter());
		}
	},

	LONG(BindLong.class){
		@Override
		void validate(final Annotation annotation) throws AnnotationException{
			final BindLong binding = (BindLong)annotation;
			validateConverter(long.class, binding.selectConverterFrom(), binding.converter());
		}
	},

	INTEGER(BindInteger.class){
		@Override
		void validate(final Annotation annotation) throws AnnotationException{
			final BindInteger binding = (BindInteger)annotation;
			validateConverter(BigInteger.class, binding.selectConverterFrom(), binding.converter());
		}
	},

	FLOAT(BindFloat.class){
		@Override
		void validate(final Annotation annotation) throws AnnotationException{
			final BindFloat binding = (BindFloat)annotation;
			validateConverter(float.class, binding.selectConverterFrom(), binding.converter());
		}
	},

	DOUBLE(BindDouble.class){
		@Override
		void validate(final Annotation annotation) throws AnnotationException{
			final BindDouble binding = (BindDouble)annotation;
			validateConverter(double.class, binding.selectConverterFrom(), binding.converter());
		}
	},

	DECIMAL(BindDecimal.class){
		@Override
		void validate(final Annotation annotation) throws AnnotationException{
			final BindDecimal binding = (BindDecimal)annotation;
			final Class<?> type = binding.type();
			final ParserDataType dataType = ParserDataType.fromType(type);
			if(dataType != ParserDataType.FLOAT && dataType != ParserDataType.DOUBLE)
				throw new AnnotationException("Bad type, should have been one of `{}.class` or `{}.class`", Float.class.getSimpleName(),
					Double.class.getSimpleName());

			validateConverter(BigDecimal.class, binding.selectConverterFrom(), binding.converter());
		}
	},

	STRING(BindString.class){
		@Override
		void validate(final Annotation annotation) throws AnnotationException{
			final BindString binding = (BindString)annotation;
			CodecHelper.assertCharset(binding.charset());

			validateConverter(String.class, binding.selectConverterFrom(), binding.converter());
		}
	},

	STRING_TERMINATED(BindStringTerminated.class){
		@Override
		void validate(final Annotation annotation) throws AnnotationException{
			final BindStringTerminated binding = (BindStringTerminated)annotation;
			CodecHelper.assertCharset(binding.charset());

			validateConverter(String.class, binding.selectConverterFrom(), binding.converter());
		}
	},

	CHECKSUM(Checksum.class){
		@Override
		void validate(final Annotation annotation) throws AnnotationException{
			final Class<?> type = ((Checksum)annotation).type();
			if(!ParserDataType.isPrimitiveOrWrapper(type))
				throw new AnnotationException("Unrecognized type for field {}.{}: {}", getClass().getName(), type.getSimpleName(),
					type.getComponentType().getSimpleName());
		}
	};


	private static final Logger LOGGER = LoggerFactory.getLogger(AnnotationValidator.class);

	private static final Map<Class<? extends Annotation>, AnnotationValidator> ANNOTATION_VALIDATORS = new HashMap<>(5);
	static{
		for(final AnnotationValidator validator : values())
			ANNOTATION_VALIDATORS.put(validator.annotationType, validator);
	}

	private final Class<? extends Annotation> annotationType;


	AnnotationValidator(final Class<? extends Annotation> type){
		annotationType = type;
	}

	static AnnotationValidator fromAnnotation(final Annotation annotation){
		return ANNOTATION_VALIDATORS.get(annotation.annotationType());
	}

	abstract void validate(final Annotation annotation) throws AnnotationException;

	private static void validateObjectChoice(final ObjectChoices selectFrom, final Class<?> selectDefault, final Class<?> type)
			throws AnnotationException{
		final int prefixSize = selectFrom.prefixSize();
		validatePrefixSize(prefixSize);

		final ObjectChoices.ObjectChoice[] alternatives = selectFrom.alternatives();
		validateObjectAlternatives(alternatives, type, prefixSize);

		validateObjectDefaultAlternative(alternatives, type, selectDefault);
	}

	private static void validatePrefixSize(final int prefixSize) throws AnnotationException{
		if(prefixSize < 0)
			throw new AnnotationException("Prefix size must be a non-negative number");
		if(prefixSize > Integer.SIZE)
			throw new AnnotationException("Prefix size cannot be greater than {} bits", Integer.SIZE);
	}

	private static void validateObjectAlternatives(final ObjectChoices.ObjectChoice[] alternatives, final Class<?> type,
			final int prefixSize) throws AnnotationException{
		final boolean hasPrefixSize = (prefixSize > 0);
		if(hasPrefixSize && alternatives.length == 0)
			throw new AnnotationException("No alternatives present");
		for(final ObjectChoices.ObjectChoice alternative : alternatives)
			validateAlternative(alternative, type, hasPrefixSize);
	}

	private static void validateAlternative(final ObjectChoices.ObjectChoice alternative, final Class<?> type,
			final boolean hasPrefixSize) throws AnnotationException{
		if(!type.isAssignableFrom(alternative.type()))
			throw new AnnotationException("Type of alternative cannot be assigned to (super) type of annotation");

		final String condition = alternative.condition();
		if(condition.isEmpty())
			throw new AnnotationException("All conditions must be non-empty");
		if(hasPrefixSize ^ CodecHelper.containsPrefixReference(condition))
			throw new AnnotationException("All conditions must {}contain a reference to the prefix", (hasPrefixSize? "": "not "));
	}

	private static void validateObjectDefaultAlternative(final ObjectChoices.ObjectChoice[] alternatives, final Class<?> type,
			final Class<?> selectDefault) throws AnnotationException{
		if(selectDefault != void.class && alternatives.length == 0)
			LOGGER.warn("Useless definition of default alternative ({}) due to no alternatives present on @BindArray or @BindObject",
				selectDefault.getSimpleName());
		if(selectDefault != void.class && !type.isAssignableFrom(selectDefault))
			throw new AnnotationException("Type of default alternative cannot be assigned to (super) type of annotation");
	}

	/**
	 * Assure `type` can be fed to the converters.
	 *
	 * @param type	The type of variable fed to the converters.
	 * @param selectConverterFrom	The list of converters from which to choose.
	 * @param defaultConverter	The default converter.
	 * @throws AnnotationException	If the give `type` cannot be fed to the converters.
	 */
	private static void validateConverter(Class<?> type, final ConverterChoices selectConverterFrom,
			final Class<? extends Converter<?, ?>> defaultConverter) throws AnnotationException{
//		type = ParserDataType.toObjectiveTypeOrSelf(type);
//		Class<?> converterInputType = ReflectionHelper.resolveGenericTypes(defaultConverter, Converter.class)[0];
//		if(!converterInputType.isAssignableFrom(type))
//			throw new AnnotationException("Type of read data ({}) cannot be fed to default converter ({})", type.getSimpleName(),
//				converterInputType.getSimpleName());
//
//		final ConverterChoices.ConverterChoice[] alternatives = selectConverterFrom.alternatives();
//		for(final ConverterChoices.ConverterChoice alternative : alternatives){
//			converterInputType = ReflectionHelper.resolveGenericTypes(alternative.converter(), Converter.class)[0];
//			if(!converterInputType.isAssignableFrom(type))
//				throw new AnnotationException("Type of read data ({}) cannot be fed to alternative converter ({})", type.getSimpleName(),
//					converterInputType.getSimpleName());
//		}
	}

}
