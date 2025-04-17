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
package io.github.mtrevisan.boxon.core.codecs;

import io.github.mtrevisan.boxon.annotations.configurations.CompositeSubField;
import io.github.mtrevisan.boxon.exceptions.UnhandledFieldException;
import io.github.mtrevisan.boxon.io.BitReaderInterface;
import io.github.mtrevisan.boxon.io.BitWriterInterface;
import io.github.mtrevisan.boxon.io.Codec;

import java.lang.annotation.Annotation;
import java.nio.charset.Charset;


/**
 * This class provides the functionality for encoding and decoding fields annotated with {@link CompositeSubField}.
 * <p>
 * Note: This codec only supports encoding. The decode method throws an {@link UnsupportedOperationException}.
 * </p>
 */
final class CodecConfigurationSubField implements Codec{

	@Override
	public Class<? extends Annotation> annotationType(){
		return CompositeSubField.class;
	}

	@Override
	public Object decode(final BitReaderInterface reader, final Annotation annotation, final Annotation collectionBinding,
			final Object rootObject){
		throw createUnsupportedOperationException(annotationType());
	}

	@Override
	public void encode(final BitWriterInterface writer, final Annotation annotation, final Annotation collectionBinding,
			final Object charset, final Object value) throws UnhandledFieldException{
		if(!(value instanceof final String v))
			throw UnhandledFieldException.create(value);

		writer.writeText(v, (Charset)charset);
	}

}
