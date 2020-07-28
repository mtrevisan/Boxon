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

import io.github.mtrevisan.boxon.annotations.BindShort;
import io.github.mtrevisan.boxon.annotations.converters.Converter;

import java.lang.annotation.Annotation;


final class CodecShort implements CodecInterface<BindShort>{

	@Override
	public final Object decode(final BitReader reader, final Annotation annotation, final Object data){
		final BindShort binding = (BindShort)annotation;

		final short v = reader.getShort(binding.byteOrder());

		@SuppressWarnings("rawtypes")
		final Class<? extends Converter> chosenConverter = CodecHelper.chooseConverter(binding.selectConverterFrom(), binding.converter(), data);
		final Object value = CodecHelper.converterDecode(chosenConverter, v);

		CodecHelper.validateData(binding.match(), binding.validator(), value);

		return value;
	}

	@Override
	public final void encode(final BitWriter writer, final Annotation annotation, final Object data, final Object value){
		final BindShort binding = (BindShort)annotation;

		CodecHelper.validateData(binding.match(), binding.validator(), value);

		@SuppressWarnings("rawtypes")
		final Class<? extends Converter> chosenConverter = CodecHelper.chooseConverter(binding.selectConverterFrom(), binding.converter(), data);
		final short v = CodecHelper.converterEncode(chosenConverter, value);

		writer.putShort(v, binding.byteOrder());
	}

	@Override
	public final Class<BindShort> codecType(){
		return BindShort.class;
	}

}
