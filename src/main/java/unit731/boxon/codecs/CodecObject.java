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

import unit731.boxon.annotations.BindObject;
import unit731.boxon.annotations.ByteOrder;
import unit731.boxon.annotations.Choices;

import java.lang.annotation.Annotation;
import java.math.BigInteger;


@SuppressWarnings("unused")
final class CodecObject implements CodecInterface<BindObject>{

	@SuppressWarnings("unused")
	private MessageParser messageParser;


	@Override
	public final Object decode(final BitBuffer reader, final Annotation annotation, final Object data){
		final BindObject binding = (BindObject)annotation;

		Class<?> type = binding.type();
		final Choices selectFrom = binding.selectFrom();
		@SuppressWarnings("ConstantConditions")
		final Choices.Choice[] alternatives = (selectFrom != null? selectFrom.alternatives(): new Choices.Choice[0]);

		if(alternatives.length > 0){
			//read prefix
			final int prefixSize = selectFrom.prefixSize();
			final ByteOrder prefixByteOrder = selectFrom.byteOrder();

			final BigInteger prefix = reader.getBigInteger(prefixSize, prefixByteOrder);

			//choose class
			final Choices.Choice chosenAlternative = CodecHelper.chooseAlternative(alternatives, prefix.intValue(), data);
			type = chosenAlternative.type();
		}

		final ProtocolMessage<?> protocolMessage = ProtocolMessage.createFrom(type, messageParser.loader);

		final Object instance = messageParser.decode(protocolMessage, reader);

		final Object value = CodecHelper.converterDecode(binding.converter(), instance);

		CodecHelper.validateData(binding.validator(), value);

		return value;
	}

	@Override
	public final void encode(final BitWriter writer, final Annotation annotation, final Object data, final Object value){
		final BindObject binding = (BindObject)annotation;

		CodecHelper.validateData(binding.validator(), value);

		Class<?> type = binding.type();
		final Choices selectFrom = binding.selectFrom();
		@SuppressWarnings("ConstantConditions")
		final Choices.Choice[] alternatives = (selectFrom != null? selectFrom.alternatives(): new Choices.Choice[0]);
		if(alternatives.length > 0){
			type = value.getClass();

			//write prefix
			final Choices.Choice chosenAlternative = CodecHelper.chooseAlternative(alternatives, type);
			CodecHelper.writePrefix(writer, chosenAlternative, selectFrom);
		}

		final ProtocolMessage<?> protocolMessage = ProtocolMessage.createFrom(type, messageParser.loader);

		final Object array = CodecHelper.converterEncode(binding.converter(), value);

		messageParser.encode(protocolMessage, array, writer);
	}

	@Override
	public final Class<BindObject> codecType(){
		return BindObject.class;
	}

}
