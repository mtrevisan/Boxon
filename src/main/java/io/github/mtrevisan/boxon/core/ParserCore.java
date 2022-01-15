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
import io.github.mtrevisan.boxon.codecs.TemplateParserCore;
import io.github.mtrevisan.boxon.codecs.TemplateParserInterface;
import io.github.mtrevisan.boxon.exceptions.AnnotationException;
import io.github.mtrevisan.boxon.exceptions.ConfigurationException;
import io.github.mtrevisan.boxon.exceptions.TemplateException;
import io.github.mtrevisan.boxon.external.codecs.CodecInterface;
import io.github.mtrevisan.boxon.external.logs.EventListener;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Objects;


@SuppressWarnings({"WeakerAccess", "unused"})
public final class ParserCore{

	private final LoaderCodec loaderCodec;

	private final Evaluator evaluator = Evaluator.create();

	private final TemplateParserCore templateParserCore;
	private final TemplateParser templateParser;
	private final ConfigurationParser configurationParser;


	/**
	 * Create an empty parser core (context, codecs and templates MUST BE manually loaded! -- templates MUST BE loaded AFTER
	 * the codecs).
	 *
	 * @return	A basic empty parser core.
	 */
	public static ParserCore create(){
		return create(null);
	}

	/**
	 * Create an empty parser core (context, codecs and templates MUST BE manually loaded! -- templates MUST BE loaded AFTER
	 * the codecs).
	 *
	 * @param eventListener	The event listener.
	 * @return	A basic empty parser core.
	 */
	public static ParserCore create(final EventListener eventListener){
		return new ParserCore(eventListener != null? eventListener: EventListener.getNoOpInstance());
	}


	private ParserCore(final EventListener eventListener){
		loaderCodec = LoaderCodec.create(eventListener);

		templateParserCore = TemplateParserCore.create(loaderCodec, eventListener, evaluator);
		templateParser = TemplateParser.create(templateParserCore);
		configurationParser = ConfigurationParser.create(loaderCodec, eventListener);
	}

	/**
	 * Adds a key-value pair to the context of this evaluator.
	 *
	 * @param key	The key used to reference the value.
	 * @param value	The value.
	 * @return	This instance, used for chaining.
	 */
	public ParserCore addToContext(final String key, final Object value){
		evaluator.addToContext(key, value);

		templateParser.addToBackupContext(key, value);

		return this;
	}

	/**
	 * Loads the context for the {@link Evaluator}.
	 *
	 * @param context	The context map.
	 * @return	This instance, used for chaining.
	 */
	public ParserCore withContext(final Map<String, Object> context){
		Objects.requireNonNull(context, "Context cannot be null");

		for(final Map.Entry<String, Object> entry : context.entrySet())
			evaluator.addToContext(entry.getKey(), entry.getValue());

		templateParser.addToBackupContext(context);

		return this;
	}

	/**
	 * Add a method to the context for the {@link Evaluator}.
	 *
	 * @param method	The method.
	 * @return	This instance, used for chaining.
	 */
	public ParserCore withContextFunction(final Method method){
		evaluator.addToContext(method);

		templateParser.addToBackupContext(method);

		return this;
	}

	/**
	 * Add a method to the context for the {@link Evaluator}.
	 *
	 * @param cls	The class in which the method resides.
	 * @param methodName	The name of the method.
	 * @param parameterTypes	The parameter array.
	 * @return	This instance, used for chaining.
	 * @throws NoSuchMethodException	If a matching method is not found.
	 */
	public ParserCore withContextFunction(final Class<?> cls, final String methodName, final Class<?>... parameterTypes)
			throws NoSuchMethodException{
		final Method method = cls.getDeclaredMethod(methodName, parameterTypes);
		return withContextFunction(method);
	}


	/**
	 * Loads all the default codecs that extends {@link CodecInterface}.
	 * <p>This method SHOULD BE called from a method inside a class that lies on a parent of all the codecs.</p>
	 *
	 * @return	This instance, used for chaining.
	 */
	public ParserCore withDefaultCodecs(){
		loaderCodec.loadDefaultCodecs();

		postProcessCodecs();

		return this;
	}

	/**
	 * Loads all the codecs that extends {@link CodecInterface}.
	 *
	 * @param basePackageClasses	Classes to be used ase starting point from which to load codecs.
	 * @return	This instance, used for chaining.
	 */
	public ParserCore withCodecs(final Class<?>... basePackageClasses){
		loaderCodec.loadCodecs(basePackageClasses);

		postProcessCodecs();

		return this;
	}

	/**
	 * Loads all the codecs that extends {@link CodecInterface}.
	 *
	 * @param codecs	The list of codecs to be loaded.
	 * @return	This instance, used for chaining.
	 */
	public ParserCore withCodecs(final CodecInterface<?>... codecs){
		loaderCodec.addCodecs(codecs);

		postProcessCodecs();

		return this;
	}

	private void postProcessCodecs(){
		loaderCodec.injectFieldInCodecs(TemplateParserInterface.class, templateParser);
		loaderCodec.injectFieldInCodecs(Evaluator.class, evaluator);
	}


	/**
	 * Loads all the default protocol classes annotated with {@link MessageHeader}.
	 *
	 * @return	This instance, used for chaining.
	 * @throws AnnotationException	If an annotation is not well formatted.
	 * @throws TemplateException	If a template is not well formatted.
	 */
	public ParserCore withDefaultTemplates() throws AnnotationException, TemplateException{
		templateParserCore.loadDefaultTemplates();

		return this;
	}

	/**
	 * Loads all the protocol classes annotated with {@link MessageHeader}.
	 *
	 * @param basePackageClasses	Classes to be used ase starting point from which to load annotated classes.
	 * @return	This instance, used for chaining.
	 * @throws AnnotationException	If an annotation is not well formatted.
	 * @throws TemplateException	If a template is not well formatted.
	 */
	public ParserCore withTemplates(final Class<?>... basePackageClasses) throws AnnotationException, TemplateException{
		templateParserCore.loadTemplates(basePackageClasses);

		return this;
	}

	/**
	 * Load the specified protocol class annotated with {@link MessageHeader}.
	 *
	 * @param templateClass	Template class.
	 * @return	This instance, used for chaining.
	 * @throws AnnotationException	If the annotation is not well formatted.
	 * @throws TemplateException	If the template is not well formatted.
	 */
	public ParserCore withTemplate(final Class<?> templateClass) throws AnnotationException, TemplateException{
		templateParserCore.loadTemplate(templateClass);

		return this;
	}


	/**
	 * Loads all the protocol classes annotated with {@link ConfigurationHeader}.
	 *
	 * @return	This instance, used for chaining.
	 * @throws AnnotationException	If an annotation is not well formatted.
	 * @throws ConfigurationException	If a configuration is not well formatted.
	 */
	public ParserCore withDefaultConfigurations() throws AnnotationException, ConfigurationException{
		configurationParser.loadDefaultConfigurations();

		return this;
	}

	/**
	 * Loads all the protocol classes annotated with {@link ConfigurationHeader}.
	 *
	 * @param basePackageClasses	Classes to be used ase starting point from which to load annotated classes.
	 * @return	This instance, used for chaining.
	 * @throws AnnotationException	If an annotation is not well formatted.
	 * @throws ConfigurationException	If a configuration is not well formatted.
	 */
	public ParserCore withConfigurations(final Class<?>... basePackageClasses) throws AnnotationException, ConfigurationException{
		configurationParser.loadConfigurations(basePackageClasses);

		return this;
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
