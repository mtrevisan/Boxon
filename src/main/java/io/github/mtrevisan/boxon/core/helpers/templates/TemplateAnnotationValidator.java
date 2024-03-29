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
package io.github.mtrevisan.boxon.core.helpers.templates;

import io.github.mtrevisan.boxon.annotations.Checksum;
import io.github.mtrevisan.boxon.annotations.bindings.BindArray;
import io.github.mtrevisan.boxon.annotations.bindings.BindArrayPrimitive;
import io.github.mtrevisan.boxon.annotations.bindings.BindBitSet;
import io.github.mtrevisan.boxon.annotations.bindings.BindList;
import io.github.mtrevisan.boxon.annotations.bindings.BindObject;
import io.github.mtrevisan.boxon.annotations.bindings.BindString;
import io.github.mtrevisan.boxon.annotations.bindings.BindStringTerminated;
import io.github.mtrevisan.boxon.annotations.bindings.ObjectChoices;
import io.github.mtrevisan.boxon.annotations.bindings.ObjectChoicesList;
import io.github.mtrevisan.boxon.annotations.converters.Converter;
import io.github.mtrevisan.boxon.annotations.converters.NullConverter;
import io.github.mtrevisan.boxon.core.helpers.ValueOf;
import io.github.mtrevisan.boxon.exceptions.AnnotationException;
import io.github.mtrevisan.boxon.helpers.CharsetHelper;
import io.github.mtrevisan.boxon.helpers.ContextHelper;
import io.github.mtrevisan.boxon.helpers.JavaHelper;
import io.github.mtrevisan.boxon.io.ParserDataType;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.nio.charset.UnsupportedCharsetException;
import java.util.BitSet;
import java.util.List;


/**
 * Container of all the validators of a message template.
 */
enum TemplateAnnotationValidator{

	OBJECT(BindObject.class){
		@SuppressWarnings("DuplicatedCode")
		@Override
		void validate(final Field field, final Annotation annotation) throws AnnotationException{
			final BindObject binding = (BindObject)annotation;
			final Class<?> type = binding.type();
			validateType(type, BindObject.class);
			if(ParserDataType.isPrimitive(type))
				throw AnnotationException.create("Bad annotation used for {}, should have been used one of the primitive type's annotations",
					BindObject.class.getSimpleName());

			final Class<? extends Converter<?, ?>> converter = binding.converter();
			final ObjectChoices selectFrom = binding.selectFrom();
			final Class<?> selectDefault = binding.selectDefault();
			validateObjectChoice(field, converter, selectFrom, selectDefault, type);
		}
	},

	ARRAY_PRIMITIVE(BindArrayPrimitive.class){
		@Override
		void validate(final Field field, final Annotation annotation) throws AnnotationException{
			final BindArrayPrimitive binding = (BindArrayPrimitive)annotation;
			final Class<?> type = binding.type();
			if(!ParserDataType.isPrimitive(type))
				throw AnnotationException.create("Bad annotation used for {}, should have been used the type `{}.class`",
					BindArray.class.getSimpleName(), type.getSimpleName());

			final Class<? extends Converter<?, ?>> converter = binding.converter();
			validateConverter(field, type, converter);
		}
	},

	ARRAY(BindArray.class){
		@SuppressWarnings("DuplicatedCode")
		@Override
		void validate(final Field field, final Annotation annotation) throws AnnotationException{
			final BindArray binding = (BindArray)annotation;
			final Class<?> type = binding.type();
			validateType(type, BindArray.class);
			if(ParserDataType.isPrimitive(type))
				throw AnnotationException.create("Bad annotation used for {}, should have been used the type `{}.class`",
					BindArray.class.getSimpleName(), type.getSimpleName());

			final Class<? extends Converter<?, ?>> converter = binding.converter();
			final ObjectChoices selectFrom = binding.selectFrom();
			final Class<?> selectDefault = binding.selectDefault();
			validateObjectChoice(field, converter, selectFrom, selectDefault, type);
		}
	},

	LIST_SEPARATED(BindList.class){
		@Override
		void validate(final Field field, final Annotation annotation) throws AnnotationException{
			final BindList binding = (BindList)annotation;
			final Class<?> type = binding.type();
			final Class<?> fieldType = field.getType();
			if(!List.class.isAssignableFrom(fieldType))
				throw AnnotationException.create("Bad annotation used for {}, should have been used the type `List<{}>.class`",
					BindList.class.getSimpleName(), type.getName());

			final Class<? extends Converter<?, ?>> converter = binding.converter();
			final ObjectChoicesList selectFrom = binding.selectFrom();
			validateObjectChoiceList(field, converter, selectFrom, type);
		}
	},

