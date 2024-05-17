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
package io.github.mtrevisan.boxon.logs;

import io.github.mtrevisan.boxon.io.AnnotationValidator;


/**
 * An empty logger that logs nothing.
 * <p>
 * Used as a base class to construct custom loggers.
 * </p>
 */
public class EventListener{

	private static final class SingletonHelper{
		private static final EventListener INSTANCE = new EventListener();
	}


	EventListener(){}


	/**
	 * The singleton instance of this logger.
	 *
	 * @return	The instance of this logger.
	 */
	public static EventListener getNoOpInstance(){
		return EventListener.SingletonHelper.INSTANCE;
	}


	/**
	 * Called when about to loading some codecs through packages classes.
	 *
	 * @param basePackageClasses	List of base packages.
	 */
	public void loadingCodecsFrom(final Class<?>[] basePackageClasses){}

	/**
	 * Called when about to loading some codecs through codec classes.
	 *
	 * @param codecClasses	List of codec classes.
	 */
	public void loadingCodec(final Class<?>... codecClasses){}

	/**
	 * Called when about to loading some codecs through codec classes.
	 *
	 * @param codecClass	Codec class.
	 * @param validator	The codec validator.
	 */
	public void loadingCodec(final Class<?> codecClass, final AnnotationValidator validator){}

	/**
	 * Called when some codecs are loaded.
	 *
	 * @param count	The number of loaded codecs.
	 */
	public void loadedCodecs(final int count){}

	/**
	 * Called when a codec cannot be instantiated.
	 *
	 * @param codecClassName	Name of the codec.
	 */
	public void cannotCreateCodec(final String codecClassName){}


	/**
	 * Called when about to loading some templates through packages classes.
	 *
	 * @param basePackageClasses	List of base packages.
	 */
	public void loadingTemplatesFrom(final Class<?>[] basePackageClasses){}

	/**
	 * Called when about to loading a template through codec classes.
	 *
	 * @param templateClass	Template class.
	 */
	public void loadingTemplate(final Class<?> templateClass){}

	/**
	 * Called when some templates are loaded.
	 *
	 * @param count	The number of loaded templates.
	 */
	public void loadedTemplates(final int count){}

	/**
	 * Called when a template cannot be loaded.
	 *
	 * @param templateClassName	Name of the template.
	 * @param exception	The encountered exception.
	 */
	public void cannotLoadTemplate(final String templateClassName, final Exception exception){}


	/**
	 * Called when about to loading some configurations through packages classes.
	 *
	 * @param basePackageClasses	List of base packages.
	 */
	public void loadingConfigurationsFrom(final Class<?>[] basePackageClasses){}

	/**
	 * Called when some configurations are loaded.
	 *
	 * @param count	The number of loaded configurations.
	 */
	public void loadedConfigurations(final int count){}

	/**
	 * Called when about to loading a configuration through codec classes.
	 *
	 * @param configurationClass	Configuration class.
	 */
	public void loadingConfiguration(final Class<?> configurationClass){}

	/**
	 * Called when a configuration was loaded.
	 */
	public void loadedConfiguration(){}

	/**
	 * Called when a configuration cannot be loaded.
	 *
	 * @param configurationClassName	Name of the configuration.
	 * @param exception	The encountered exception.
	 */
	public void cannotLoadConfiguration(final String configurationClassName, final Exception exception){}


	/**
	 * Called when about to read a field.
	 *
	 * @param templateName	The template name.
	 * @param fieldName	The field name.
	 * @param bindingTypeName	The binding type name.
	 */
	public void readingField(final String templateName, final String fieldName, final String bindingTypeName){}

	/**
	 * Called when a field was read.
	 *
	 * @param templateName	The template name.
	 * @param fieldName	The field name.
	 * @param value	The value read.
	 */
	public void readField(final String templateName, final String fieldName, final Object value){}

	/**
	 * Called when about to evaluate a field.
	 *
	 * @param templateName	The template name.
	 * @param fieldName	The field name.
	 */
	public void evaluatingField(final String templateName, final String fieldName){}

	/**
	 * Called when a field was evaluated.
	 *
	 * @param templateName	The template name.
	 * @param fieldName	The field name.
	 * @param value	The value generated.
	 */
	public void evaluatedField(final String templateName, final String fieldName, final Object value){}

	/**
	 * Called when about to write a field.
	 *
	 * @param templateName	The template name.
	 * @param fieldName	The field name.
	 * @param bindingTypeName	The binding type name.
	 */
	public void writingField(final String templateName, final String fieldName, final String bindingTypeName){}

	/**
	 * Called when a field was written.
	 *
	 * @param templateName	The template name.
	 * @param fieldName	The field name.
	 * @param value	The value written.
	 */
	public void writtenField(final String templateName, final String fieldName, final Object value){}

}
