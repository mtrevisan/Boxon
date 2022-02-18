/*
 * Copyright (c) 2020-2022 Mauro Trevisan
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
package io.github.mtrevisan.boxon.core.managers.writer;

import io.github.mtrevisan.boxon.helpers.CharsetHelper;
import io.github.mtrevisan.boxon.io.BitWriterInterface;
import io.github.mtrevisan.boxon.io.ParserDataType;

import java.nio.charset.Charset;
import java.util.Map;
import java.util.function.Function;


/**
 * Factory for the {@link WriterManagerInterface writer manager}.
 */
public final class WriterManagerFactory{

	private static final Map<Class<?>, Function<BitWriterInterface, WriterManagerInterface>> MANAGERS_LEVEL1 = Map.of(
		Float.class, FloatWriterManager::new,
		Double.class, DoubleWriterManager::new
	);


	private WriterManagerFactory(){}


	/**
	 * Returns a {@link WriterManagerInterface writer manager} from the class type of the given value, filling it with the given writer,
	 * radix, and charset name parameters.
	 *
	 * @param valueClass	The class type of the value.
	 * @param writer	The writer to be injected into the resulting manager.
	 * @param radix	The (number) radix to be injected into the resulting manager.
	 * @param charsetName	The charset name to be injected into the resulting manager.
	 * @return	An instance of writer manager.
	 */
	public static WriterManagerInterface buildManager(final Class<?> valueClass, final BitWriterInterface writer, final int radix,
			final String charsetName){
		final Class<?> fieldClass = ParserDataType.toObjectiveTypeOrSelf(valueClass);
		final Function<BitWriterInterface, WriterManagerInterface> builder = MANAGERS_LEVEL1.get(fieldClass);

		WriterManagerInterface manager = null;
		if(builder != null)
			manager = builder.apply(writer);
		else if(Number.class.isAssignableFrom(fieldClass))
			manager = new NumberWriterManager(writer)
				.withRadix(radix);
		else if(String.class.isAssignableFrom(fieldClass)){
			final Charset charset = CharsetHelper.lookup(charsetName);
			manager = new StringWriterManager(writer)
				.withCharset(charset);
		}
		return manager;
	}

}
