/*
 * Copyright (c) 2024 Mauro Trevisan
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
package io.github.mtrevisan.boxon.core.codecs.behaviors;

import io.github.mtrevisan.boxon.annotations.bindings.ByteOrder;
import io.github.mtrevisan.boxon.annotations.bindings.ConverterChoices;
import io.github.mtrevisan.boxon.annotations.converters.Converter;
import io.github.mtrevisan.boxon.annotations.validators.Validator;
import io.github.mtrevisan.boxon.core.helpers.BitSetHelper;
import io.github.mtrevisan.boxon.core.helpers.CodecHelper;
import io.github.mtrevisan.boxon.core.helpers.DataTypeCaster;
import io.github.mtrevisan.boxon.io.BitReaderInterface;
import io.github.mtrevisan.boxon.io.BitWriterInterface;

import java.math.BigInteger;
import java.util.BitSet;


/**
 * Represents the behavior for handling integer values in a {@link BitSet}.
 * <p>
 * This class extends {@link BitSetBehavior} and adds support for specifying the byte order of the integer values.
 * </p>
 */
public final class IntegerBehavior extends BitSetBehavior{

	private final ByteOrder byteOrder;


	IntegerBehavior(final int size, final ByteOrder byteOrder, final ConverterChoices converterChoices,
			final Class<? extends Converter<?, ?>> defaultConverter, final Class<? extends Validator<?>> validator){
		super(size, converterChoices, defaultConverter, validator);

		this.byteOrder = byteOrder;
	}


	@Override
	public Object createArray(final int arraySize){
		return CodecHelper.createArray(BigInteger.class, arraySize);
	}

	@Override
	public Object readValue(final BitReaderInterface reader){
		return reader.readBigInteger(size, byteOrder);
	}

	@Override
	public void writeValue(final BitWriterInterface writer, final Object value){
		final BigInteger v = DataTypeCaster.reinterpretToBigInteger((Number)value);
		final BitSet bitmap = BitSetHelper.createBitSet(size, v, byteOrder);

		writer.writeBitSet(bitmap, size);
	}

}
