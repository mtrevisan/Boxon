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

import io.github.mtrevisan.boxon.annotations.BindBits;
import io.github.mtrevisan.boxon.annotations.ByteOrder;
import io.github.mtrevisan.boxon.annotations.converters.Converter;
import io.github.mtrevisan.boxon.helpers.BitSet;

import java.lang.annotation.Annotation;


@SuppressWarnings("unused")
final class CodecBits implements CodecInterface<BindBits>{

	@Override
	public final Object decode(final BitReader reader, final Annotation annotation, final Object data){
		final BindBits binding = (BindBits)annotation;

		final int size = Evaluator.evaluateSize(binding.size(), data);
		final BitSet bits = reader.getBits(size);
		if(binding.byteOrder() == ByteOrder.LITTLE_ENDIAN)
			bits.reverseBits(size);

		final Class<? extends Converter> chosenConverter = CodecHelper.chooseConverter(binding.selectConverterFrom(), binding.converter(), data);
		final Object value = CodecHelper.converterDecode(chosenConverter, bits);

		CodecHelper.validateData(binding.match(), binding.validator(), value);

		return value;
	}

	@Override
	public final void encode(final BitWriter writer, final Annotation annotation, final Object data, final Object value){
		final BindBits binding = (BindBits)annotation;

		CodecHelper.validateData(binding.match(), binding.validator(), value);

		final Class<? extends Converter> chosenConverter = CodecHelper.chooseConverter(binding.selectConverterFrom(), binding.converter(), data);
		final BitSet bits = CodecHelper.converterEncode(chosenConverter, value);
		final int size = Evaluator.evaluateSize(binding.size(), data);
		if(binding.byteOrder() == ByteOrder.LITTLE_ENDIAN)
			bits.reverseBits( size);

		writer.putBits(bits, size);
	}

	@Override
	public final Class<BindBits> codecType(){
		return BindBits.class;
	}

}
