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
import io.github.mtrevisan.boxon.codecs.Evaluator;
import io.github.mtrevisan.boxon.exceptions.AnnotationException;
import io.github.mtrevisan.boxon.exceptions.ConfigurationException;
import io.github.mtrevisan.boxon.exceptions.TemplateException;
import io.github.mtrevisan.boxon.external.io.CodecInterface;
import io.github.mtrevisan.boxon.external.logs.EventListener;
import io.github.mtrevisan.boxon.internal.JavaHelper;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;


/**
 * Builder for the {@link BoxonCore common data} used by the {@link Parser}, {@link Descriptor}, {@link Composer}, and {@link Configurator}.
 */
public final class BoxonCoreBuilder{

	private enum ConfigurationStep{
		EVENT_LISTENER,
		CONTEXT,
		CODEC,
		TEMPLATE,
		CONFIGURATION
	}

	@FunctionalInterface
	private interface RunnableThrowable{
		void execute() throws AnnotationException, TemplateException, ConfigurationException;
	}


	private final BoxonCore core;

	private final Map<ConfigurationStep, List<RunnableThrowable>> calls = new EnumMap<>(ConfigurationStep.class);


	/**
	 * Create a core builder.
	 *
	 * @return	A core builder.
	 */
	public static BoxonCoreBuilder builder(){
		return new BoxonCoreBuilder();
	}


	private BoxonCoreBuilder(){
		core = BoxonCore.create();
	}


	/**
	 * Assign an event listener.
	 *
	 * @param eventListener	The event listener.
	 * @return	The current instance.
	 */
	public BoxonCoreBuilder withEventListener(final EventListener eventListener){
		addMethod(ConfigurationStep.EVENT_LISTENER, () -> core.withEventListener(eventListener));

		return this;
	}


	/**
	 * Adds a key-value pair to the context of this evaluator.
	 *
	 * @param key	The key used to reference the value.
	 * @param value	The value.
	 * @return	This instance, used for chaining.
	 */
	public BoxonCoreBuilder addToContext(final String key, final Object value){
		addMethod(ConfigurationStep.CONTEXT, () -> core.addToContext(key, value));

		return this;
	}

	/**
	 * Loads the context for the {@link Evaluator}.
	 *
	 * @param context	The context map.
	 * @return	This instance, used for chaining.
	 */
	public BoxonCoreBuilder withContext(final Map<String, Object> context){
		addMethod(ConfigurationStep.CONTEXT, () -> core.withContext(context));

		return this;
	}

