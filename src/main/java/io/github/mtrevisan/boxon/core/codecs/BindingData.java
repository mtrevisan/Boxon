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

import io.github.mtrevisan.boxon.annotations.bindings.ConverterChoices;
import io.github.mtrevisan.boxon.annotations.bindings.ObjectChoices;
import io.github.mtrevisan.boxon.annotations.bindings.ObjectChoicesList;
import io.github.mtrevisan.boxon.annotations.converters.Converter;
import io.github.mtrevisan.boxon.annotations.validators.Validator;
import io.github.mtrevisan.boxon.exceptions.CodecException;
import io.github.mtrevisan.boxon.helpers.CharsetHelper;
import io.github.mtrevisan.boxon.helpers.ConstructorHelper;
import io.github.mtrevisan.boxon.helpers.ContextHelper;
import io.github.mtrevisan.boxon.helpers.Evaluator;
import io.github.mtrevisan.boxon.io.BitReaderInterface;
import io.github.mtrevisan.boxon.io.ByteOrder;
import org.springframework.expression.EvaluationException;

import java.lang.annotation.Annotation;
import java.nio.charset.Charset;


/** Data associated to an annotation. */
final class BindingData{

	private static final ObjectChoices.ObjectChoice EMPTY_CHOICE = new NullObjectChoice();
	private static final ObjectChoicesList.ObjectChoiceList EMPTY_CHOICE_SEPARATED = new NullObjectChoiceList();


	private Class<?> type;
	private String size;
	private Class<?> selectDefault = void.class;
	private ObjectChoices selectObjectFrom;
	private ObjectChoicesList selectObjectListFrom;
	private final ConverterChoices selectConverterFrom;
	private final Class<? extends Validator<?>> validator;
	private final Class<? extends Converter<?, ?>> defaultConverter;

	private final Object rootObject;
	private final Evaluator evaluator;


	BindingData(final ConverterChoices selectConverterFrom, final Class<? extends Validator<?>> validator,
			final Class<? extends Converter<?, ?>> defaultConverter, final Object rootObject, final Evaluator evaluator){
		this.selectConverterFrom = selectConverterFrom;
		this.validator = validator;
		this.defaultConverter = defaultConverter;

		this.rootObject = rootObject;
		this.evaluator = evaluator;
	}


	void setType(final Class<?> type){
		this.type = type;
	}

	void setSize(final String size){
		this.size = size;
	}

	void setSelectDefault(final Class<?> selectDefault){
		this.selectDefault = selectDefault;
	}

	void setSelectObjectFrom(final ObjectChoices selectObjectFrom){
		this.selectObjectFrom = selectObjectFrom;
	}

	void setSelectObjectListFrom(final ObjectChoicesList selectObjectListFrom){
		this.selectObjectListFrom = selectObjectListFrom;
	}

	/**
	 * Validate the value passed using the configured validator.
	 *
	 * @param value	The value.
	 * @param <T>	The class type of the value.
	 */
	@SuppressWarnings("unchecked")
	<T> void validate(final T value){
		final Validator<T> validatorCreator = (Validator<T>)ConstructorHelper.getCreator(validator)
			.get();
		if(!validatorCreator.isValid(value))
			throw new IllegalArgumentException("Validation of " + validator.getSimpleName() + " didn't passed (value is " + value + ")");
	}

	/**
	 * Adds the given instance as `self` to the context of the evaluator.
	 * <p>Passing {@code null} the `self` key will be deleted.</p>
	 *
	 * @param instance	The object (pass {@code null} to remove the `self` key from the context).
	 */
	void addToContext(final Object instance){
		evaluator.addToContext(ContextHelper.CONTEXT_SELF, instance);
	}

	/**
	 * Convenience method to fast evaluate a positive integer.
	 *
	 * @return	The size, or a negative number if the expression is not a valid positive integer.
	 * @throws EvaluationException	If an error occurs during the evaluation of an expression.
	 */
	int evaluateSize(){
		return evaluator.evaluateSize(size, rootObject);
	}

	/**
	 * Gets the alternative class type that parses the next data.
	 *
	 * @param reader	The reader from which to read the data from.
	 * @return	The class type of the chosen alternative.
	 * @throws CodecException	If a codec cannot be found for the chosen alternative.
	 */
	Class<?> chooseAlternativeType(final BitReaderInterface reader) throws CodecException{
		if(!hasSelectAlternatives())
			return type;

		addPrefixToContext(reader);

		final ObjectChoices.ObjectChoice[] alternatives = selectObjectFrom.alternatives();
		final ObjectChoices.ObjectChoice chosenAlternative = chooseAlternative(alternatives);
		final Class<?> chosenAlternativeType = (!isEmptyChoice(chosenAlternative)
			? chosenAlternative.type()
			: selectDefault);

		if(chosenAlternativeType == void.class)
			throw CodecException.create("Cannot find a valid codec from given alternatives for {}",
				rootObject.getClass().getSimpleName());

		return chosenAlternativeType;
	}

