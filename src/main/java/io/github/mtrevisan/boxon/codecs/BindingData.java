/**
 * Copyright (c) 2020-2021 Mauro Trevisan
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
import io.github.mtrevisan.boxon.codecs.managers.ContextHelper;
import io.github.mtrevisan.boxon.exceptions.CodecException;
import io.github.mtrevisan.boxon.external.codecs.BitReader;
import io.github.mtrevisan.boxon.external.codecs.ByteOrder;

import java.lang.annotation.Annotation;


/** Data associated to an annotated field. */
public final class BindingData<T extends Annotation>{

	private static final ObjectChoices.ObjectChoice EMPTY_CHOICE = new NullObjectChoice();


	public final Class<T> annotation;

	private Class<?> type;
	private String size;
	private Class<?> selectDefault;
	private ObjectChoices selectObjectFrom;
	private final ConverterChoices selectConverterFrom;
	private final Class<? extends Validator<?>> validator;
	private final Class<? extends Converter<?, ?>> defaultConverter;

	private final Object rootObject;
	private final Evaluator evaluator;


	public static BindingData<BindArray> create(final BindArray annotation, final Object rootObject, final Evaluator evaluator){
		final ConverterChoices selectConverterFrom = annotation.selectConverterFrom();
		final Class<? extends Validator<?>> validator = annotation.validator();
		final Class<? extends Converter<?, ?>> converter = annotation.converter();
		final BindingData<BindArray> data = new BindingData<>(BindArray.class, selectConverterFrom, validator, converter, rootObject,
			evaluator);
		data.type = annotation.type();
		data.size = annotation.size();
		data.selectDefault = annotation.selectDefault();
		data.selectObjectFrom = annotation.selectFrom();
		return data;
	}

	public static BindingData<BindArrayPrimitive> create(final BindArrayPrimitive annotation, final Object rootObject,
			final Evaluator evaluator){
		final ConverterChoices selectConverterFrom = annotation.selectConverterFrom();
		final Class<? extends Validator<?>> validator = annotation.validator();
		final Class<? extends Converter<?, ?>> converter = annotation.converter();
		final BindingData<BindArrayPrimitive> data = new BindingData<>(BindArrayPrimitive.class, selectConverterFrom, validator, converter,
			rootObject, evaluator);
		data.type = annotation.type();
		data.size = annotation.size();
		return data;
	}

	public static BindingData<BindBits> create(final BindBits annotation, final Object rootObject, final Evaluator evaluator){
		final ConverterChoices selectConverterFrom = annotation.selectConverterFrom();
		final Class<? extends Validator<?>> validator = annotation.validator();
		final Class<? extends Converter<?, ?>> converter = annotation.converter();
		final BindingData<BindBits> data = new BindingData<>(BindBits.class, selectConverterFrom, validator, converter, rootObject,
			evaluator);
		data.size = annotation.size();
		return data;
	}

	public static BindingData<BindByte> create(final BindByte annotation, final Object rootObject, final Evaluator evaluator){
		final ConverterChoices selectConverterFrom = annotation.selectConverterFrom();
		final Class<? extends Validator<?>> validator = annotation.validator();
		final Class<? extends Converter<?, ?>> converter = annotation.converter();
		return new BindingData<>(BindByte.class, selectConverterFrom, validator, converter, rootObject, evaluator);
	}

	public static BindingData<BindDouble> create(final BindDouble annotation, final Object rootObject, final Evaluator evaluator){
		final ConverterChoices selectConverterFrom = annotation.selectConverterFrom();
		final Class<? extends Validator<?>> validator = annotation.validator();
		final Class<? extends Converter<?, ?>> converter = annotation.converter();
		return new BindingData<>(BindDouble.class, selectConverterFrom, validator, converter, rootObject, evaluator);
	}

	public static BindingData<BindFloat> create(final BindFloat annotation, final Object rootObject, final Evaluator evaluator){
		final ConverterChoices selectConverterFrom = annotation.selectConverterFrom();
		final Class<? extends Validator<?>> validator = annotation.validator();
		final Class<? extends Converter<?, ?>> converter = annotation.converter();
		return new BindingData<>(BindFloat.class, selectConverterFrom, validator, converter, rootObject, evaluator);
	}

	public static BindingData<BindInt> create(final BindInt annotation, final Object rootObject, final Evaluator evaluator){
		final ConverterChoices selectConverterFrom = annotation.selectConverterFrom();
		final Class<? extends Validator<?>> validator = annotation.validator();
		final Class<? extends Converter<?, ?>> converter = annotation.converter();
		return new BindingData<>(BindInt.class, selectConverterFrom, validator, converter, rootObject, evaluator);
	}

