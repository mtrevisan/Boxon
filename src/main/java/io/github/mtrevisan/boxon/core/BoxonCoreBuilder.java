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
import io.github.mtrevisan.boxon.external.io.CodecInterface;
import io.github.mtrevisan.boxon.external.logs.EventListener;
import io.github.mtrevisan.boxon.internal.JavaHelper;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * Builder for the common data used by the {@link Parser}, {@link Descriptor}, {@link Composer}, and {@link Configurator} classes.
 */
public final class BoxonCoreBuilder{

	private static Method METHOD_WITH_EVENT_LISTENER;
	private static Method METHOD_ADD_TO_CONTEXT;
	private static Method METHOD_WITH_CONTEXT;
	private static Method METHOD_WITH_CONTEXT_FUNCTION;
	private static Method METHOD_WITH_DEFAULT_CODECS;
	private static Method METHOD_WITH_CODECS;
	private static Method METHOD_WITH_CODECS_INTERFACE;
	private static Method METHOD_WITH_DEFAULT_TEMPLATES;
	private static Method METHOD_WITH_TEMPLATES;
	private static Method METHOD_WITH_TEMPLATE;
	private static Method METHOD_WITH_DEFAULT_CONFIGURATIONS;
	private static Method METHOD_WITH_CONFIGURATIONS;
	static{
		try{
			METHOD_WITH_EVENT_LISTENER = BoxonCore.class.getDeclaredMethod("withEventListener", EventListener.class);
			METHOD_ADD_TO_CONTEXT = BoxonCore.class.getDeclaredMethod("addToContext", String.class, Object.class);
			METHOD_WITH_CONTEXT = BoxonCore.class.getDeclaredMethod("withContext", Map.class);
			METHOD_WITH_CONTEXT_FUNCTION = BoxonCore.class.getDeclaredMethod("withContextFunction", Method.class);
			METHOD_WITH_DEFAULT_CODECS = BoxonCore.class.getDeclaredMethod("withDefaultCodecs");
			METHOD_WITH_CODECS = BoxonCore.class.getDeclaredMethod("withCodecs", Class[].class);
			METHOD_WITH_CODECS_INTERFACE = BoxonCore.class.getDeclaredMethod("withCodecs", CodecInterface[].class);
			METHOD_WITH_DEFAULT_TEMPLATES = BoxonCore.class.getDeclaredMethod("withDefaultTemplates");
			METHOD_WITH_TEMPLATES = BoxonCore.class.getDeclaredMethod("withTemplates", Class[].class);
			METHOD_WITH_TEMPLATE = BoxonCore.class.getDeclaredMethod("withTemplate", Class.class);
			METHOD_WITH_DEFAULT_CONFIGURATIONS = BoxonCore.class.getDeclaredMethod("withDefaultConfigurations");
			METHOD_WITH_CONFIGURATIONS = BoxonCore.class.getDeclaredMethod("withConfigurations", Class[].class);
		}
		catch(final NoSuchMethodException ignored){}
	}

	private enum CoreMethods{
		WITH_EVENT_LISTENER(METHOD_WITH_EVENT_LISTENER),
		ADD_TO_CONTEXT(METHOD_ADD_TO_CONTEXT),
		WITH_CONTEXT(METHOD_WITH_CONTEXT),
		WITH_CONTEXT_FUNCTION(METHOD_WITH_CONTEXT_FUNCTION),
		WITH_DEFAULT_CODECS(METHOD_WITH_DEFAULT_CODECS),
		WITH_CODECS(METHOD_WITH_CODECS),
		WITH_CODECS_INTERFACE(METHOD_WITH_CODECS_INTERFACE),
		WITH_DEFAULT_TEMPLATES(METHOD_WITH_DEFAULT_TEMPLATES),
		WITH_TEMPLATES(METHOD_WITH_TEMPLATES),
		WITH_TEMPLATE(METHOD_WITH_TEMPLATE),
		WITH_DEFAULT_CONFIGURATIONS(METHOD_WITH_DEFAULT_CONFIGURATIONS),
		WITH_CONFIGURATIONS(METHOD_WITH_CONFIGURATIONS);


		Method method;

		CoreMethods(final Method method){
			this.method = method;
		}
	}

	private final BoxonCore core;

	private final Map<Method, List<Object>> calls = new HashMap<>(0);


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
		calls.computeIfAbsent(METHOD_WITH_EVENT_LISTENER, k -> new ArrayList<>(1))
			.add(eventListener);

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
		calls.computeIfAbsent(METHOD_ADD_TO_CONTEXT, k -> new ArrayList<>(1))
			.add(new Object[]{key, value});

