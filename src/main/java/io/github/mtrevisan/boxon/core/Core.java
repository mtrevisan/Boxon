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
import io.github.mtrevisan.boxon.exceptions.CodecException;
import io.github.mtrevisan.boxon.exceptions.ConfigurationException;
import io.github.mtrevisan.boxon.exceptions.TemplateException;
import io.github.mtrevisan.boxon.io.AnnotationValidator;
import io.github.mtrevisan.boxon.io.Codec;
import io.github.mtrevisan.boxon.io.Evaluator;
import io.github.mtrevisan.boxon.logs.EventListener;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Objects;


/**
 * Common data used by {@link Parser}, {@link Describer}, {@link Composer}, and {@link Configurator}.
 */
@SuppressWarnings({"unused", "WeakerAccess"})
public final class Core{

	private static final Evaluator EVALUATOR = Evaluator.getInstance();

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
	 * Adds a key-value pair to the context of this evaluator.
	 *
	 * @param key	The key used to reference the value.
	 * @param value	The value.
	 */
	static void putToContext(final String key, final Object value){
		EVALUATOR.putToContext(key, value);
	}

	/**
	 * Loads the context for the {@link Evaluator}.
	 *
	 * @param context	The context map.
	 */
	static void putToContext(final Map<String, Object> context){
		Objects.requireNonNull(context, "Context cannot be null");

		for(final Map.Entry<String, Object> entry : context.entrySet())
			EVALUATOR.putToContext(entry.getKey(), entry.getValue());
	}

	/**
	 * Add a method to the context for the {@link Evaluator}.
	 *
	 * @param method	The method.
	 */
	static void putToContext(final Method method){
		EVALUATOR.putToContext(method);
	}

	/**
	 * Remove a key-value pair to the context of this evaluator.
	 *
	 * @param key	The key used to reference the value.
	 */
	static void removeFromContext(final String key){
		EVALUATOR.removeFromContext(key);
	}

	/**
	 * Remove a method to the context of this evaluator.
	 *
	 * @param method	The method.
	 */
	static void removeFromContext(final Method method){
		EVALUATOR.removeFromContext(method);
	}

	/**
	 * Clear the context for the {@link Evaluator}.
	 */
	static void clearContext(){
		EVALUATOR.clearContext();
	}

	static Map<String, Object> getContext(){
		return EVALUATOR.getContext();
	}


	/**
	 * Loads all the default codecs.
	 */
	static void useDefaultCodecs(){
		CodecLoader.loadDefaultCodecs();
	}

	/**
	 * Loads all the codecs that extends {@link Codec}.
	 *
	 * @param basePackageClasses	Classes to be used ase starting point from which to load codecs.
	 * @throws CodecException	If a codec was already loaded.
	 */
	static void addCodecsFrom(final Class<?>... basePackageClasses) throws CodecException{
		CodecLoader.loadCodecsFrom(basePackageClasses);
	}

	/**
	 * Loads the given codec.
	 *
	 * @param codec	The codec to be loaded.
	 * @throws CodecException	If the codec was already loaded.
	 */
	static void addCodec(final Codec codec) throws CodecException{
		CodecLoader.addCodec(codec);
	}

	/**
	 * Loads the given codec.
	 *
	 * @param codec	The codec to be loaded.
	 * @param validator	The codec validator.
	 * @throws CodecException	If the codec was already loaded.
	 */
	static void addCodec(final Codec codec, final AnnotationValidator validator) throws CodecException{
		CodecLoader.addCodec(codec, validator);
	}

	/**
	 * Loads all the given codecs.
	 *
	 * @param codecs	The list of codecs to be loaded.
	 * @throws CodecException	If the codec was already loaded.
	 */
	static void addCodecs(final Codec... codecs) throws CodecException{
		CodecLoader.addCodecs(codecs);
	}


	/**
	 * Loads all the protocol classes annotated with {@link TemplateHeader}.
	 *
	 * @param basePackageClasses	Classes to be used ase starting point from which to load annotated classes.
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
	 * @param basePackageClasses	Classes to be used ase starting point from which to load annotated classes.
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


	public EventListener getEventListener(){
		return eventListener;
	}

}
