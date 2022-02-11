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

import java.lang.reflect.Method;
import java.util.Map;


/**
 * Builder for the common data used by the {@link Parser}, {@link Descriptor}, {@link Composer}, and {@link Configurator} classes.
 */
public final class BoxonCoreBuilder{

	private final BoxonCore core;

	private EventListener eventListener;


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
		this.eventListener = eventListener;

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
		core.addToContext(key, value);

		return this;
	}

	/**
	 * Loads the context for the {@link Evaluator}.
	 *
	 * @param context	The context map.
	 * @return	This instance, used for chaining.
	 */
	public BoxonCoreBuilder withContext(final Map<String, Object> context){
		core.withContext(context);

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
	 * @param method	The method.
	 * @return	This instance, used for chaining.
	 */
	public BoxonCoreBuilder withContextFunction(final Method method){
		core.withContextFunction(method);

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
	public BoxonCoreBuilder withContextFunction(final Class<?> cls, final String methodName, final Class<?>... parameterTypes)
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
	public BoxonCoreBuilder withDefaultCodecs(){
		core.withDefaultCodecs();

		return this;
	}

	/**
	 * Loads all the codecs that extends {@link CodecInterface}.
	 *
	 * @param basePackageClasses	Classes to be used ase starting point from which to load codecs.
	 * @return	This instance, used for chaining.
	 */
	public BoxonCoreBuilder withCodecs(final Class<?>... basePackageClasses){
		core.withCodecs(basePackageClasses);

		return this;
	}

	/**
	 * Loads all the codecs that extends {@link CodecInterface}.
	 *
	 * @param codecs	The list of codecs to be loaded.
	 * @return	This instance, used for chaining.
	 */
	public BoxonCoreBuilder withCodecs(final CodecInterface<?>... codecs){
		core.withCodecs(codecs);

		return this;
	}


	/**
	 * Loads all the default protocol classes annotated with {@link MessageHeader}.
	 *
	 * @return	This instance, used for chaining.
	 * @throws AnnotationException	If an annotation is not well formatted.
	 * @throws TemplateException	If a template is not well formatted.
	 */
	public BoxonCoreBuilder withDefaultTemplates() throws AnnotationException, TemplateException{
		core.withDefaultTemplates();

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
	public BoxonCoreBuilder withTemplates(final Class<?>... basePackageClasses) throws AnnotationException, TemplateException{
		core.withTemplates(basePackageClasses);

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
	public BoxonCoreBuilder withTemplate(final Class<?> templateClass) throws AnnotationException, TemplateException{
		core.withTemplate(templateClass);

		return this;
	}


	/**
	 * Loads all the protocol classes annotated with {@link ConfigurationHeader}.
	 *
	 * @return	This instance, used for chaining.
	 * @throws AnnotationException	If an annotation is not well formatted.
	 * @throws ConfigurationException	If a configuration is not well formatted.
	 */
	public BoxonCoreBuilder withDefaultConfigurations() throws AnnotationException, ConfigurationException{
		core.withDefaultConfigurations();

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
	public BoxonCoreBuilder withConfigurations(final Class<?>... basePackageClasses) throws AnnotationException, ConfigurationException{
		core.withConfigurations(basePackageClasses);

		return this;
	}


	public BoxonCore create(){
		if(eventListener != null)
			core.withEventListener(eventListener);

		//TODO execute all the methods
//		public BoxonCoreBuilder addToContext(final String key, final Object value);
//		public BoxonCoreBuilder withContext(final Map<String, Object> context);
//		public BoxonCoreBuilder withContextFunction(final Method method);
//		public BoxonCoreBuilder withContextFunction(final Class<?> cls, final String methodName, final Class<?>... parameterTypes);

//		public BoxonCoreBuilder withDefaultCodecs();
//		public BoxonCoreBuilder withCodecs(final Class<?>... basePackageClasses);
//		public BoxonCoreBuilder withCodecs(final CodecInterface<?>... codecs);

//		public BoxonCoreBuilder withDefaultTemplates();
//		public BoxonCoreBuilder withTemplates(final Class<?>... basePackageClasses);
//		public BoxonCoreBuilder withTemplate(final Class<?> templateClass);

//		public BoxonCoreBuilder withDefaultConfigurations();
//		public BoxonCoreBuilder withConfigurations(final Class<?>... basePackageClasses);

		return core;
	}

}
