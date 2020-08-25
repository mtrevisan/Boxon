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
package io.github.mtrevisan.boxon.internal.reflection.adapters;

import io.github.mtrevisan.boxon.internal.JavaHelper;
import org.slf4j.Logger;


public final class MetadataAdapterBuilder{

	private static final Logger LOGGER = JavaHelper.getLoggerFor(MetadataAdapterBuilder.class);

	private static MetadataAdapterInterface<?> INSTANCE;
	static{
		/**
		 * if javassist library exists in the classpath, this method returns {@link JavassistAdapter} otherwise defaults to {@link JavaReflectionAdapter}.
		 * <p>the {@link JavassistAdapter} is preferred in terms of performance and class loading.
		 */
		try{
			INSTANCE = new JavassistAdapter();
		}
		catch(final Throwable e){
			if(LOGGER != null)
				LOGGER.warn("could not create JavassistAdapter, using JavaReflectionAdapter", e);

			INSTANCE = new JavaReflectionAdapter();
		}
	}


	private MetadataAdapterBuilder(){}

	public static MetadataAdapterInterface<?> getMetadataAdapter(){
		return INSTANCE;
	}

}