	public static BindingData<BindInteger> create(final BindInteger annotation, final Object rootObject, final Evaluator evaluator){
		final ConverterChoices selectConverterFrom = annotation.selectConverterFrom();
		final Class<? extends Validator<?>> validator = annotation.validator();
		final Class<? extends Converter<?, ?>> converter = annotation.converter();
		final BindingData<BindInteger> data = new BindingData<>(BindInteger.class, selectConverterFrom, validator, converter, rootObject,
			evaluator);
		data.size = annotation.size();
		return data;
	}

	public static BindingData<BindLong> create(final BindLong annotation, final Object rootObject, final Evaluator evaluator){
		final ConverterChoices selectConverterFrom = annotation.selectConverterFrom();
		final Class<? extends Validator<?>> validator = annotation.validator();
		final Class<? extends Converter<?, ?>> converter = annotation.converter();
		return new BindingData<>(BindLong.class, selectConverterFrom, validator, converter, rootObject, evaluator);
	}

	public static BindingData<BindObject> create(final BindObject annotation, final Object rootObject, final Evaluator evaluator){
		final ConverterChoices selectConverterFrom = annotation.selectConverterFrom();
		final Class<? extends Validator<?>> validator = annotation.validator();
		final Class<? extends Converter<?, ?>> converter = annotation.converter();
		final BindingData<BindObject> data = new BindingData<>(BindObject.class, selectConverterFrom, validator, converter, rootObject,
			evaluator);
		data.type = annotation.type();
		data.selectDefault = annotation.selectDefault();
		data.selectObjectFrom = annotation.selectFrom();
		return data;
	}

	public static BindingData<BindShort> create(final BindShort annotation, final Object rootObject, final Evaluator evaluator){
		final ConverterChoices selectConverterFrom = annotation.selectConverterFrom();
		final Class<? extends Validator<?>> validator = annotation.validator();
		final Class<? extends Converter<?, ?>> converter = annotation.converter();
		return new BindingData<>(BindShort.class, selectConverterFrom, validator, converter, rootObject, evaluator);
	}

	public static BindingData<BindString> create(final BindString annotation, final Object rootObject, final Evaluator evaluator){
		final ConverterChoices selectConverterFrom = annotation.selectConverterFrom();
		final Class<? extends Validator<?>> validator = annotation.validator();
		final Class<? extends Converter<?, ?>> converter = annotation.converter();
		final BindingData<BindString> data = new BindingData<>(BindString.class, selectConverterFrom, validator, converter, rootObject,
			evaluator);
		data.size = annotation.size();
		return data;
	}

	public static BindingData<BindStringTerminated> create(final BindStringTerminated annotation, final Object rootObject,
			final Evaluator evaluator){
		final ConverterChoices selectConverterFrom = annotation.selectConverterFrom();
		final Class<? extends Validator<?>> validator = annotation.validator();
		final Class<? extends Converter<?, ?>> converter = annotation.converter();
		return new BindingData<>(BindStringTerminated.class, selectConverterFrom, validator, converter, rootObject, evaluator);
	}

	private BindingData(final Class<T> annotation, final ConverterChoices selectConverterFrom, final Class<? extends Validator<?>> validator,
			final Class<? extends Converter<?, ?>> defaultConverter, final Object rootObject, final Evaluator evaluator){
		this.annotation = annotation;

		this.selectConverterFrom = selectConverterFrom;
		this.validator = validator;
		this.defaultConverter = defaultConverter;

		this.rootObject = rootObject;
		this.evaluator = evaluator;
	}

	boolean hasSelectAlternatives(){
		return (selectObjectFrom.alternatives().length > 0);
	}

	void validate(final Object value){
		CodecHelper.validateData(validator, value);
	}

	void addToContext(final Object instance){
		evaluator.addToContext(ContextHelper.CONTEXT_SELF, instance);
	}

	int evaluateSize(){
		return evaluator.evaluateSize(size, rootObject);
	}

	Class<?> chooseAlternativeType(final BitReader reader) throws CodecException{
		Class<?> chosenAlternativeType = type;
		if(hasSelectAlternatives()){
			if(selectObjectFrom.prefixSize() > 0){
				final int prefixSize = selectObjectFrom.prefixSize();
				final ByteOrder prefixByteOrder = selectObjectFrom.byteOrder();
				final int prefix = reader.getInteger(prefixSize, prefixByteOrder)
					.intValue();

				evaluator.addToContext(ContextHelper.CONTEXT_CHOICE_PREFIX, prefix);
			}

			final ObjectChoices.ObjectChoice[] alternatives = selectObjectFrom.alternatives();
			final ObjectChoices.ObjectChoice chosenAlternative = chooseAlternative(alternatives);
			chosenAlternativeType = (!isEmptyChoice(chosenAlternative)
				? chosenAlternative.type()
				: selectDefault);

			if(chosenAlternativeType == void.class)
				throw CodecException.create("Cannot find a valid codec from given alternatives for {}",
					rootObject.getClass().getSimpleName());
		}
		return chosenAlternativeType;
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