	/**
	 * Add the prefix to the evaluator context if needed.
	 *
	 * @param reader	The reader from which to read the prefix.
	 */
	private void addPrefixToContext(final BitReaderInterface reader){
		final int prefixSize = selectObjectFrom.prefixLength();
		if(prefixSize > 0){
			final ByteOrder prefixBitOrder = selectObjectFrom.bitOrder();
			final long[] array = reader.getBitSet(prefixSize, prefixBitOrder)
				.toLongArray();
			final int prefix = (array.length > 0? (int)array[0]: 0);

			evaluator.addToContext(ContextHelper.CONTEXT_CHOICE_PREFIX, prefix);
		}
	}

	private ObjectChoices.ObjectChoice chooseAlternative(final ObjectChoices.ObjectChoice[] alternatives){
		for(int i = 0, length = alternatives.length; i < length; i ++){
			final ObjectChoices.ObjectChoice alternative = alternatives[i];
			if(evaluator.evaluateBoolean(alternative.condition(), rootObject))
				return alternative;
		}
		return EMPTY_CHOICE;
	}

	/**
	 * Whether the select-object-from binding has any alternatives.
	 *
	 * @return	Whether the select-object-from binding has any alternatives.
	 */
	boolean hasSelectAlternatives(){
		return (selectObjectFrom.alternatives().length > 0);
	}

	/**
	 * Gets the alternative class type that parses the next data.
	 *
	 * @param reader	The reader from which to read the data from.
	 * @return	The class type of the chosen alternative.
	 */
	Class<?> chooseAlternativeSeparatedType(final BitReaderInterface reader){
		if(!hasSelectSeparatedAlternatives())
			return type;

		final boolean hasHeader = addListHeaderToContext(reader);
		if(!hasHeader)
			return null;

		final ObjectChoicesList.ObjectChoiceList[] alternatives = selectObjectListFrom.alternatives();
		final ObjectChoicesList.ObjectChoiceList chosenAlternative = chooseAlternative(alternatives);
		final Class<?> chosenAlternativeType = (!isEmptyChoice(chosenAlternative)
			? chosenAlternative.type()
			: selectDefault);

		return (chosenAlternativeType != void.class? chosenAlternativeType: null);
	}

	/**
	 * Add the prefix to the evaluator context if needed.
	 *
	 * @param reader	The reader from which to read the header.
	 * @return	Whether a prefix was retrieved.
	 */
	private boolean addListHeaderToContext(final BitReaderInterface reader){
		final byte terminator = selectObjectListFrom.terminator();
		final Charset charset = CharsetHelper.lookup(selectObjectListFrom.charset());
		final String prefix = reader.getTextUntilTerminatorWithoutConsuming(terminator, charset);
		evaluator.addToContext(ContextHelper.CONTEXT_CHOICE_PREFIX, prefix);
		return !prefix.isEmpty();
	}

	/**
	 * Whether the select-object-separated-from binding has any alternatives.
	 *
	 * @return	Whether the select-object-separated-from binding has any alternatives.
	 */
	private boolean hasSelectSeparatedAlternatives(){
		return (selectObjectListFrom.alternatives().length > 0);
	}

	private ObjectChoicesList.ObjectChoiceList chooseAlternative(final ObjectChoicesList.ObjectChoiceList[] alternatives){
		for(int i = 0, length = alternatives.length; i < length; i ++){
			final ObjectChoicesList.ObjectChoiceList alternative = alternatives[i];

			if(evaluator.evaluateBoolean(alternative.condition(), rootObject))
				return alternative;
		}
		return EMPTY_CHOICE_SEPARATED;
	}

	private static boolean isEmptyChoice(final Annotation choice){
		return (choice.annotationType() == Annotation.class);
	}

	/**
	 * Get the first converter that matches the condition.
	 *
	 * @return	The converter class.
	 */
	Class<? extends Converter<?, ?>> getChosenConverter(){
		final ConverterChoices.ConverterChoice[] alternatives = selectConverterFrom.alternatives();
		for(int i = 0, length = alternatives.length; i < length; i ++){
			final ConverterChoices.ConverterChoice alternative = alternatives[i];

			if(evaluator.evaluateBoolean(alternative.condition(), rootObject))
				return alternative.converter();
		}
		return defaultConverter;
	}

}
