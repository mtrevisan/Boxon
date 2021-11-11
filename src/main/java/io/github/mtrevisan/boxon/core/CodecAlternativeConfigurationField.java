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
package io.github.mtrevisan.boxon.core;

import io.github.mtrevisan.boxon.annotations.configurations.AlternativeConfigurationField;
import io.github.mtrevisan.boxon.exceptions.ConfigurationException;
import io.github.mtrevisan.boxon.external.BitReader;
import io.github.mtrevisan.boxon.external.BitWriter;
import io.github.mtrevisan.boxon.external.ByteOrder;
import io.github.mtrevisan.boxon.internal.ParserDataType;

import java.lang.annotation.Annotation;
import java.nio.charset.Charset;


final class CodecAlternativeConfigurationField implements CodecInterface<AlternativeConfigurationField>{

	@Override
	public Object decode(final BitReader reader, final Annotation annotation, final Object rootObject){
		throw new UnsupportedOperationException("Cannot decode this type of annotation: " + getClass().getSimpleName());
	}

	@Override
	public void encode(final BitWriter writer, final Annotation annotation, final Object fieldType, Object value)
			throws ConfigurationException{
		final AlternativeConfigurationField binding = extractBinding(annotation);
		final Charset charset = Charset.forName(binding.charset());

		value = CodecHelper.interpretValue(value, (Class<?>)fieldType);
		if(value != null){
			if(String.class.isInstance(value))
				writer.putText((String)value, charset);
			else{
				final Class<?> fieldClass = ParserDataType.toObjectiveTypeOrSelf(value.getClass());
				if(fieldClass == Float.class)
					writer.putFloat((Float)value, ByteOrder.BIG_ENDIAN);
				else if(fieldClass == Double.class)
					writer.putDouble((Double)value, ByteOrder.BIG_ENDIAN);
				else if(Number.class.isAssignableFrom(fieldClass)){
					value = Long.toString(((Number)value).longValue(), binding.radix());
					writer.putText((String)value, charset);
				}
				else
					throw ConfigurationException.create("Cannot handle this type of field: {}, please report to the developer",
						fieldClass);
			}
		}
	}

}
