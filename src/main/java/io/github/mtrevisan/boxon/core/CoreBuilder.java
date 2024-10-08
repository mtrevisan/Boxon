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
import io.github.mtrevisan.boxon.core.helpers.MethodHelper;
import io.github.mtrevisan.boxon.exceptions.AnnotationException;
import io.github.mtrevisan.boxon.exceptions.BoxonException;
import io.github.mtrevisan.boxon.exceptions.CodecException;
import io.github.mtrevisan.boxon.exceptions.ConfigurationException;
import io.github.mtrevisan.boxon.exceptions.TemplateException;
import io.github.mtrevisan.boxon.helpers.JavaHelper;
import io.github.mtrevisan.boxon.io.AnnotationValidator;
import io.github.mtrevisan.boxon.io.Codec;
import io.github.mtrevisan.boxon.io.Evaluator;
import io.github.mtrevisan.boxon.logs.EventListener;

import java.lang.reflect.Method;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;


/**
 * Builder for the {@link Core common data} used by the {@link Parser}, {@link Describer}, {@link Composer}, and {@link Configurator}.
 */
@SuppressWarnings({"unused", "WeakerAccess"})
public final class CoreBuilder{

	private enum ConfigurationStep{
		EVENT_LISTENER,
		CONTEXT,
		CODEC,
		TEMPLATE,
		CONFIGURATION
	}

	@FunctionalInterface
	private interface RunnableThrowable{
		void execute() throws BoxonException;
	}


	private final Core core;

	private final Map<ConfigurationStep, List<RunnableThrowable>> calls = new EnumMap<>(ConfigurationStep.class);


	/**
	 * Create a core builder.
	 *
	 * @return	A core builder.
	 */
	public static CoreBuilder builder(){
		return new CoreBuilder();
	}


	private CoreBuilder(){
		core = Core.create();
	}


	/**
	 * Assign an event listener.
	 *
	 * @param eventListener	The event listener.
	 * @return	This instance, used for chaining.
	 */
	public CoreBuilder withEventListener(final EventListener eventListener){
		addMethod(ConfigurationStep.EVENT_LISTENER, () -> core.setEventListener(eventListener));

		return this;
	}


	/**
	 * Adds a key-value pair to the context of this evaluator.
	 *
	 * @param key	The key used to reference the value.
	 * @param value	The value.
	 * @return	This instance, used for chaining.
	 */
	public CoreBuilder withContext(final String key, final Object value){
		addMethod(ConfigurationStep.CONTEXT, () -> core.putToContext(key, value));

		return this;
	}

	/**
	 * Loads the context for the {@link Evaluator}.
	 *
	 * @param context	The context map.
	 * @return	This instance, used for chaining.
	 */
	public CoreBuilder withContext(final Map<String, Object> context){
		addMethod(ConfigurationStep.CONTEXT, () -> core.putToContext(context));

		return this;
	}

	/**
	 * Add a method to the context for the {@link Evaluator}.
	 *
	 * @param type	The class containing the method.
	 * @param methodName	The method name.
	 * @return	This instance, used for chaining.
	 * @throws NullPointerException	If {@code methodName} is {@code null}.
	 * @throws NoSuchMethodException	If a matching method is not found.
	 */
	public CoreBuilder withContext(final Class<?> type, final String methodName) throws NoSuchMethodException{
		final Method method = MethodHelper.getMethod(type, methodName, null);
		if(method == null)
			throw new NoSuchMethodException(JavaHelper.prettyPrintMethodName(type, methodName));

		return withContext(method);
	}

	/**
	 * Add a method to the context for the {@link Evaluator}.
	 *
	 * @param type	The class in which the method resides.
	 * @param methodName	The name of the method.
	 * @param parameterTypes	The parameter array.
	 * @return	This instance, used for chaining.
	 * @throws NoSuchMethodException	If a matching method is not found.
	 */
	public CoreBuilder withContext(final Class<?> type, final String methodName, final Class<?>... parameterTypes)
			throws NoSuchMethodException{
		final Method method = MethodHelper.getMethod(type, methodName, null, parameterTypes);
		if(method == null)
			throw new NoSuchMethodException(JavaHelper.prettyPrintMethodName(type, methodName, parameterTypes));

		return withContext(method);
	}

	/**
	 * Add a method to the context for the {@link Evaluator}.
	 *
	 * @param method	The method.
	 * @return	This instance, used for chaining.
	 */
	public CoreBuilder withContext(final Method method){
		addMethod(ConfigurationStep.CONTEXT, () -> core.putToContext(method));

		return this;
	}


	/**
	 * Loads all the default codecs.
	 *
	 * @return	This instance, used for chaining.
	 */
	public CoreBuilder withDefaultCodecs(){
		addMethod(ConfigurationStep.CODEC, core::useDefaultCodecs);

		return this;
	}

