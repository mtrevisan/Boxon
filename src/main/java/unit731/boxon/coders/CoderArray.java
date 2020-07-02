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

import unit731.boxon.annotations.BindArray;
import unit731.boxon.annotations.ByteOrder;
import unit731.boxon.annotations.Choices;
import unit731.boxon.helpers.ReflectionHelper;

import java.math.BigInteger;


class CoderArray implements CoderInterface<BindArray>{

	@Override
	public Object decode(final MessageParser messageParser, final BitBuffer reader, final BindArray annotation, final Object data){
		final int size = Evaluator.evaluate(annotation.size(), int.class, data);
		final Choices selectFrom = annotation.selectFrom();
		@SuppressWarnings("ConstantConditions")
		final Choices.Choice[] alternatives = (selectFrom != null? selectFrom.alternatives(): new Choices.Choice[0]);

		final Object[] array = ReflectionHelper.createArray(annotation.type(), size);
		if(alternatives.length > 0){
			//read prefix
			final int prefixSize = selectFrom.prefixSize();
			final ByteOrder prefixByteOrder = selectFrom.byteOrder();

			for(int i = 0; i < size; i ++){
				final BigInteger prefix = reader.getBigInteger(prefixSize, prefixByteOrder);

				//choose class
				final Choices.Choice chosenAlternative = CoderHelper.chooseAlternative(alternatives, prefix.intValue(), data);

				//read object
				final Codec<?> subCodec = Codec.createFrom(chosenAlternative.type());

				array[i] = messageParser.decode(subCodec, reader);
			}
		}
		else{
			final Codec<?> codec = Codec.createFrom(annotation.type());

			for(int i = 0; i < size; i ++)
				array[i] = messageParser.decode(codec, reader);
		}

		final Object value = CoderHelper.converterDecode(annotation.converter(), array);

		CoderHelper.validateData(annotation.validator(), value);

		return value;
	}

	@Override
	public void encode(final MessageParser messageParser, final BitWriter writer, final BindArray annotation, final Object data,
			final Object value){
		CoderHelper.validateData(annotation.validator(), value);

		final int size = Evaluator.evaluate(annotation.size(), int.class, data);
		final Choices selectFrom = annotation.selectFrom();
		@SuppressWarnings("ConstantConditions")
		final Choices.Choice[] alternatives = (selectFrom != null? selectFrom.alternatives(): new Choices.Choice[0]);

		final Object[] array = CoderHelper.converterEncode(annotation.converter(), value);

		if(alternatives.length > 0)
			for(int i = 0; i < size; i ++){
				final Class<?> cls = array[i].getClass();
				CoderHelper.writePrefix(writer, CoderHelper.chooseAlternative(alternatives, cls), selectFrom);

				final Codec<?> codec = Codec.createFrom(cls);

				messageParser.encode(codec, array[i], writer);
			}
		else{
			final Codec<?> codec = Codec.createFrom(annotation.type());

			for(int i = 0; i < size; i ++)
				messageParser.encode(codec, array[i], writer);
		}
	}

	@Override
	public Class<BindArray> coderType(){
		return BindArray.class;
	}

}
