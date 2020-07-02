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
import unit731.boxon.helpers.ByteHelper;

import java.math.BigInteger;
import java.util.BitSet;


class CoderInteger implements CoderInterface<BindInteger>{

	@Override
	public Object decode(final MessageParser messageParser, final BitBuffer reader, final BindInteger annotation, final Object data){
		final int size = Evaluator.evaluate(annotation.size(), int.class, data);

		final Object value;
		if(annotation.allowPrimitive() && size < Long.SIZE){
			final long v = reader.getLong(size, annotation.byteOrder());

			value = CoderHelper.converterDecode(annotation.converter(), v);
		}
		else{
			final BigInteger v = reader.getBigInteger(size, annotation.byteOrder());

			value = CoderHelper.converterDecode(annotation.converter(), v);
		}

		CoderHelper.validateData(annotation.match(), annotation.validator(), value);

		return value;
	}

	@Override
	public void encode(final MessageParser messageParser, final BitWriter writer, final BindInteger annotation, final Object data,
			final Object value){
		CoderHelper.validateData(annotation.match(), annotation.validator(), value);

		final int size = Evaluator.evaluate(annotation.size(), int.class, data);

		final BigInteger v;
		if(annotation.allowPrimitive() && size < Long.SIZE){
			final long vv = CoderHelper.converterEncode(annotation.converter(), value);

			v = BigInteger.valueOf(Math.abs(vv));
			if(vv < 0)
				v.setBit(size);
		}
		else
			v = CoderHelper.converterEncode(annotation.converter(), value);

		final ByteOrder byteOrder = annotation.byteOrder();
		final BitSet bits = ByteHelper.bigIntegerToBitSet(v, size, byteOrder);

		writer.putBits(bits, size);
	}

	@Override
	public Class<BindInteger> coderType(){
		return BindInteger.class;
	}

}
