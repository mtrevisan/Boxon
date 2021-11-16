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


public class EventListener{

	private static class SingletonHelper{
		private static final EventListener INSTANCE = new EventListener();
	}


	EventListener(){}

	public static EventListener getNoOpInstance(){
		return EventListener.SingletonHelper.INSTANCE;
	}


	@SuppressWarnings("unused")
	public void loadingCodecs(final Class<?>[] basePackageClasses){}

	@SuppressWarnings("unused")
	public void loadingCodec(final Class<?>[] codecClasses){}

	@SuppressWarnings("unused")
	public void loadedCodecs(final int count){}

	@SuppressWarnings("unused")
	public void cannotCreateCodec(final String codecClassName){}


	@SuppressWarnings("unused")
	public void loadingTemplates(final Class<?>[] basePackageClasses){}

	@SuppressWarnings("unused")
	public void loadedTemplates(final int count){}

	@SuppressWarnings("unused")
	public void cannotLoadTemplate(final String templateClassName, final Exception exception){}


	@SuppressWarnings("unused")
	public void loadingConfigurations(final Class<?>[] basePackageClasses){}

	@SuppressWarnings("unused")
	public void loadedConfigurations(final int count){}

	@SuppressWarnings("unused")
	public void cannotLoadConfiguration(final String configurationClassName, final Exception exception){}


	@SuppressWarnings("unused")
	public void uselessAlternative(final String defaultAlternativeClassName){}

	@SuppressWarnings("unused")
	public void processingAlternative(final Exception exception){}


	@SuppressWarnings("unused")
	public void decodingField(final String templateName, final String fieldName, final String bindingTypeName){}

	@SuppressWarnings("unused")
	public void decodedField(final String templateName, final String fieldName, final Object value){}

	@SuppressWarnings("unused")
	public void evaluatingField(final String templateName, final String fieldName){}

	@SuppressWarnings("unused")
	public void evaluatedField(final String templateName, final String fieldName, final Object value){}

	@SuppressWarnings("unused")
	public void writingField(final String templateName, final String fieldName, final String bindingTypeName){}

	@SuppressWarnings("unused")
	public void writtenField(final String templateName, final String fieldName, final Object value){}

}
