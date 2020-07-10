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

import unit731.boxon.annotations.BindInteger;
import unit731.boxon.annotations.ByteOrder;
import unit731.boxon.helpers.BitSet;
import unit731.boxon.helpers.ByteHelper;

import java.lang.annotation.Annotation;
import java.math.BigInteger;


@SuppressWarnings("unused")
final class CoderInteger implements CoderInterface<BindInteger>{

	@Override
	public final Object decode(final BitBuffer reader, final Annotation annotation, final Object data){
		final BindInteger binding = (BindInteger)annotation;

		final int size = Evaluator.evaluateSize(binding.size(), data);

		final Object value;
		if(binding.allowPrimitive() && size < Long.SIZE){
			final long v = reader.getLong(size, binding.byteOrder());

			value = CoderHelper.converterDecode(binding.converter(), v);
		}
		else{
			final BigInteger v = reader.getBigInteger(size, binding.byteOrder());

			value = CoderHelper.converterDecode(binding.converter(), v);
		}

		CoderHelper.validateData(binding.match(), binding.validator(), value);

		return value;
	}

	@Override
	public final void encode(final BitWriter writer, final Annotation annotation, final Object data, final Object value){
		final BindInteger binding = (BindInteger)annotation;

		CoderHelper.validateData(binding.match(), binding.validator(), value);

		final int size = Evaluator.evaluateSize(binding.size(), data);

		final BigInteger v;
		if(binding.allowPrimitive() && size < Long.SIZE){
			final long vv = CoderHelper.converterEncode(binding.converter(), value);

			v = BigInteger.valueOf(Math.abs(vv));
			if(vv < 0)
				//noinspection ResultOfMethodCallIgnored
				v.setBit(size);
		}
		else
			v = CoderHelper.converterEncode(binding.converter(), value);

		final ByteOrder byteOrder = binding.byteOrder();
		final BitSet bits = ByteHelper.toBits(v, size, byteOrder);

		writer.putBits(bits, size);
	}

	@Override
	public final Class<BindInteger> coderType(){
		return BindInteger.class;
	}

}
