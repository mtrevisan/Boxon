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

import io.github.mtrevisan.boxon.annotations.configurations.CompositeConfigurationField;
import io.github.mtrevisan.boxon.annotations.configurations.ConfigurationField;
import io.github.mtrevisan.boxon.exceptions.ConfigurationException;
import io.github.mtrevisan.boxon.external.BitReader;
import io.github.mtrevisan.boxon.external.BitWriter;
import io.github.mtrevisan.boxon.internal.JavaHelper;
import io.github.mtrevisan.boxon.internal.ParserDataType;

import java.lang.annotation.Annotation;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;


final class CodecCompositeConfigurationField implements CodecInterface<CompositeConfigurationField>{

	@Override
	public Object decode(final BitReader reader, final Annotation annotation, final Object rootObject){
		throw new UnsupportedOperationException("Cannot decode this type of annotation: " + getClass().getSimpleName());
	}

	@Override
	public void encode(final BitWriter writer, final Annotation annotation, final Object fieldType, final Object value)
			throws ConfigurationException{
		final CompositeConfigurationField binding = extractBinding(annotation);

		String charsetName = null;
		final ConfigurationField[] compositeFields = binding.value();
		for(int j = 0; charsetName == null && j < compositeFields.length; j ++)
			charsetName = compositeFields[j].charset();
		final Charset charset = (charsetName != null? Charset.forName(charsetName): StandardCharsets.UTF_8);

		Object val = value;
		if(String.class.isInstance(value))
			val = JavaHelper.getValue((Class<?>)fieldType, (String)value);

		if(val != null){
			if(String.class.isInstance(value))
				writer.putText((String)val, charset);
			else
				throw ConfigurationException.create("Cannot handle this type of field: {}, please report to the developer",
					ParserDataType.toObjectiveTypeOrSelf(val.getClass()));
		}

		if(!binding.terminator().isEmpty())
			writer.putText(binding.terminator(), StandardCharsets.UTF_8);
	}

}
