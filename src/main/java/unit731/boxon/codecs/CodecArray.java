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
package unit731.boxon.codecs;

import unit731.boxon.annotations.BindArray;
import unit731.boxon.annotations.ByteOrder;
import unit731.boxon.annotations.Choices;
import unit731.boxon.helpers.ReflectionHelper;

import java.lang.annotation.Annotation;
import java.math.BigInteger;


@SuppressWarnings("unused")
final class CodecArray implements CodecInterface<BindArray>{

	@SuppressWarnings("unused")
	private MessageParser messageParser;


	@Override
	public final Object decode(final BitBuffer reader, final Annotation annotation, final Object data){
		final BindArray binding = (BindArray)annotation;

		final int size = Evaluator.evaluateSize(binding.size(), data);
		final Choices selectFrom = binding.selectFrom();
		@SuppressWarnings("ConstantConditions")
		final Choices.Choice[] alternatives = (selectFrom != null? selectFrom.alternatives(): new Choices.Choice[0]);

		final Object[] array = ReflectionHelper.createArray(binding.type(), size);
		if(alternatives.length > 0){
			//read prefix
			final int prefixSize = selectFrom.prefixSize();
			final ByteOrder prefixByteOrder = selectFrom.byteOrder();

			for(int i = 0; i < size; i ++){
				final BigInteger prefix = reader.getBigInteger(prefixSize, prefixByteOrder);

				//choose class
				final Choices.Choice chosenAlternative = CodecHelper.chooseAlternative(alternatives, prefix.intValue(), data);

				//read object
				final ProtocolMessage<?> subProtocolMessage = ProtocolMessage.createFrom(chosenAlternative.type(), messageParser.loader);

				array[i] = messageParser.decode(subProtocolMessage, reader);
			}
		}
		else{
			final ProtocolMessage<?> protocolMessage = ProtocolMessage.createFrom(binding.type(), messageParser.loader);

			for(int i = 0; i < size; i ++)
				array[i] = messageParser.decode(protocolMessage, reader);
		}

		final Object value = CodecHelper.converterDecode(binding.converter(), array);

		CodecHelper.validateData(binding.validator(), value);

		return value;
	}

	@Override
	public final void encode(final BitWriter writer, final Annotation annotation, final Object data, final Object value){
		final BindArray binding = (BindArray)annotation;

		CodecHelper.validateData(binding.validator(), value);

		final int size = Evaluator.evaluateSize(binding.size(), data);
		final Choices selectFrom = binding.selectFrom();
		@SuppressWarnings("ConstantConditions")
		final Choices.Choice[] alternatives = (selectFrom != null? selectFrom.alternatives(): new Choices.Choice[0]);

		final Object[] array = CodecHelper.converterEncode(binding.converter(), value);

		if(alternatives.length > 0)
			for(int i = 0; i < size; i ++){
				final Class<?> cls = array[i].getClass();
				CodecHelper.writePrefix(writer, CodecHelper.chooseAlternative(alternatives, cls), selectFrom);

				final ProtocolMessage<?> protocolMessage = ProtocolMessage.createFrom(cls, messageParser.loader);

				messageParser.encode(protocolMessage, array[i], writer);
			}
		else{
			final ProtocolMessage<?> protocolMessage = ProtocolMessage.createFrom(binding.type(), messageParser.loader);

			for(int i = 0; i < size; i ++)
				messageParser.encode(protocolMessage, array[i], writer);
		}
	}

	@Override
	public final Class<BindArray> codecType(){
		return BindArray.class;
	}

}
