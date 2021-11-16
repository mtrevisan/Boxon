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
package io.github.mtrevisan.boxon.core.codecs;

import io.github.mtrevisan.boxon.annotations.bindings.BindDecimal;
import io.github.mtrevisan.boxon.annotations.bindings.ConverterChoices;
import io.github.mtrevisan.boxon.annotations.converters.Converter;
import io.github.mtrevisan.boxon.core.CodecInterface;
import io.github.mtrevisan.boxon.exceptions.AnnotationException;
import io.github.mtrevisan.boxon.external.BitReader;
import io.github.mtrevisan.boxon.external.BitWriter;

import java.lang.annotation.Annotation;
import java.math.BigDecimal;


final class CodecDecimal implements CodecInterface<BindDecimal>{

	@Override
	public Object decode(final BitReader reader, final Annotation annotation, final Object rootObject) throws AnnotationException{
		final BindDecimal binding = extractBinding(annotation);

		final BigDecimal v = reader.getDecimal(binding.type(), binding.byteOrder());

		final ConverterChoices selectConverterFrom = binding.selectConverterFrom();
		final Class<? extends Converter<?, ?>> defaultConverter = binding.converter();
		final Class<? extends Converter<?, ?>> chosenConverter = CodecHelper.chooseConverter(selectConverterFrom, defaultConverter,
			rootObject);
		final Object value = CodecHelper.converterDecode(chosenConverter, v);

		CodecHelper.validateData(binding.validator(), value);

		return value;
	}

	@Override
	public void encode(final BitWriter writer, final Annotation annotation, final Object rootObject, final Object value)
			throws AnnotationException{
		final BindDecimal binding = extractBinding(annotation);

		CodecHelper.validateData(binding.validator(), value);

		final Class<? extends Converter<?, ?>> chosenConverter = CodecHelper.chooseConverter(binding.selectConverterFrom(),
			binding.converter(), rootObject);
		final BigDecimal v = CodecHelper.converterEncode(chosenConverter, value);

		writer.putDecimal(v, binding.type(), binding.byteOrder());
	}

}
