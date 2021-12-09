/**
 * Copyright (c) 2020-2021 Mauro Trevisan
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

import io.github.mtrevisan.boxon.exceptions.CodecException;
import io.github.mtrevisan.boxon.exceptions.FieldException;
import io.github.mtrevisan.boxon.external.codecs.BitWriter;
import io.github.mtrevisan.boxon.external.codecs.CodecInterface;
import io.github.mtrevisan.boxon.external.logs.EventListener;
import io.github.mtrevisan.boxon.internal.Evaluator;

import java.lang.annotation.Annotation;
import java.nio.charset.Charset;


final class ParserHelper{

	private ParserHelper(){}

	static void writeAffix(final String affix, final String charsetName, final BitWriter writer){
		if(!affix.isEmpty()){
			final Charset charset = Charset.forName(charsetName);
			writer.putText(affix, charset);
		}
	}

	static void encodeField(final ParserContext<?> parserContext, final BitWriter writer, final LoaderCodecInterface loaderCodec,
			final EventListener eventListener, final Evaluator evaluator) throws FieldException{
		final Class<? extends Annotation> annotationType = parserContext.getBinding().annotationType();
		final CodecInterface<?> codec = loaderCodec.getCodec(annotationType);
		if(codec == null)
			throw CodecException.create("Cannot find codec for binding {}", annotationType.getSimpleName())
				.withClassNameAndFieldName(parserContext.getClassName(), parserContext.getFieldName());

		eventListener.writingField(parserContext.getClassName(), parserContext.getFieldName(), annotationType.getSimpleName());

		try{
			//encode value from current object
			final Object value = parserContext.getFieldValue();
			//write value to raw message
			codec.encode(writer, parserContext.getBinding(), parserContext.getRootObject(), value, evaluator);

			eventListener.writtenField(parserContext.getClassName(), parserContext.getFieldName(), value);
		}
		catch(final FieldException fe){
			fe.withClassNameAndFieldName(parserContext.getClassName(), parserContext.getFieldName());
			throw fe;
		}
		catch(final Exception e){
			throw FieldException.create(e)
				.withClassNameAndFieldName(parserContext.getClassName(), parserContext.getFieldName());
		}
	}

}