		return this;
	}

	/**
	 * Loads the context for the {@link Evaluator}.
	 *
	 * @param context	The context map.
	 * @return	This instance, used for chaining.
	 */
	public BoxonCoreBuilder withContext(final Map<String, Object> context){
		calls.computeIfAbsent(METHOD_WITH_CONTEXT, k -> new ArrayList<>(1))
			.add(context);

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
		calls.computeIfAbsent(METHOD_WITH_CONTEXT_FUNCTION, k -> new ArrayList<>(1))
			.add(method);

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
		calls.computeIfAbsent(METHOD_WITH_DEFAULT_CODECS, k -> new ArrayList<>(1))
			.add(new Object[0]);

		return this;
	}

	/**
	 * Loads all the codecs that extends {@link CodecInterface}.
	 *
	 * @param basePackageClasses	Classes to be used ase starting point from which to load codecs.
	 * @return	This instance, used for chaining.
	 */
	public BoxonCoreBuilder withCodecs(final Class<?>... basePackageClasses){
		calls.computeIfAbsent(METHOD_WITH_CODECS, k -> new ArrayList<>(1))
			.add(basePackageClasses);

		return this;
	}

	/**
	 * Loads all the codecs that extends {@link CodecInterface}.
	 *
	 * @param codecs	The list of codecs to be loaded.
	 * @return	This instance, used for chaining.
	 */
	public BoxonCoreBuilder withCodecs(final CodecInterface<?>... codecs){
		calls.computeIfAbsent(METHOD_WITH_CODECS_INTERFACE, k -> new ArrayList<>(1))
			.add(codecs);

		return this;
	}


	/**
	 * Loads all the default protocol classes annotated with {@link MessageHeader}.
	 *
	 * @return	This instance, used for chaining.
	 */
	public BoxonCoreBuilder withDefaultTemplates(){
		calls.computeIfAbsent(METHOD_WITH_DEFAULT_TEMPLATES, k -> new ArrayList<>(1))
			.add(new Object[0]);

		return this;
	}

	/**
	 * Loads all the protocol classes annotated with {@link MessageHeader}.
	 *
	 * @param basePackageClasses	Classes to be used ase starting point from which to load annotated classes.
	 * @return	This instance, used for chaining.
	 */
	public BoxonCoreBuilder withTemplates(final Class<?>... basePackageClasses){
		calls.computeIfAbsent(METHOD_WITH_TEMPLATES, k -> new ArrayList<>(1))
			.add(basePackageClasses);

		return this;
	}

	/**
	 * Load the specified protocol class annotated with {@link MessageHeader}.
	 *
	 * @param templateClass	Template class.
	 * @return	This instance, used for chaining.
	 */
	public BoxonCoreBuilder withTemplate(final Class<?> templateClass){
		calls.computeIfAbsent(METHOD_WITH_TEMPLATE, k -> new ArrayList<>(1))
			.add(templateClass);

		return this;
	}


	/**
	 * Loads all the protocol classes annotated with {@link ConfigurationHeader}.
	 *
	 * @return	This instance, used for chaining.
	 */
	public BoxonCoreBuilder withDefaultConfigurations(){
		calls.computeIfAbsent(METHOD_WITH_DEFAULT_CONFIGURATIONS, k -> new ArrayList<>(1))
			.add(new Object[0]);

		return this;
	}

	/**
	 * Loads all the protocol classes annotated with {@link ConfigurationHeader}.
	 *
	 * @param basePackageClasses	Classes to be used ase starting point from which to load annotated classes.
	 * @return	This instance, used for chaining.
	 */
	public BoxonCoreBuilder withConfigurations(final Class<?>... basePackageClasses){
		calls.computeIfAbsent(METHOD_WITH_CONFIGURATIONS, k -> new ArrayList<>(1))
			.add(basePackageClasses);

		return this;
	}


	public BoxonCore create(){
		try{
			for(final CoreMethods coreMethod : CoreMethods.values()){
				final Method method = coreMethod.method;
				List<Object> callbacks = calls.get(method);
				for(int i = 0; i < JavaHelper.lengthOrZero(callbacks); i++)
					method.invoke(core, callbacks.get(i));
			}
		}
		catch(final IllegalAccessException | InvocationTargetException ignored){}

		return core;
	}

}
