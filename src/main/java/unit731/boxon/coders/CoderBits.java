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
package unit731.boxon.coders;

import unit731.boxon.annotations.BindBits;
import unit731.boxon.annotations.ByteOrder;
import unit731.boxon.helpers.BitSet;
import unit731.boxon.helpers.ByteHelper;

import java.lang.annotation.Annotation;


class CoderBits implements CoderInterface<BindBits>{

	@Override
	public Object decode(final BitBuffer reader, final Annotation annotation, final Object data){
		final BindBits binding = (BindBits)annotation;

		final int size = Evaluator.evaluateSize(binding.size(), data);
		final BitSet bits = reader.getBits(size);
		if(binding.byteOrder() == ByteOrder.LITTLE_ENDIAN)
			ByteHelper.reverseBits(bits, size);

		final Object value = CoderHelper.converterDecode(binding.converter(), bits);

		CoderHelper.validateData(binding.match(), binding.validator(), value);

		return value;
	}

	@Override
	public void encode(final BitWriter writer, final Annotation annotation, final Object data, final Object value){
		final BindBits binding = (BindBits)annotation;

		CoderHelper.validateData(binding.match(), binding.validator(), value);

		final BitSet bits = CoderHelper.converterEncode(binding.converter(), value);
		final int size = Evaluator.evaluateSize(binding.size(), data);
		if(binding.byteOrder() == ByteOrder.LITTLE_ENDIAN)
			ByteHelper.reverseBits(bits, size);

		writer.putBits(bits, size);
	}

	@Override
	public Class<BindBits> coderType(){
		return BindBits.class;
	}

}