	/**
	 * Add a method to the context for the {@link Evaluator}.
	 *
	 * @param type	The class containing the method.
	 * @param methodName	The method name.
	 * @return	This instance, used for chaining.
	 * @throws NoSuchMethodException	If a matching method is not found.
	 * @throws NullPointerException	If {@code methodName} is {@code null}.
	 */
	public BoxonCoreBuilder withContextFunction(final Class<?> type, final String methodName) throws NoSuchMethodException{
		return withContextFunction(type.getDeclaredMethod(methodName));
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
	public BoxonCoreBuilder withContextFunction(final Class<?> cls, final String methodName, final Class<?>... parameterTypes)
		throws NoSuchMethodException{
		final Method method = cls.getDeclaredMethod(methodName, parameterTypes);
		return withContextFunction(method);
	}

	/**
	 * Add a method to the context for the {@link Evaluator}.
	 *
	 * @param method	The method.
	 * @return	This instance, used for chaining.
	 */
	public BoxonCoreBuilder withContextFunction(final Method method){
		addMethod(ConfigurationStep.CONTEXT, () -> core.withContextFunction(method));

		return this;
	}


	/**
	 * Loads all the default codecs that extends {@link CodecInterface}.
	 * <p>This method SHOULD BE called from a method inside a class that lies on a parent of all the codecs.</p>
	 *
	 * @return	This instance, used for chaining.
	 */
	public BoxonCoreBuilder withDefaultCodecs(){
		addMethod(ConfigurationStep.CODEC, core::withDefaultCodecs);

		return this;
	}

	/**
	 * Loads all the codecs that extends {@link CodecInterface}.
	 *
	 * @param basePackageClasses	Classes to be used ase starting point from which to load codecs.
	 * @return	This instance, used for chaining.
	 */
	public BoxonCoreBuilder withCodecs(final Class<?>... basePackageClasses){
		addMethod(ConfigurationStep.CODEC, () -> core.withCodecs(basePackageClasses));

		return this;
	}

	/**
	 * Loads all the codecs that extends {@link CodecInterface}.
	 *
	 * @param codecs	The list of codecs to be loaded.
	 * @return	This instance, used for chaining.
	 */
	public BoxonCoreBuilder withCodecs(final CodecInterface<?>... codecs){
		addMethod(ConfigurationStep.CODEC, () -> core.withCodecs(codecs));

		return this;
	}


	/**
	 * Loads all the default protocol classes annotated with {@link MessageHeader}.
	 *
	 * @return	This instance, used for chaining.
	 */
	public BoxonCoreBuilder withDefaultTemplates(){
		addMethod(ConfigurationStep.TEMPLATE, core::withDefaultTemplates);

		return this;
	}

	/**
	 * Loads all the protocol classes annotated with {@link MessageHeader}.
	 *
	 * @param basePackageClasses	Classes to be used ase starting point from which to load annotated classes.
	 * @return	This instance, used for chaining.
	 */
	public BoxonCoreBuilder withTemplates(final Class<?>... basePackageClasses){
		addMethod(ConfigurationStep.TEMPLATE, () -> core.withTemplates(basePackageClasses));

		return this;
	}

	/**
	 * Load the specified protocol class annotated with {@link MessageHeader}.
	 *
	 * @param templateClass	Template class.
	 * @return	This instance, used for chaining.
	 */
	public BoxonCoreBuilder withTemplate(final Class<?> templateClass){
		addMethod(ConfigurationStep.TEMPLATE, () -> core.withTemplate(templateClass));

		return this;
	}


	/**
	 * Loads all the protocol classes annotated with {@link ConfigurationHeader}.
	 *
	 * @return	This instance, used for chaining.
	 */
	public BoxonCoreBuilder withDefaultConfigurations(){
		addMethod(ConfigurationStep.CONFIGURATION, core::withDefaultConfigurations);

		return this;
	}

	/**
	 * Loads all the protocol classes annotated with {@link ConfigurationHeader}.
	 *
	 * @param basePackageClasses	Classes to be used ase starting point from which to load annotated classes.
	 * @return	This instance, used for chaining.
	 */
	public BoxonCoreBuilder withConfigurations(final Class<?>... basePackageClasses){
		addMethod(ConfigurationStep.CONFIGURATION, () -> core.withConfigurations(basePackageClasses));

		return this;
	}

	private void addMethod(final ConfigurationStep configurationStep, final RunnableThrowable runnable){
		calls.computeIfAbsent(configurationStep, k -> new ArrayList<>(1))
			.add(runnable);
	}


	/**
	 * Create the common core data.
	 *
	 * @return	{@link BoxonCore Core} data used by {@link Parser}, {@link Descriptor}, {@link Composer}, and {@link Configurator}.
	 * @throws AnnotationException   If an annotation is not well formatted.
	 * @throws TemplateException   If a template is not well formatted.
	 * @throws ConfigurationException   If a configuration is not well formatted.
	 */
	public BoxonCore create() throws AnnotationException, TemplateException, ConfigurationException{
		for(final ConfigurationStep configurationStep : ConfigurationStep.values()){
			final List<RunnableThrowable> executors = calls.get(configurationStep);
			for(int i = 0; i < JavaHelper.lengthOrZero(executors); i ++){
				final RunnableThrowable executor = executors.get(i);
				executor.execute();
			}
		}

		return core;
	}

}
