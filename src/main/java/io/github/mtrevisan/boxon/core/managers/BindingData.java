/*
 * Copyright (c) 2020-2022 Mauro Trevisan
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
package io.github.mtrevisan.boxon.core.managers;

import io.github.mtrevisan.boxon.annotations.bindings.ConverterChoices;
import io.github.mtrevisan.boxon.annotations.bindings.ObjectChoices;
import io.github.mtrevisan.boxon.annotations.converters.Converter;
import io.github.mtrevisan.boxon.annotations.validators.Validator;
import io.github.mtrevisan.boxon.core.managers.helpers.Evaluator;
import io.github.mtrevisan.boxon.exceptions.CodecException;
import io.github.mtrevisan.boxon.io.BitReaderInterface;
import io.github.mtrevisan.boxon.io.ByteOrder;
import io.github.mtrevisan.boxon.internal.ConstructorHelper;
import io.github.mtrevisan.boxon.core.managers.helpers.ContextHelper;
import org.springframework.expression.EvaluationException;

import java.lang.annotation.Annotation;


/** Data associated to an annotation. */
public final class BindingData{

	private static final ObjectChoices.ObjectChoice EMPTY_CHOICE = new NullObjectChoice();


	private Class<?> type;
	private String size;
	private Class<?> selectDefault = void.class;
	private ObjectChoices selectObjectFrom;
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

	/**
	 * Validate the value passed using the configured validator.
	 *
	 * @param value	The value.
	 * @param <T>	The class type of the value.
	 */
	@SuppressWarnings("unchecked")
	public <T> void validate(final T value){
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
	public void addToContext(final Object instance){
		evaluator.addToContext(ContextHelper.CONTEXT_SELF, instance);
	}

	/**
	 * Convenience method to fast evaluate a positive integer.
	 *
	 * @return	The size, or a negative number if the expression is not a valid positive integer.
	 * @throws EvaluationException   If an error occurs during the evaluation of an expression.
	 */
	public int evaluateSize(){
		return evaluator.evaluateSize(size, rootObject);
	}

	/**
	 * Gets the alternative class type that parses the next data.
	 *
	 * @param reader	The reader from which to read the data from.
	 * @return	The class type of the chosen alternative.
	 * @throws CodecException	If a codec cannot be found for the chosen alternative.
	 */
	public Class<?> chooseAlternativeType(final BitReaderInterface reader) throws CodecException{
		if(!hasSelectAlternatives())
			return type;

		//add the prefix to the evaluator context:
		final int prefixSize = selectObjectFrom.prefixSize();
		if(prefixSize > 0){
			final ByteOrder prefixBitOrder = selectObjectFrom.bitOrder();
			final long[] array = reader.getBitSet(prefixSize, prefixBitOrder)
				.toLongArray();
			final int prefix = (array.length > 0? (int)array[0]: 0);

			evaluator.addToContext(ContextHelper.CONTEXT_CHOICE_PREFIX, prefix);
		}

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
	 * Whether the select-object-from binding has any alternatives.
	 *
	 * @return	Whether the select-object-from binding has any alternatives.
	 */
	public boolean hasSelectAlternatives(){
		return (selectObjectFrom.alternatives().length > 0);
	}

	private static boolean isEmptyChoice(final ObjectChoices.ObjectChoice choice){
		return (choice.annotationType() == Annotation.class);
	}

	private ObjectChoices.ObjectChoice chooseAlternative(final ObjectChoices.ObjectChoice[] alternatives){
		for(int i = 0; i < alternatives.length; i ++){
			final ObjectChoices.ObjectChoice alternative = alternatives[i];
			if(evaluator.evaluateBoolean(alternative.condition(), rootObject))
				return alternative;
		}
		return EMPTY_CHOICE;
	}

	/**
	 * Get the first converter that matches the condition.
	 *
	 * @return	The converter class.
	 */
	public Class<? extends Converter<?, ?>> getChosenConverter(){
		final ConverterChoices.ConverterChoice[] alternatives = selectConverterFrom.alternatives();
		for(int i = 0; i < alternatives.length; i ++){
			final ConverterChoices.ConverterChoice alternative = alternatives[i];
			if(evaluator.evaluateBoolean(alternative.condition(), rootObject))
				return alternative.converter();
		}
		return defaultConverter;
	}

}
