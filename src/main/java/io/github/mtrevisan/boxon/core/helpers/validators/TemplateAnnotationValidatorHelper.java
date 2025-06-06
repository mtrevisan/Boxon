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
package io.github.mtrevisan.boxon.core.helpers.validators;

import io.github.mtrevisan.boxon.annotations.bindings.BindObject;
import io.github.mtrevisan.boxon.annotations.bindings.ObjectChoices;
import io.github.mtrevisan.boxon.annotations.bindings.ObjectChoicesList;
import io.github.mtrevisan.boxon.annotations.converters.Converter;
import io.github.mtrevisan.boxon.annotations.converters.NullConverter;
import io.github.mtrevisan.boxon.exceptions.AnnotationException;
import io.github.mtrevisan.boxon.helpers.CharsetHelper;
import io.github.mtrevisan.boxon.helpers.ContextHelper;
import io.github.mtrevisan.boxon.helpers.JavaHelper;

import java.nio.charset.UnsupportedCharsetException;


/**
 * A container for all validators of a message template.
 */
final class TemplateAnnotationValidatorHelper{

	private static final ValidationStrategy NULL_CONVERTER_VALIDATOR = new NullConverterValidationStrategy();
	private static final ValidationStrategy NON_NULL_CONVERTER_VALIDATOR = new NonNullConverterValidationStrategy();


	private TemplateAnnotationValidatorHelper(){}


	static void validateConverter(final Class<?> fieldType, final Class<? extends Converter<?, ?>> converter, final Class<?> bindingType)
			throws AnnotationException{
		if(fieldType != Object.class){
			final ValidationStrategy strategy = selectValidationStrategy(converter);
			strategy.validate(fieldType, converter, bindingType);
		}
	}

	private static ValidationStrategy selectValidationStrategy(final Class<? extends Converter<?, ?>> converter){
		return (converter == NullConverter.class? NULL_CONVERTER_VALIDATOR: NON_NULL_CONVERTER_VALIDATOR);
	}


	private static void validateConverterToList(final Class<?> fieldType, final Class<?> bindingType,
			final Class<? extends Converter<?, ?>> converter, final Class<?> type) throws AnnotationException{
		if(converter == NullConverter.class && bindingType != Object.class && !type.isAssignableFrom(bindingType))
			throw AnnotationException.createBadAnnotation(fieldType, bindingType);
	}

	static void validateObjectChoice(final Class<?> fieldType, final BindObject binding) throws AnnotationException{
		final ObjectChoices selectFrom = binding.selectFrom();
		final byte prefixLength = selectFrom.prefixLength();
		validatePrefixLength(prefixLength);


		final Class<?> type = binding.type();
		final ObjectChoices.ObjectChoice[] alternatives = selectFrom.alternatives();
		if(alternatives.length > 0){
			final Class<? extends Converter<?, ?>> converter = binding.converter();
			final Class<?> selectDefault = binding.selectDefault();

			validateObjectAlternatives(fieldType, converter, type, alternatives, prefixLength);


			validateObjectDefaultAlternative(alternatives.length, type, selectDefault);

			validateConverter(fieldType, converter, type);
		}
	}

	private static void validatePrefixLength(final byte prefixSize) throws AnnotationException{
		if(prefixSize < 0)
			throw AnnotationException.create("Prefix size must be a non-negative number");
		if(prefixSize > Long.SIZE)
			throw AnnotationException.create("Prefix size cannot be greater than {} (bits)", Long.SIZE);
	}

	private static void validateObjectAlternatives(final Class<?> fieldType, final Class<? extends Converter<?, ?>> converter,
			final Class<?> type, final ObjectChoices.ObjectChoice[] alternatives, final int prefixLength) throws AnnotationException{
		final boolean hasPrefix = (prefixLength > 0);
		final int length = alternatives.length;
		if(hasPrefix && length == 0)
			throw AnnotationException.create("No alternatives present");

		for(int i = 0; i < length; i ++){
			final ObjectChoices.ObjectChoice alternative = alternatives[i];

			validateAlternative(alternative.type(), alternative.condition(), type, hasPrefix);

			validateConverter(fieldType, converter, alternative.type());
		}

		validateConverter(fieldType, converter, type);
	}


	static void validateObjectChoiceList(final Class<?> fieldType, final BindObject binding) throws AnnotationException{
		final ObjectChoicesList selectFromList = binding.selectFromList();
		final ObjectChoices.ObjectChoice[] alternatives = selectFromList.alternatives();
		final int length = alternatives.length;
		if(length > 0){
			final Class<? extends Converter<?, ?>> converter = binding.converter();
			final Class<?> type = binding.type();

			final int minHeaderLength = calculateMinHeaderLength(alternatives);
			final boolean hasPrefix = (minHeaderLength > 0);
			for(int i = 0; i < length; i ++){
				final ObjectChoices.ObjectChoice alternative = alternatives[i];

				validateAlternative(alternative.type(), alternative.condition(), type, hasPrefix);

				validateConverterToList(fieldType, alternative.type(), converter, type);
			}


			validateConverterToList(fieldType, type, converter, type);

			final Class<?> selectDefault = binding.selectDefault();
			validateObjectDefaultAlternative(alternatives.length, type, selectDefault);
		}
	}

	private static int calculateMinHeaderLength(final ObjectChoices.ObjectChoice[] alternatives){
		int minHeaderLength = Integer.MAX_VALUE;
		for(int i = 0, length = alternatives.length; i < length; i ++){
			final ObjectChoices.ObjectChoice alternative = alternatives[i];

			final int headerLength = alternative.prefix()
				.length();
			if(headerLength < minHeaderLength)
				minHeaderLength = headerLength;
		}
		return minHeaderLength;
	}

	private static void validateAlternative(final Class<?> alternativeType, final CharSequence alternativeCondition, final Class<?> type,
			final boolean hasPrefix) throws AnnotationException{
		if(!type.isAssignableFrom(alternativeType))
			throw AnnotationException.create("Type of alternative cannot be assigned to (super) type of annotation");

		if(alternativeCondition.isEmpty())
			throw AnnotationException.create("All conditions must be non-empty");
		if(hasPrefix ^ ContextHelper.containsHeaderReference(alternativeCondition))
			throw AnnotationException.create("All conditions must {}contain a reference to the prefix",
				(hasPrefix? JavaHelper.EMPTY_STRING: "not "));
	}


	private static void validateObjectDefaultAlternative(final int alternativesCount, final Class<?> type,
			final Class<?> selectDefault) throws AnnotationException{
		if(selectDefault != void.class && alternativesCount == 0)
			throw AnnotationException.create("Useless empty alternative");
		if(selectDefault != void.class && !type.isAssignableFrom(selectDefault))
			throw AnnotationException.create("Type of default alternative cannot be assigned to (super) type of annotation");
	}

	static void validateCharset(final String charsetName) throws AnnotationException{
		try{
			CharsetHelper.lookup(charsetName);
		}
		catch(final UnsupportedCharsetException ignored){
			throw AnnotationException.create("Invalid charset: '{}'", charsetName);
		}
	}

}
