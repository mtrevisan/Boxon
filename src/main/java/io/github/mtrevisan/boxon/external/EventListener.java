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
package io.github.mtrevisan.boxon.external;

import io.github.mtrevisan.boxon.core.CodecInterface;


public class EventListener{

	private static class SingletonHelper{
		private static final EventListener INSTANCE = new EventListener();
	}


	public static EventListener getNoOpInstance(){
		return EventListener.SingletonHelper.INSTANCE;
	}


	public void loadingCodecs(final Class<?>[] basePackageClasses){}

	public void loadingCodec(final CodecInterface<?>[] codecs){}

	public void loadedCodecs(final int count){}

	public void cannotCreateCodec(final String codecClassName){}


	public void loadingTemplates(final Class<?>[] basePackageClasses){}

	public void loadedTemplates(final int count){}

	public void cannotLoadTemplate(final String templateClassName, final Exception exception){}


	public void loadingConfigurations(final Class<?>[] basePackageClasses){}

	public void loadedConfigurations(final int count){}

	public void cannotLoadConfiguration(final String configurationClassName, final Exception exception){}


	public void uselessAlternative(final String defaultAlternativeClassName){}

	public void processingAlternative(final Exception exception){}


	public void decodingField(final String templateName, final String fieldName, final String bindingTypeName){}

	public void decodedField(final String templateName, final String fieldName, final Object value){}

	public void evaluatingField(final String templateName, final String fieldName){}

	public void evaluatedField(final String templateName, final String fieldName, final Object value){}

	public void writingField(final String templateName, final String fieldName, final String bindingTypeName){}

	public void writtenField(final String templateName, final String fieldName, final Object value){}

}