	BIT_SET(BindBitSet.class){
		@Override
		void validate(final Field field, final Annotation annotation) throws AnnotationException{
			final BindBitSet binding = (BindBitSet)annotation;

			final Class<? extends Converter<?, ?>> converter = binding.converter();
			validateConverter(field, BitSet.class, converter);
		}
	},

	STRING(BindString.class){
		@Override
		void validate(final Field field, final Annotation annotation) throws AnnotationException{
			final BindString binding = (BindString)annotation;
			validateCharset(binding.charset());

			final Class<? extends Converter<?, ?>> converter = binding.converter();
			validateConverter(field, String.class, converter);
		}
	},

	STRING_TERMINATED(BindStringTerminated.class){
		@Override
		void validate(final Field field, final Annotation annotation) throws AnnotationException{
			final BindStringTerminated binding = (BindStringTerminated)annotation;
			validateCharset(binding.charset());

			final Class<? extends Converter<?, ?>> converter = binding.converter();
			validateConverter(field, String.class, converter);
		}
	},


	CHECKSUM(Checksum.class){
		@Override
		void validate(final Field field, final Annotation annotation) throws AnnotationException{
			final Class<?> type = ((Checksum)annotation).type();
			final ParserDataType dataType = ParserDataType.fromType(type);
			if(dataType != ParserDataType.BYTE && dataType != ParserDataType.SHORT)
				throw AnnotationException.create("Unrecognized type, must be `byte` or `short`: {}", type.getSimpleName(),
					type.getComponentType().getSimpleName());

			validateConverter(field, type, NullConverter.class);
		}
	};


	private static final ValueOf<TemplateAnnotationValidator, Class<? extends Annotation>> VALIDATORS
		= ValueOf.create(TemplateAnnotationValidator.class, validator -> validator.annotationType);


	private final Class<? extends Annotation> annotationType;


	TemplateAnnotationValidator(final Class<? extends Annotation> type){
		annotationType = type;
	}

	/**
	 * Get the validator for the given annotation.
	 *
	 * @param annotationType	The annotation class type.
	 * @return	The validator for the given annotation.
	 */
	static TemplateAnnotationValidator fromAnnotation(final Class<? extends Annotation> annotationType){
		return VALIDATORS.get(annotationType);
	}

	/**
	 * Validate field and annotation.
	 *
	 * @param field	The field associated to the annotation.
	 * @param annotation	The annotation.
	 * @throws AnnotationException	If an error is detected.
	 */
	abstract void validate(final Field field, final Annotation annotation) throws AnnotationException;


	private static void validateType(final Class<?> bindingType, final Class<? extends Annotation> annotation) throws AnnotationException{
		if(bindingType == Object.class)
			throw AnnotationException.create("Field `type` in {} must not be `Object.class`",
				annotation.getSimpleName());
	}

	private static void validateConverter(final Field field, final Class<?> bindingType, final Class<? extends Converter<?, ?>> converter)
			throws AnnotationException{
		if(converter == NullConverter.class && bindingType != Object.class){
			Class<?> fieldType = field.getType();
			if(fieldType.isArray())
				fieldType = fieldType.getComponentType();

			if(!fieldType.isAssignableFrom(bindingType))
				throw AnnotationException.create("Bad annotation used for type {}: {}",
					fieldType.getSimpleName(), bindingType.getSimpleName());
		}
	}

	private static void validateConverterToList(final Field field, final Class<?> bindingType,
			final Class<? extends Converter<?, ?>> converter, final Class<?> type) throws AnnotationException{
		if(converter == NullConverter.class && bindingType != Object.class){
			final Class<?> fieldType = field.getType();

			if(!type.isAssignableFrom(bindingType))
				throw AnnotationException.create("Bad annotation used for type {}: {}",
					fieldType.getSimpleName(), bindingType.getSimpleName());
		}
	}

