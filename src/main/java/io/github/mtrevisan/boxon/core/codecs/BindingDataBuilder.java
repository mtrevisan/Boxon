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
package io.github.mtrevisan.boxon.core.codecs;

import io.github.mtrevisan.boxon.annotations.bindings.BindArray;
import io.github.mtrevisan.boxon.annotations.bindings.BindArrayPrimitive;
import io.github.mtrevisan.boxon.annotations.bindings.BindBitSet;
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
import io.github.mtrevisan.boxon.annotations.converters.Converter;
import io.github.mtrevisan.boxon.annotations.validators.Validator;
import io.github.mtrevisan.boxon.helpers.Evaluator;


/**
 * Builder for the {@link BindingData binding data}.
 */
final class BindingDataBuilder{

	private BindingDataBuilder(){}


	/**
	 * Create a binding data structure for the {@link BindArray} annotation.
	 *
	 * @param annotation	The annotation.
	 * @param rootObject	The root object.
	 * @param evaluator	The evaluator.
	 * @return	The instance.
	 */
	static BindingData create(final BindArray annotation, final Object rootObject, final Evaluator evaluator){
		final ConverterChoices selectConverterFrom = annotation.selectConverterFrom();
		final Class<? extends Validator<?>> validator = annotation.validator();
		final Class<? extends Converter<?, ?>> converter = annotation.converter();
		final BindingData data = new BindingData(selectConverterFrom, validator, converter, rootObject, evaluator);
		data.setType(annotation.type());
		data.setSize(annotation.size());
		data.setSelectDefault(annotation.selectDefault());
		data.setSelectObjectFrom(annotation.selectFrom());
		return data;
	}

	/**
	 * Create a binding data structure for the {@link BindArrayPrimitive} annotation.
	 *
	 * @param annotation	The annotation.
	 * @param rootObject	The root object.
	 * @param evaluator	The evaluator.
	 * @return	The instance.
	 */
	static BindingData create(final BindArrayPrimitive annotation, final Object rootObject, final Evaluator evaluator){
		final ConverterChoices selectConverterFrom = annotation.selectConverterFrom();
		final Class<? extends Validator<?>> validator = annotation.validator();
		final Class<? extends Converter<?, ?>> converter = annotation.converter();
		final BindingData data = new BindingData(selectConverterFrom, validator, converter, rootObject, evaluator);
		data.setType(annotation.type());
		data.setSize(annotation.size());
		return data;
	}

	/**
	 * Create a binding data structure for the {@link BindBitSet} annotation.
	 *
	 * @param annotation	The annotation.
	 * @param rootObject	The root object.
	 * @param evaluator	The evaluator.
	 * @return	The instance.
	 */
	static BindingData create(final BindBitSet annotation, final Object rootObject, final Evaluator evaluator){
		final ConverterChoices selectConverterFrom = annotation.selectConverterFrom();
		final Class<? extends Validator<?>> validator = annotation.validator();
		final Class<? extends Converter<?, ?>> converter = annotation.converter();
		final BindingData data = new BindingData(selectConverterFrom, validator, converter, rootObject, evaluator);
		data.setSize(annotation.size());
		return data;
	}

	/**
	 * Create a binding data structure for the {@link BindByte} annotation.
	 *
	 * @param annotation	The annotation.
	 * @param rootObject	The root object.
	 * @param evaluator	The evaluator.
	 * @return	The instance.
	 */
	static BindingData create(final BindByte annotation, final Object rootObject, final Evaluator evaluator){
		final ConverterChoices selectConverterFrom = annotation.selectConverterFrom();
		final Class<? extends Validator<?>> validator = annotation.validator();
		final Class<? extends Converter<?, ?>> converter = annotation.converter();
		return new BindingData(selectConverterFrom, validator, converter, rootObject, evaluator);
	}

	/**
	 * Create a binding data structure for the {@link BindDouble} annotation.
	 *
	 * @param annotation	The annotation.
	 * @param rootObject	The root object.
	 * @param evaluator	The evaluator.
	 * @return	The instance.
	 */
	static BindingData create(final BindDouble annotation, final Object rootObject, final Evaluator evaluator){
		final ConverterChoices selectConverterFrom = annotation.selectConverterFrom();
		final Class<? extends Validator<?>> validator = annotation.validator();
		final Class<? extends Converter<?, ?>> converter = annotation.converter();
		return new BindingData(selectConverterFrom, validator, converter, rootObject, evaluator);
	}

	/**
	 * Create a binding data structure for the {@link BindFloat} annotation.
	 *
	 * @param annotation	The annotation.
	 * @param rootObject	The root object.
	 * @param evaluator	The evaluator.
	 * @return	The instance.
	 */
	static BindingData create(final BindFloat annotation, final Object rootObject, final Evaluator evaluator){
		final ConverterChoices selectConverterFrom = annotation.selectConverterFrom();
		final Class<? extends Validator<?>> validator = annotation.validator();
		final Class<? extends Converter<?, ?>> converter = annotation.converter();
		return new BindingData(selectConverterFrom, validator, converter, rootObject, evaluator);
	}

