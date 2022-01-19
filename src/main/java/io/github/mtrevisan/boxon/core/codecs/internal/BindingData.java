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
package io.github.mtrevisan.boxon.core.codecs.internal;

import io.github.mtrevisan.boxon.annotations.bindings.BindArray;
import io.github.mtrevisan.boxon.annotations.bindings.BindArrayPrimitive;
import io.github.mtrevisan.boxon.annotations.bindings.BindBits;
import io.github.mtrevisan.boxon.annotations.bindings.BindByte;
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
import io.github.mtrevisan.boxon.annotations.validators.Validator;
import io.github.mtrevisan.boxon.core.codecs.BitReaderInterface;
import io.github.mtrevisan.boxon.core.codecs.ByteOrder;
import io.github.mtrevisan.boxon.exceptions.CodecException;
import io.github.mtrevisan.boxon.internal.ConstructorHelper;
import io.github.mtrevisan.boxon.internal.ContextHelper;

import java.lang.annotation.Annotation;


/** Data associated to an annotation. */
final class BindingData{

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


	static BindingData create(final BindArray annotation, final Object rootObject, final Evaluator evaluator){
		final ConverterChoices selectConverterFrom = annotation.selectConverterFrom();
		final Class<? extends Validator<?>> validator = annotation.validator();
		final Class<? extends Converter<?, ?>> converter = annotation.converter();
		final BindingData data = new BindingData(selectConverterFrom, validator, converter, rootObject, evaluator);
		data.type = annotation.type();
		data.size = annotation.size();
		data.selectDefault = annotation.selectDefault();
		data.selectObjectFrom = annotation.selectFrom();
		return data;
	}

	static BindingData create(final BindArrayPrimitive annotation, final Object rootObject, final Evaluator evaluator){
		final ConverterChoices selectConverterFrom = annotation.selectConverterFrom();
		final Class<? extends Validator<?>> validator = annotation.validator();
		final Class<? extends Converter<?, ?>> converter = annotation.converter();
		final BindingData data = new BindingData(selectConverterFrom, validator, converter, rootObject, evaluator);
		data.type = annotation.type();
		data.size = annotation.size();
		return data;
	}

	static BindingData create(final BindBits annotation, final Object rootObject, final Evaluator evaluator){
		final ConverterChoices selectConverterFrom = annotation.selectConverterFrom();
		final Class<? extends Validator<?>> validator = annotation.validator();
		final Class<? extends Converter<?, ?>> converter = annotation.converter();
		final BindingData data = new BindingData(selectConverterFrom, validator, converter, rootObject, evaluator);
		data.size = annotation.size();
		return data;
	}

	static BindingData create(final BindByte annotation, final Object rootObject, final Evaluator evaluator){
		final ConverterChoices selectConverterFrom = annotation.selectConverterFrom();
		final Class<? extends Validator<?>> validator = annotation.validator();
		final Class<? extends Converter<?, ?>> converter = annotation.converter();
		return new BindingData(selectConverterFrom, validator, converter, rootObject, evaluator);
	}

	static BindingData create(final BindDouble annotation, final Object rootObject, final Evaluator evaluator){
		final ConverterChoices selectConverterFrom = annotation.selectConverterFrom();
		final Class<? extends Validator<?>> validator = annotation.validator();
		final Class<? extends Converter<?, ?>> converter = annotation.converter();
		return new BindingData(selectConverterFrom, validator, converter, rootObject, evaluator);
	}

	static BindingData create(final BindFloat annotation, final Object rootObject, final Evaluator evaluator){
		final ConverterChoices selectConverterFrom = annotation.selectConverterFrom();
		final Class<? extends Validator<?>> validator = annotation.validator();
		final Class<? extends Converter<?, ?>> converter = annotation.converter();
		return new BindingData(selectConverterFrom, validator, converter, rootObject, evaluator);
	}

	static BindingData create(final BindInt annotation, final Object rootObject, final Evaluator evaluator){
		final ConverterChoices selectConverterFrom = annotation.selectConverterFrom();
		final Class<? extends Validator<?>> validator = annotation.validator();
		final Class<? extends Converter<?, ?>> converter = annotation.converter();
		return new BindingData(selectConverterFrom, validator, converter, rootObject, evaluator);
	}

