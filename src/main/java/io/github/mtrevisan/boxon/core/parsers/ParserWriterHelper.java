/*
 * Copyright (c) 2020-2024 Mauro Trevisan
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
package io.github.mtrevisan.boxon.core.parsers;

import io.github.mtrevisan.boxon.core.codecs.LoaderCodecInterface;
import io.github.mtrevisan.boxon.exceptions.CodecException;
import io.github.mtrevisan.boxon.exceptions.FieldException;
import io.github.mtrevisan.boxon.helpers.CharsetHelper;
import io.github.mtrevisan.boxon.io.BitWriterInterface;
import io.github.mtrevisan.boxon.io.CodecInterface;
import io.github.mtrevisan.boxon.logs.EventListener;

import java.lang.annotation.Annotation;
import java.nio.charset.Charset;
import java.nio.charset.UnsupportedCharsetException;


final class ParserWriterHelper{

	private ParserWriterHelper(){}


	/**
	 * Writes the given affix to the writer using the specified charset.
	 *
	 * @param affix	The affix to be written.
	 * @param charsetName	The name of the charset to be used.
	 * @param writer	The BitWriterInterface where the affix is written.
	 * @throws UnsupportedCharsetException	If the specified charset name is not supported.
	 */
	static void writeAffix(final String affix, final String charsetName, final BitWriterInterface writer) throws UnsupportedCharsetException{
		if(!affix.isEmpty()){
			final Charset charset = CharsetHelper.lookup(charsetName);
			writer.putText(affix, charset);
		}
	}

	/**
	 * Encodes a field using the provided parser context, bit writer, and loader codec.
	 *
	 * @param parserContext	The parser context containing information about the field to encode.
	 * @param writer	The bit writer to write the encoded field to.
	 * @param loaderCodec	The loader codec used for encoding the field.
	 * @param eventListener	The event listener.
	 * @throws FieldException	If an error occurs during field encoding.
	 */
	static void encodeField(final ParserContext<?> parserContext, final BitWriterInterface writer, final LoaderCodecInterface loaderCodec,
			final EventListener eventListener) throws FieldException{
		final String className = parserContext.getClassName();
		final String fieldName = parserContext.getFieldName();
		final Annotation binding = parserContext.getBinding();
		final Annotation collectionBinding = parserContext.getCollectionBinding();

		final Class<? extends Annotation> annotationType = binding.annotationType();
		CodecInterface codec = loaderCodec.getCodec(annotationType);
		if(codec == null)
			//load default codec
			codec = loaderCodec.getCodec(void.class);
		if(codec == null)
			throw CodecException.createNoCodecForBinding(annotationType)
				.withClassNameAndFieldName(className, fieldName);

		eventListener.writingField(className, fieldName, annotationType.getSimpleName());

		try{
			//encode value from current object
			final Object value = parserContext.getFieldValue();
			//write value to raw message
			codec.encode(writer, binding, collectionBinding, parserContext.getRootObject(), value);

			eventListener.writtenField(className, fieldName, value);
		}
		catch(final FieldException fe){
			fe.withClassNameAndFieldName(className, fieldName);
			throw fe;
		}
		catch(final Exception e){
			throw FieldException.create(e)
				.withClassNameAndFieldName(className, fieldName);
		}
	}

}
