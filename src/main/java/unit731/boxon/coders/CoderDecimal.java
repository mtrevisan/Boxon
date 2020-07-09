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

import unit731.boxon.annotations.BindDecimal;

import java.lang.annotation.Annotation;
import java.math.BigDecimal;


@SuppressWarnings("unused")
class CoderDecimal implements CoderInterface<BindDecimal>{

	@Override
	public Object decode(final BitBuffer reader, final Annotation annotation, final Object data){
		final BindDecimal binding = (BindDecimal)annotation;

		final BigDecimal v = reader.getDecimal(binding.type(), binding.byteOrder());

		final Object value = CoderHelper.converterDecode(binding.converter(), v);

		CoderHelper.validateData(binding.match(), binding.validator(), value);

		return value;
	}

	@Override
	public void encode(final BitWriter writer, final Annotation annotation, final Object data, final Object value){
		final BindDecimal binding = (BindDecimal)annotation;

		CoderHelper.validateData(binding.match(), binding.validator(), value);

		final BigDecimal v = CoderHelper.converterEncode(binding.converter(), value);

		writer.putDecimal(v, binding.type(), binding.byteOrder());
	}

	@Override
	public Class<BindDecimal> coderType(){
		return BindDecimal.class;
	}

}
