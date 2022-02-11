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
package io.github.mtrevisan.boxon.core;

import io.github.mtrevisan.boxon.annotations.MessageHeader;
import io.github.mtrevisan.boxon.annotations.configurations.ConfigurationHeader;
import io.github.mtrevisan.boxon.codecs.ConfigurationParser;
import io.github.mtrevisan.boxon.codecs.Evaluator;
import io.github.mtrevisan.boxon.codecs.LoaderCodec;
import io.github.mtrevisan.boxon.codecs.TemplateParser;
import io.github.mtrevisan.boxon.codecs.TemplateParserInterface;
import io.github.mtrevisan.boxon.exceptions.AnnotationException;
import io.github.mtrevisan.boxon.exceptions.ConfigurationException;
import io.github.mtrevisan.boxon.exceptions.TemplateException;
import io.github.mtrevisan.boxon.external.io.CodecInterface;
import io.github.mtrevisan.boxon.external.logs.EventListener;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Objects;


/**
 * Common data used by {@link Parser}, {@link Descriptor}, {@link Composer}, and {@link Configurator}.
 */
@SuppressWarnings({"WeakerAccess", "unused"})
public final class BoxonCore{

	private final LoaderCodec loaderCodec;

	private final Evaluator evaluator;

	private final TemplateParser templateParser;
	private final ConfigurationParser configurationParser;


	/**
	 * Create an empty parser core.
	 * <p>
	 * Remember that templates and configurations MUST BE loaded AFTER the codecs!
	 * </p>
	 *
	 * @return	A basic empty parser core.
	 */
	static BoxonCore create(){
		return new BoxonCore();
	}


	private BoxonCore(){
		loaderCodec = LoaderCodec.create();

		evaluator = Evaluator.create();

		templateParser = TemplateParser.create(loaderCodec, evaluator);
		configurationParser = ConfigurationParser.create(loaderCodec);
	}


	/**
	 * Assign an event listener.
	 *
	 * @param eventListener	The event listener.
	 */
	void setEventListener(final EventListener eventListener){
		loaderCodec.withEventListener(eventListener);

		templateParser.withEventListener(eventListener);
		configurationParser.withEventListener(eventListener);
	}


	/**
	 * Adds a key-value pair to the context of this evaluator.
	 *
	 * @param key	The key used to reference the value.
	 * @param value	The value.
	 */
	void addToContext(final String key, final Object value){
		evaluator.addToContext(key, value);

		templateParser.addToBackupContext(key, value);
	}

	/**
	 * Loads the context for the {@link Evaluator}.
	 *
	 * @param context	The context map.
	 */
	void addContext(final Map<String, Object> context){
		Objects.requireNonNull(context, "Context cannot be null");

		for(final Map.Entry<String, Object> entry : context.entrySet())
			evaluator.addToContext(entry.getKey(), entry.getValue());

		templateParser.addToBackupContext(context);
	}

	/**
	 * Add a method to the context for the {@link Evaluator}.
	 *
	 * @param method	The method.
	 */
	void addContextFunction(final Method method){
		evaluator.addToContext(method);

		templateParser.addToBackupContext(method);
	}


	/**
	 * Loads all the default codecs that extends {@link CodecInterface}.
	 * <p>This method SHOULD BE called from a method inside a class that lies on a parent of all the codecs.</p>
	 */
	void useDefaultCodecs(){
		loaderCodec.loadDefaultCodecs();

		postProcessCodecs();
	}

	/**
	 * Loads all the codecs that extends {@link CodecInterface}.
	 *
	 * @param basePackageClasses	Classes to be used ase starting point from which to load codecs.
	 */
	void addCodecs(final Class<?>... basePackageClasses){
		loaderCodec.loadCodecs(basePackageClasses);

		postProcessCodecs();
	}

	/**
	 * Loads all the codecs that extends {@link CodecInterface}.
	 *
	 * @param codecs	The list of codecs to be loaded.
	 */
	void addCodecs(final CodecInterface<?>... codecs){
		loaderCodec.addCodecs(codecs);

		postProcessCodecs();
	}

	private void postProcessCodecs(){
		loaderCodec.injectFieldInCodecs(TemplateParserInterface.class, templateParser);
		loaderCodec.injectFieldInCodecs(Evaluator.class, evaluator);
	}


	/**
	 * Loads all the default protocol classes annotated with {@link MessageHeader}.
	 *
	 * @throws AnnotationException	If an annotation is not well formatted.
	 * @throws TemplateException	If a template is not well formatted.
	 */
	void useDefaultTemplates() throws AnnotationException, TemplateException{
		templateParser.withDefaultTemplates();
	}

	/**
	 * Loads all the protocol classes annotated with {@link MessageHeader}.
	 *
	 * @param basePackageClasses	Classes to be used ase starting point from which to load annotated classes.
	 * @throws AnnotationException	If an annotation is not well formatted.
	 * @throws TemplateException	If a template is not well formatted.
	 */
	void addTemplates(final Class<?>... basePackageClasses) throws AnnotationException, TemplateException{
		templateParser.withTemplates(basePackageClasses);
	}

	/**
	 * Load the specified protocol class annotated with {@link MessageHeader}.
	 *
	 * @param templateClass	Template class.
	 * @throws AnnotationException	If the annotation is not well formatted.
	 * @throws TemplateException	If the template is not well formatted.
	 */
	void addTemplate(final Class<?> templateClass) throws AnnotationException, TemplateException{
		templateParser.withTemplate(templateClass);
	}


	/**
	 * Loads all the protocol classes annotated with {@link ConfigurationHeader}.
	 *
	 * @throws AnnotationException	If an annotation is not well formatted.
	 * @throws ConfigurationException	If a configuration is not well formatted.
	 */
	void useDefaultConfigurations() throws AnnotationException, ConfigurationException{
		configurationParser.loadDefaultConfigurations();
	}

	/**
	 * Loads all the protocol classes annotated with {@link ConfigurationHeader}.
	 *
	 * @param basePackageClasses	Classes to be used ase starting point from which to load annotated classes.
	 * @throws AnnotationException	If an annotation is not well formatted.
	 * @throws ConfigurationException	If a configuration is not well formatted.
	 */
	void addConfigurations(final Class<?>... basePackageClasses) throws AnnotationException, ConfigurationException{
		configurationParser.loadConfigurations(basePackageClasses);
	}

	/**
	 * Load the specified protocol class annotated with {@link ConfigurationHeader}.
	 *
	 * @param configurationClass	Configuration class.
	 * @throws AnnotationException	If the annotation is not well formatted.
	 * @throws ConfigurationException	If a configuration is not well formatted.
	 */
	void addConfiguration(final Class<?> configurationClass) throws AnnotationException, ConfigurationException{
		configurationParser.withConfiguration(configurationClass);
	}


	Evaluator getEvaluator(){
		return evaluator;
	}

	TemplateParser getTemplateParser(){
		return templateParser;
	}

	ConfigurationParser getConfigurationParser(){
		return configurationParser;
	}

}
