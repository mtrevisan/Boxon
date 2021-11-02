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

import io.github.mtrevisan.boxon.annotations.configurations.ConfigurationEnum;
import io.github.mtrevisan.boxon.annotations.configurations.ConfigurationField;
import io.github.mtrevisan.boxon.external.BitReader;
import io.github.mtrevisan.boxon.external.BitWriter;
import io.github.mtrevisan.boxon.internal.JavaHelper;

import java.lang.annotation.Annotation;
import java.lang.reflect.Array;


final class CodecConfigurationField implements CodecInterface<ConfigurationField>{

	@Override
	public Object decode(final BitReader reader, final Annotation annotation, final Object rootObject){
		throw new UnsupportedOperationException("Cannot decode this type of annotation: " + getClass().getSimpleName());
	}

	@Override
	public void encode(final BitWriter writer, final Annotation annotation, final Object fieldType, final Object value){
		final ConfigurationField binding = extractBinding(annotation);

		Object val = value;
		if(String.class.isAssignableFrom(value.getClass()))
			val = JavaHelper.getValue((Class<?>)fieldType, (String)value);

		if(value.getClass().isEnum())
			val = ((ConfigurationEnum)value).getCode();
		else if(value.getClass().isArray()){
			int compositeEnumValue = 0;
			for(int i = 0; i < Array.getLength(value); i ++)
				compositeEnumValue |= ((ConfigurationEnum)Array.get(value, i)).getCode();
			val = compositeEnumValue;
		}
		//TODO write hex if fieldType requires it
		//TODO
		System.out.println();
//		CodecHelper.validateData(binding.validator(), value);
//
//		final Charset charset = Charset.forName(binding.charset());
//
//		final Class<? extends Converter<?, ?>> chosenConverter = CodecHelper.chooseConverter(binding.selectConverterFrom(),
//			binding.converter(), rootObject);
//		final String text = CodecHelper.converterEncode(chosenConverter, value);
//
//		writer.putText(text, charset);
		writer.putByte(binding.terminator());
	}

}
