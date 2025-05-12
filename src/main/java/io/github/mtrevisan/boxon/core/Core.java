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
package io.github.mtrevisan.boxon.core;

import io.github.mtrevisan.boxon.annotations.TemplateHeader;
import io.github.mtrevisan.boxon.annotations.configurations.ConfigurationHeader;
import io.github.mtrevisan.boxon.core.codecs.CodecLoader;
import io.github.mtrevisan.boxon.core.parsers.ConfigurationParser;
import io.github.mtrevisan.boxon.core.parsers.TemplateParser;
import io.github.mtrevisan.boxon.exceptions.AnnotationException;
import io.github.mtrevisan.boxon.exceptions.ConfigurationException;
import io.github.mtrevisan.boxon.exceptions.TemplateException;
import io.github.mtrevisan.boxon.logs.EventListener;


/**
 * Common data used by {@link Parser}, {@link Describer}, {@link Composer}, and {@link Configurator}.
 */
@SuppressWarnings({"unused", "WeakerAccess"})
public final class Core{

	private final TemplateParser templateParser;
	private final ConfigurationParser configurationParser;

	private EventListener eventListener = EventListener.getNoOpInstance();


	/**
	 * Create an empty parser core.
	 * <p>
	 * Remember that templates and configurations MUST BE loaded AFTER the codecs!
	 * </p>
	 *
	 * @return	An empty core.
	 */
	static Core create(){
		return new Core();
	}


	private Core(){
		templateParser = TemplateParser.getInstance();
		templateParser.clear();

		configurationParser = ConfigurationParser.create();
	}


	/**
	 * Assign an event listener.
	 *
	 * @param eventListener	The event listener.
	 */
	void setEventListener(final EventListener eventListener){
		if(eventListener != null)
			this.eventListener = eventListener;

		CodecLoader.setEventListener(eventListener);

		templateParser.withEventListener(eventListener);
		configurationParser.withEventListener(eventListener);
	}


	/**
	 * Loads all the protocol classes annotated with {@link TemplateHeader}.
	 *
	 * @param basePackageClasses	Classes to be used as a starting point from which to load annotated classes.
	 * @throws AnnotationException	If an annotation error occurs.
	 * @throws TemplateException	If a template error occurs.
	 */
	void addTemplatesFrom(final Class<?>... basePackageClasses) throws AnnotationException, TemplateException{
		templateParser.withTemplatesFrom(basePackageClasses);
	}

	/**
	 * Load the specified protocol class annotated with {@link TemplateHeader}.
	 *
	 * @param templateClass	Template class.
	 * @throws AnnotationException	If an annotation error occurs.
	 * @throws TemplateException	If a template error occurs.
	 */
	void addTemplate(final Class<?> templateClass) throws AnnotationException, TemplateException{
		templateParser.withTemplate(templateClass);
	}


	/**
	 * Loads all the protocol classes annotated with {@link ConfigurationHeader}.
	 *
	 * @param basePackageClasses	Classes to be used as a starting point from which to load annotated classes.
	 * @throws AnnotationException	If an annotation error occurs.
	 * @throws ConfigurationException	If a configuration error occurs.
	 */
	void addConfigurationsFrom(final Class<?>... basePackageClasses) throws AnnotationException, ConfigurationException{
		configurationParser.loadConfigurationsFrom(basePackageClasses);
	}

	/**
	 * Load the specified protocol class annotated with {@link ConfigurationHeader}.
	 *
	 * @param configurationClass	Configuration class.
	 * @throws AnnotationException	If an annotation error occurs.
	 * @throws ConfigurationException	If a configuration error occurs.
	 */
	void addConfiguration(final Class<?> configurationClass) throws AnnotationException, ConfigurationException{
		configurationParser.withConfiguration(configurationClass);
	}


	TemplateParser getTemplateParser(){
		return templateParser;
	}

	ConfigurationParser getConfigurationParser(){
		return configurationParser;
	}


	/**
	 * Retrieves the event listener associated with the core.
	 *
	 * @return	The event listener instance.
	 */
	public EventListener getEventListener(){
		return eventListener;
	}

}