	/**
	 * Create a binding data structure for the {@link BindInt} annotation.
	 *
	 * @param annotation	The annotation.
	 * @param rootObject	The root object.
	 * @param evaluator	The evaluator.
	 * @return	The instance.
	 */
	static BindingData create(final BindInt annotation, final Object rootObject, final Evaluator evaluator){
		final ConverterChoices selectConverterFrom = annotation.selectConverterFrom();
		final Class<? extends Validator<?>> validator = annotation.validator();
		final Class<? extends Converter<?, ?>> converter = annotation.converter();
		return new BindingData(selectConverterFrom, validator, converter, rootObject, evaluator);
	}

	/**
	 * Create a binding data structure for the {@link BindInteger} annotation.
	 *
	 * @param annotation	The annotation.
	 * @param rootObject	The root object.
	 * @param evaluator	The evaluator.
	 * @return	The instance.
	 */
	static BindingData create(final BindInteger annotation, final Object rootObject, final Evaluator evaluator){
		final ConverterChoices selectConverterFrom = annotation.selectConverterFrom();
		final Class<? extends Validator<?>> validator = annotation.validator();
		final Class<? extends Converter<?, ?>> converter = annotation.converter();
		final BindingData data = new BindingData(selectConverterFrom, validator, converter, rootObject, evaluator);
		data.setSize(annotation.size());
		return data;
	}

	/**
	 * Create a binding data structure for the {@link BindLong} annotation.
	 *
	 * @param annotation	The annotation.
	 * @param rootObject	The root object.
	 * @param evaluator	The evaluator.
	 * @return	The instance.
	 */
	static BindingData create(final BindLong annotation, final Object rootObject, final Evaluator evaluator){
		final ConverterChoices selectConverterFrom = annotation.selectConverterFrom();
		final Class<? extends Validator<?>> validator = annotation.validator();
		final Class<? extends Converter<?, ?>> converter = annotation.converter();
		return new BindingData(selectConverterFrom, validator, converter, rootObject, evaluator);
	}

	/**
	 * Create a binding data structure for the {@link BindObject} annotation.
	 *
	 * @param annotation	The annotation.
	 * @param rootObject	The root object.
	 * @param evaluator	The evaluator.
	 * @return	The instance.
	 */
	static BindingData create(final BindObject annotation, final Object rootObject, final Evaluator evaluator){
		final ConverterChoices selectConverterFrom = annotation.selectConverterFrom();
		final Class<? extends Validator<?>> validator = annotation.validator();
		final Class<? extends Converter<?, ?>> converter = annotation.converter();
		final BindingData data = new BindingData(selectConverterFrom, validator, converter, rootObject, evaluator);
		data.setType(annotation.type());
		data.setSelectDefault(annotation.selectDefault());
		data.setSelectObjectFrom(annotation.selectFrom());
		return data;
	}

	/**
	 * Create a binding data structure for the {@link BindShort} annotation.
	 *
	 * @param annotation	The annotation.
	 * @param rootObject	The root object.
	 * @param evaluator	The evaluator.
	 * @return	The instance.
	 */
	static BindingData create(final BindShort annotation, final Object rootObject, final Evaluator evaluator){
		final ConverterChoices selectConverterFrom = annotation.selectConverterFrom();
		final Class<? extends Validator<?>> validator = annotation.validator();
		final Class<? extends Converter<?, ?>> converter = annotation.converter();
		return new BindingData(selectConverterFrom, validator, converter, rootObject, evaluator);
	}

	/**
	 * Create a binding data structure for the {@link BindString} annotation.
	 *
	 * @param annotation	The annotation.
	 * @param rootObject	The root object.
	 * @param evaluator	The evaluator.
	 * @return	The instance.
	 */
	static BindingData create(final BindString annotation, final Object rootObject, final Evaluator evaluator){
		final ConverterChoices selectConverterFrom = annotation.selectConverterFrom();
		final Class<? extends Validator<?>> validator = annotation.validator();
		final Class<? extends Converter<?, ?>> converter = annotation.converter();
		final BindingData data = new BindingData(selectConverterFrom, validator, converter, rootObject, evaluator);
		data.setSize(annotation.size());
		return data;
	}

	/**
	 * Create a binding data structure for the {@link BindStringTerminated} annotation.
	 *
	 * @param annotation	The annotation.
	 * @param rootObject	The root object.
	 * @param evaluator	The evaluator.
	 * @return	The instance.
	 */
	static BindingData create(final BindStringTerminated annotation, final Object rootObject, final Evaluator evaluator){
		final ConverterChoices selectConverterFrom = annotation.selectConverterFrom();
		final Class<? extends Validator<?>> validator = annotation.validator();
		final Class<? extends Converter<?, ?>> converter = annotation.converter();
		return new BindingData(selectConverterFrom, validator, converter, rootObject, evaluator);
	}

}
