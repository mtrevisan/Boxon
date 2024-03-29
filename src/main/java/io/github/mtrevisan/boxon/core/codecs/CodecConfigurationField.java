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

import io.github.mtrevisan.boxon.annotations.configurations.ConfigurationField;
import io.github.mtrevisan.boxon.core.helpers.codecs.WriterManagerFactory;
import io.github.mtrevisan.boxon.core.helpers.codecs.WriterManagerInterface;
import io.github.mtrevisan.boxon.exceptions.CodecException;
import io.github.mtrevisan.boxon.io.BitReaderInterface;
import io.github.mtrevisan.boxon.io.BitWriterInterface;
import io.github.mtrevisan.boxon.io.CodecInterface;

import java.lang.annotation.Annotation;


final class CodecConfigurationField implements CodecInterface<ConfigurationField>{

	@Override
	public Object decode(final BitReaderInterface reader, final Annotation annotation, final Object rootObject){
		throw new UnsupportedOperationException("Cannot decode this type of annotation: " + getClass().getSimpleName());
	}

	@Override
	public void encode(final BitWriterInterface writer, final Annotation annotation, final Object fieldType, Object value)
			throws CodecException{
		final ConfigurationField binding = extractBinding(annotation);

		value = CodecHelper.interpretValue((Class<?>)fieldType, value);
		if(value != null){
			final WriterManagerInterface writerManager = WriterManagerFactory.buildManager(value.getClass(), writer, binding.radix(),
				binding.charset());
			if(writerManager == null)
				throw CodecException.create("Cannot handle this type of field: {}, please report to the developer",
					value.getClass().getSimpleName());

			writerManager.put(value);
		}

		final String terminator = binding.terminator();
		if(!terminator.isEmpty())
			writer.putText(terminator);
	}

}