	/**
	 * Loads all the codecs that extends {@link Codec}.
	 *
	 * @param basePackageClasses	Classes to be used ase starting point from which to load codecs.
	 * @return	This instance, used for chaining.
	 */
	public CoreBuilder withCodecsFrom(final Class<?>... basePackageClasses){
		addMethod(ConfigurationStep.CODEC, () -> core.addCodecsFrom(basePackageClasses));

		return this;
	}

	/**
	 * Loads the given codec.
	 *
	 * @param codec	The codec to be loaded.
	 * @return	This instance, used for chaining.
	 */
	public CoreBuilder withCodec(final Codec codec){
		addMethod(ConfigurationStep.CODEC, () -> core.addCodec(codec));

		return this;
	}

	/**
	 * Loads the given codec.
	 *
	 * @param codec	The codec to be loaded.
	 * @param validator	The codec validator.
	 * @return	This instance, used for chaining.
	 */
	public CoreBuilder withCodec(final Codec codec, final AnnotationValidator validator){
		addMethod(ConfigurationStep.CODEC, () -> core.addCodec(codec, validator));

		return this;
	}

	/**
	 * Loads all the given codecs.
	 *
	 * @param codecs	The list of codecs to be loaded.
	 * @return	This instance, used for chaining.
	 */
	public CoreBuilder withCodecs(final Codec... codecs){
		addMethod(ConfigurationStep.CODEC, () -> core.addCodecs(codecs));

		return this;
	}


	/**
	 * Loads all the protocol classes annotated with {@link TemplateHeader}.
	 *
	 * @param basePackageClasses	Classes to be used ase starting point from which to load annotated classes.
	 * @return	This instance, used for chaining.
	 */
	public CoreBuilder withTemplatesFrom(final Class<?>... basePackageClasses){
		addMethod(ConfigurationStep.TEMPLATE, () -> core.addTemplatesFrom(basePackageClasses));

		return this;
	}

	/**
	 * Load the specified protocol class annotated with {@link TemplateHeader}.
	 *
	 * @param templateClass	Template class.
	 * @return	This instance, used for chaining.
	 */
	public CoreBuilder withTemplate(final Class<?> templateClass){
		addMethod(ConfigurationStep.TEMPLATE, () -> core.addTemplate(templateClass));

		return this;
	}


	/**
	 * Loads all the protocol classes annotated with {@link ConfigurationHeader}.
	 *
	 * @param basePackageClasses	Classes to be used ase starting point from which to load annotated classes.
	 * @return	This instance, used for chaining.
	 */
	public CoreBuilder withConfigurationsFrom(final Class<?>... basePackageClasses){
		addMethod(ConfigurationStep.CONFIGURATION, () -> core.addConfigurationsFrom(basePackageClasses));

		return this;
	}

	/**
	 * Load the specified protocol class annotated with {@link ConfigurationHeader}.
	 *
	 * @param configurationClass	Configuration class.
	 * @return	This instance, used for chaining.
	 */
	public CoreBuilder withConfiguration(final Class<?> configurationClass){
		addMethod(ConfigurationStep.CONFIGURATION, () -> core.addConfiguration(configurationClass));

		return this;
	}


	private void addMethod(final ConfigurationStep configurationStep, final RunnableThrowable runnable){
		calls.computeIfAbsent(configurationStep, k -> new CopyOnWriteArrayList<>())
			.add(runnable);
	}


	/**
	 * Create the common core data executing all the configuration commands called in the proper order.
	 *
	 * @return	{@link Core Core} data used by {@link Parser}, {@link Describer}, {@link Composer}, and {@link Configurator}.
	 * @throws AnnotationException	If an annotation error occurs.
	 * @throws CodecException	If a codec was already loaded.
	 * @throws TemplateException	If a template error occurs.
	 * @throws ConfigurationException	If a configuration error occurs.
	 * @deprecated	Use {@link #build()} instead.
	 */
	@Deprecated
	public Core create() throws BoxonException{
		return build();
	}

	/**
	 * Create the common core data executing all the configuration commands called in the proper order.
	 *
	 * @return	{@link Core Core} data used by {@link Parser}, {@link Describer}, {@link Composer}, and {@link Configurator}.
	 * @throws AnnotationException	If an annotation error occurs.
	 * @throws CodecException	If a codec was already loaded.
	 * @throws TemplateException	If a template error occurs.
	 * @throws ConfigurationException	If a configuration error occurs.
	 */
	public Core build() throws BoxonException{
		CodecLoader.clearCodecs();

		final ConfigurationStep[] values = ConfigurationStep.values();
		for(int i = 0, length = values.length; i < length; i ++){
			final List<RunnableThrowable> executors = calls.get(values[i]);

			executeCommands(executors);
		}

		return core;
	}

	private static void executeCommands(final List<RunnableThrowable> executors) throws BoxonException{
		for(int i = 0, length = JavaHelper.sizeOrZero(executors); i < length; i ++){
			final RunnableThrowable executor = executors.get(i);

			executor.execute();
		}
	}

}