	static BindingData create(final BindInteger annotation, final Object rootObject, final Evaluator evaluator){
		final ConverterChoices selectConverterFrom = annotation.selectConverterFrom();
		final Class<? extends Validator<?>> validator = annotation.validator();
		final Class<? extends Converter<?, ?>> converter = annotation.converter();
		final BindingData data = new BindingData(selectConverterFrom, validator, converter, rootObject, evaluator);
		data.size = annotation.size();
		return data;
	}

	static BindingData create(final BindLong annotation, final Object rootObject, final Evaluator evaluator){
		final ConverterChoices selectConverterFrom = annotation.selectConverterFrom();
		final Class<? extends Validator<?>> validator = annotation.validator();
		final Class<? extends Converter<?, ?>> converter = annotation.converter();
		return new BindingData(selectConverterFrom, validator, converter, rootObject, evaluator);
	}

	static BindingData create(final BindObject annotation, final Object rootObject, final Evaluator evaluator){
		final ConverterChoices selectConverterFrom = annotation.selectConverterFrom();
		final Class<? extends Validator<?>> validator = annotation.validator();
		final Class<? extends Converter<?, ?>> converter = annotation.converter();
		final BindingData data = new BindingData(selectConverterFrom, validator, converter, rootObject, evaluator);
		data.type = annotation.type();
		data.selectDefault = annotation.selectDefault();
		data.selectObjectFrom = annotation.selectFrom();
		return data;
	}

	static BindingData create(final BindShort annotation, final Object rootObject, final Evaluator evaluator){
		final ConverterChoices selectConverterFrom = annotation.selectConverterFrom();
		final Class<? extends Validator<?>> validator = annotation.validator();
		final Class<? extends Converter<?, ?>> converter = annotation.converter();
		return new BindingData(selectConverterFrom, validator, converter, rootObject, evaluator);
	}

	static BindingData create(final BindString annotation, final Object rootObject, final Evaluator evaluator){
		final ConverterChoices selectConverterFrom = annotation.selectConverterFrom();
		final Class<? extends Validator<?>> validator = annotation.validator();
		final Class<? extends Converter<?, ?>> converter = annotation.converter();
		final BindingData data = new BindingData(selectConverterFrom, validator, converter, rootObject, evaluator);
		data.size = annotation.size();
		return data;
	}

	static BindingData create(final BindStringTerminated annotation, final Object rootObject, final Evaluator evaluator){
		final ConverterChoices selectConverterFrom = annotation.selectConverterFrom();
		final Class<? extends Validator<?>> validator = annotation.validator();
		final Class<? extends Converter<?, ?>> converter = annotation.converter();
		return new BindingData(selectConverterFrom, validator, converter, rootObject, evaluator);
	}


	private BindingData(final ConverterChoices selectConverterFrom, final Class<? extends Validator<?>> validator,
			final Class<? extends Converter<?, ?>> defaultConverter, final Object rootObject, final Evaluator evaluator){
		this.selectConverterFrom = selectConverterFrom;
		this.validator = validator;
		this.defaultConverter = defaultConverter;

		this.rootObject = rootObject;
		this.evaluator = evaluator;
	}

	@SuppressWarnings("unchecked")
	<T> void validate(final Object value){
		final Validator<T> validatorCreator = (Validator<T>)ConstructorHelper.getCreator(validator)
			.get();
		if(!validatorCreator.isValid((T)value))
			throw new IllegalArgumentException("Validation with " + validator.getSimpleName() + " not passed (value is " + value + ")");
	}

	void addToContext(final Object instance){
		evaluator.addToContext(ContextHelper.CONTEXT_SELF, instance);
	}

	int evaluateSize(){
		return evaluator.evaluateSize(size, rootObject);
	}

	Class<?> chooseAlternativeType(final BitReaderInterface reader) throws CodecException{
		if(!hasSelectAlternatives())
			return type;

		final int prefixSize = selectObjectFrom.prefixSize();
		if(prefixSize > 0){
			final ByteOrder prefixByteOrder = selectObjectFrom.byteOrder();
			final int prefix = reader.getInteger(prefixSize, prefixByteOrder)
				.intValue();

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

	boolean hasSelectAlternatives(){
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