	private static void validateObjectChoice(final Field field, final Class<? extends Converter<?, ?>> converter,
			final ObjectChoices selectFrom, final Class<?> selectDefault, final Class<?> type) throws AnnotationException{
		final int prefixLength = selectFrom.prefixLength();
		validatePrefixLength(prefixLength);


		final ObjectChoices.ObjectChoice[] alternatives = selectFrom.alternatives();
		validateObjectAlternatives(field, converter, alternatives, type, prefixLength);


		validateObjectDefaultAlternative(alternatives.length, type, selectDefault);

		validateConverter(field, type, converter);
	}

	private static void validatePrefixLength(final int prefixSize) throws AnnotationException{
		if(prefixSize < 0)
			throw AnnotationException.create("Prefix size must be a non-negative number");
		if(prefixSize > Integer.SIZE)
			throw AnnotationException.create("Prefix size cannot be greater than {} bits", Integer.SIZE);
	}

	private static void validateObjectAlternatives(final Field field, final Class<? extends Converter<?, ?>> converter,
			final ObjectChoices.ObjectChoice[] alternatives, final Class<?> type, final int prefixLength) throws AnnotationException{
		final boolean hasPrefix = (prefixLength > 0);
		final int length = alternatives.length;
		if(hasPrefix && length == 0)
			throw AnnotationException.create("No alternatives present");

		for(int i = 0; i < length; i ++){
			final ObjectChoices.ObjectChoice alternative = alternatives[i];

			validateAlternative(alternative.type(), alternative.condition(), type, hasPrefix);

			validateConverter(field, alternative.type(), converter);
		}
	}


	private static void validateObjectChoiceList(final Field field, final Class<? extends Converter<?, ?>> converter,
			final ObjectChoicesList selectFrom, final Class<?> type) throws AnnotationException{
		final ObjectChoicesList.ObjectChoiceList[] alternatives = selectFrom.alternatives();
		final int length = alternatives.length;
		if(length == 0)
			throw AnnotationException.create("All alternatives must be non-empty");

		int minHeaderLength = Integer.MAX_VALUE;
		for(int i = 0; i < length; i ++){
			final int headerLength = alternatives[i].prefix()
				.length();
			if(headerLength < minHeaderLength)
				minHeaderLength = headerLength;
		}
		validateObjectListAlternatives(field, converter, alternatives, type, minHeaderLength);


		validateConverterToList(field, type, converter, type);
	}

	private static void validateObjectListAlternatives(final Field field, final Class<? extends Converter<?, ?>> converter,
			final ObjectChoicesList.ObjectChoiceList[] alternatives, final Class<?> type, final int prefixLength)
			throws AnnotationException{
		final boolean hasPrefix = (prefixLength > 0);
		final int length = alternatives.length;
		if(hasPrefix && length == 0)
			throw AnnotationException.create("No alternatives present");

		for(int i = 0; i < length; i ++){
			final ObjectChoicesList.ObjectChoiceList alternative = alternatives[i];

			validateAlternative(alternative.type(), alternative.condition(), type, hasPrefix);

			validateConverterToList(field, alternative.type(), converter, type);
		}
	}

	private static void validateAlternative(final Class<?> alternativeType, final CharSequence alternativeCondition, final Class<?> type,
			final boolean hasPrefixLength) throws AnnotationException{
		if(!type.isAssignableFrom(alternativeType))
			throw AnnotationException.create("Type of alternative cannot be assigned to (super) type of annotation");

		if(alternativeCondition.isEmpty())
			throw AnnotationException.create("All conditions must be non-empty");
		if(hasPrefixLength ^ ContextHelper.containsHeaderReference(alternativeCondition))
			throw AnnotationException.create("All conditions must {}contain a reference to the prefix",
				(hasPrefixLength? JavaHelper.EMPTY_STRING: "not "));
	}


	private static void validateObjectDefaultAlternative(final int alternativesCount, final Class<?> type,
			final Class<?> selectDefault) throws AnnotationException{
		if(selectDefault != void.class && alternativesCount == 0)
			throw AnnotationException.create("Useless empty alternative");
		if(selectDefault != void.class && !type.isAssignableFrom(selectDefault))
			throw AnnotationException.create("Type of default alternative cannot be assigned to (super) type of annotation");
	}

	private static void validateCharset(final String charsetName) throws AnnotationException{
		try{
			CharsetHelper.lookup(charsetName);
		}
		catch(final UnsupportedCharsetException ignored){
			throw AnnotationException.create("Invalid charset: '{}'", charsetName);
		}
	}

}
