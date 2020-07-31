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

import io.github.mtrevisan.boxon.annotations.BindObject;
import io.github.mtrevisan.boxon.annotations.ByteOrder;
import io.github.mtrevisan.boxon.annotations.ObjectChoices;
import io.github.mtrevisan.boxon.annotations.converters.Converter;
import io.github.mtrevisan.boxon.annotations.exceptions.NoCodecException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Annotation;


final class CodecObject implements CodecInterface<BindObject>{

	private static final Logger LOGGER = LoggerFactory.getLogger(CodecObject.class);


	@SuppressWarnings("unused")
	private ProtocolMessageParser protocolMessageParser;


	@Override
	public final Object decode(final BitReader reader, final Annotation annotation, final Object rootObject){
		final BindObject binding = (BindObject)annotation;

		try{
			Class<?> type = binding.type();
			final ObjectChoices selectFrom = binding.selectFrom();
			final ObjectChoices.ObjectChoice[] alternatives = selectFrom.alternatives();
			if(alternatives.length > 0){
				//read prefix
				final int prefixSize = selectFrom.prefixSize();
				final ByteOrder prefixByteOrder = selectFrom.byteOrder();

				final ObjectChoices.ObjectChoice chosenAlternative = (prefixSize > 0?
					CodecHelper.chooseAlternativeWithPrefix(reader, prefixSize, prefixByteOrder, alternatives, rootObject):
					CodecHelper.chooseAlternativeWithoutPrefix(alternatives, rootObject));

				type = chosenAlternative.type();
			}

			final ProtocolMessage<?> protocolMessage = ProtocolMessage.createFrom(type, protocolMessageParser.loader);

			final Object instance = protocolMessageParser.decode(protocolMessage, reader, rootObject);
			Evaluator.addToContext(CodecHelper.CONTEXT_SELF, instance);

			final Class<? extends Converter<?, ?>> chosenConverter = CodecHelper.chooseConverter(binding.selectConverterFrom(), binding.converter(), rootObject);
			final Object value = CodecHelper.converterDecode(chosenConverter, instance);

			CodecHelper.validateData(binding.validator(), value);

			return value;
		}
		catch(final NoCodecException e){
			LOGGER.warn(e.getMessage());

			return null;
		}
	}

	@Override
	public final void encode(final BitWriter writer, final Annotation annotation, final Object rootObject, final Object value){
		final BindObject binding = (BindObject)annotation;

		CodecHelper.validateData(binding.validator(), value);

		Class<?> type = binding.type();
		final ObjectChoices selectFrom = binding.selectFrom();
		final ObjectChoices.ObjectChoice[] alternatives = selectFrom.alternatives();
		if(alternatives.length > 0){
			type = value.getClass();

			final ObjectChoices.ObjectChoice chosenAlternative = CodecHelper.chooseAlternative(alternatives, type);
			if(chosenAlternative == null)
				throw new IllegalArgumentException("Cannot find a valid codec for type " + type.getSimpleName());

			//write prefix
			CodecHelper.writePrefix(writer, chosenAlternative, selectFrom);
		}

		Evaluator.addToContext(CodecHelper.CONTEXT_SELF, value);

		final ProtocolMessage<?> protocolMessage = ProtocolMessage.createFrom(type, protocolMessageParser.loader);

		final Class<? extends Converter<?, ?>> chosenConverter = CodecHelper.chooseConverter(binding.selectConverterFrom(), binding.converter(), rootObject);
		final Object obj = CodecHelper.converterEncode(chosenConverter, value);

		protocolMessageParser.encode(protocolMessage, writer, rootObject, obj);
	}

	@Override
	public final Class<BindObject> codecType(){
		return BindObject.class;
	}

}
