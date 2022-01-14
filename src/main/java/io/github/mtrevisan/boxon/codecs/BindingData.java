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
import io.github.mtrevisan.boxon.annotations.converters.Converter;

import java.lang.annotation.Annotation;


/** Data associated to an annotated field. */
public final class BindingData<T extends Annotation>{

	public final Class<T> annotation;

	private final ConverterChoices selectConverterFrom;
	private final Class<? extends Converter<?, ?>> defaultConverter;


	public static BindingData<BindArray> create(final BindArray annotation){
		return new BindingData<>(BindArray.class, annotation.selectConverterFrom(), annotation.converter());
	}

	public static BindingData<BindArrayPrimitive> create(final BindArrayPrimitive annotation){
		return new BindingData<>(BindArrayPrimitive.class, annotation.selectConverterFrom(), annotation.converter());
	}

	public static BindingData<BindBits> create(final BindBits annotation){
		return new BindingData<>(BindBits.class, annotation.selectConverterFrom(), annotation.converter());
	}

	public static BindingData<BindByte> create(final BindByte annotation){
		return new BindingData<>(BindByte.class, annotation.selectConverterFrom(), annotation.converter());
	}

	public static BindingData<BindDouble> create(final BindDouble annotation){
		return new BindingData<>(BindDouble.class, annotation.selectConverterFrom(), annotation.converter());
	}

	public static BindingData<BindFloat> create(final BindFloat annotation){
		return new BindingData<>(BindFloat.class, annotation.selectConverterFrom(), annotation.converter());
	}

	public static BindingData<BindInt> create(final BindInt annotation){
		return new BindingData<>(BindInt.class, annotation.selectConverterFrom(), annotation.converter());
	}

	public static BindingData<BindInteger> create(final BindInteger annotation){
		return new BindingData<>(BindInteger.class, annotation.selectConverterFrom(), annotation.converter());
	}

	public static BindingData<BindLong> create(final BindLong annotation){
		return new BindingData<>(BindLong.class, annotation.selectConverterFrom(), annotation.converter());
	}

	public static BindingData<BindObject> create(final BindObject annotation){
		return new BindingData<>(BindObject.class, annotation.selectConverterFrom(), annotation.converter());
	}

	public static BindingData<BindShort> create(final BindShort annotation){
		return new BindingData<>(BindShort.class, annotation.selectConverterFrom(), annotation.converter());
	}

	public static BindingData<BindString> create(final BindString annotation){
		return new BindingData<>(BindString.class, annotation.selectConverterFrom(), annotation.converter());
	}

	public static BindingData<BindStringTerminated> create(final BindStringTerminated annotation){
		return new BindingData<>(BindStringTerminated.class, annotation.selectConverterFrom(), annotation.converter());
	}

	private BindingData(final Class<T> annotation, final ConverterChoices selectConverterFrom,
			final Class<? extends Converter<?, ?>> defaultConverter){
		this.annotation = annotation;

		this.selectConverterFrom = selectConverterFrom;
		this.defaultConverter = defaultConverter;
	}

	public Class<? extends Converter<?, ?>> getChosenConverter(final Object rootObject, final Evaluator evaluator){
		return CodecHelper.chooseConverter(selectConverterFrom, defaultConverter, rootObject, evaluator);
	}

}
