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

import io.github.mtrevisan.boxon.annotations.BindArray;
import io.github.mtrevisan.boxon.annotations.ObjectChoices;
import io.github.mtrevisan.boxon.annotations.converters.Converter;
import io.github.mtrevisan.boxon.annotations.exceptions.NoCodecException;
import io.github.mtrevisan.boxon.helpers.ReflectionHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Annotation;
import java.lang.reflect.Array;


final class CodecArray implements CodecInterface<BindArray>{

	private static final Logger LOGGER = LoggerFactory.getLogger(CodecArray.class);


	@SuppressWarnings("unused")
	private ProtocolMessageParser protocolMessageParser;


	@Override
	public final Object decode(final BitReader reader, final Annotation annotation, final Object rootObject){
		final BindArray binding = (BindArray)annotation;

		final int size = Evaluator.evaluateSize(binding.size(), rootObject);
		final ObjectChoices selectFrom = binding.selectFrom();

		final Object[] array = ReflectionHelper.createArray(binding.type(), size);
		if(selectFrom.alternatives().length > 0)
			decodeWithAlternatives(reader, array, selectFrom, rootObject);
		else
			decodeWithoutAlternatives(reader, array, binding.type());

		final Class<? extends Converter<?, ?>> chosenConverter = CodecHelper.chooseConverter(binding.selectConverterFrom(), binding.converter(), rootObject);
		final Object value = CodecHelper.converterDecode(chosenConverter, array);

		CodecHelper.validateData(binding.validator(), value);

		return value;
	}

	private void decodeWithAlternatives(final BitReader reader, final Object[] array, final ObjectChoices selectFrom, final Object rootObject){
		final boolean hasPrefix = (selectFrom.prefixSize() > 0);

		for(int i = 0; i < array.length; i ++){
			try{
				final ObjectChoices.ObjectChoice chosenAlternative = (hasPrefix?
					CodecHelper.chooseAlternativeWithPrefix(reader, selectFrom, rootObject):
					CodecHelper.chooseAlternativeWithoutPrefix(selectFrom, rootObject));

				//read object
				final ProtocolMessage<?> subProtocolMessage = ProtocolMessage.createFrom(chosenAlternative.type(), protocolMessageParser.loader);

				array[i] = protocolMessageParser.decode(subProtocolMessage, reader, rootObject);
			}
			catch(final NoCodecException e){
				LOGGER.warn(e.getMessage());
			}
		}
	}

	private void decodeWithoutAlternatives(final BitReader reader, final Object[] array, final Class<?> type){
		final ProtocolMessage<?> protocolMessage = ProtocolMessage.createFrom(type, protocolMessageParser.loader);

		for(int i = 0; i < array.length; i ++)
			array[i] = protocolMessageParser.decode(protocolMessage, reader, null);
	}

	@Override
	public final void encode(final BitWriter writer, final Annotation annotation, final Object rootObject, final Object value){
		final BindArray binding = (BindArray)annotation;

		CodecHelper.validateData(binding.validator(), value);

		final ObjectChoices selectFrom = binding.selectFrom();

		final Class<? extends Converter<?, ?>> chosenConverter = CodecHelper.chooseConverter(binding.selectConverterFrom(), binding.converter(), rootObject);
		final Object[] array = CodecHelper.converterEncode(chosenConverter, value);

		final int size = Evaluator.evaluateSize(binding.size(), rootObject);
		if(size != Array.getLength(array))
			throw new IllegalArgumentException("Size mismatch, expected " + size + ", got " + Array.getLength(value));

		if(selectFrom.alternatives().length > 0)
			encodeWithAlternatives(writer, array, selectFrom);
		else
			encodeWithoutAlternatives(writer, array, binding.type());
	}

	private void encodeWithAlternatives(final BitWriter writer, final Object[] array, final ObjectChoices selectFrom){
		final ObjectChoices.ObjectChoice[] alternatives = selectFrom.alternatives();
		for(int i = 0; i < array.length; i ++){
			final Object elem = array[i];
			final Class<?> type = elem.getClass();

			final ObjectChoices.ObjectChoice chosenAlternative = CodecHelper.chooseAlternative(alternatives, type);

			CodecHelper.writePrefix(writer, chosenAlternative, selectFrom);

			final ProtocolMessage<?> protocolMessage = ProtocolMessage.createFrom(type, protocolMessageParser.loader);

			protocolMessageParser.encode(protocolMessage, writer, null, elem);
		}
	}

	private void encodeWithoutAlternatives(final BitWriter writer, final Object[] array, final Class<?> type){
		final ProtocolMessage<?> protocolMessage = ProtocolMessage.createFrom(type, protocolMessageParser.loader);

		for(int i = 0; i < array.length; i ++)
			protocolMessageParser.encode(protocolMessage, writer, null, array[i]);
	}

	@Override
	public final Class<BindArray> codecType(){
		return BindArray.class;
	}

}
