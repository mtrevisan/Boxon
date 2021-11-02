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
package io.github.mtrevisan.boxon.core;

import io.github.mtrevisan.boxon.external.EventListener;
import io.github.mtrevisan.boxon.internal.InjectEventListener;
import io.github.mtrevisan.boxon.internal.ReflectionHelper;
import io.github.mtrevisan.boxon.internal.ReflectiveClassLoader;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;


final class LoaderCodec{

	/**
	 * NOTE: all the methods listed below HAS TO HAVE this very same charset!
	 *
	 * @see io.github.mtrevisan.boxon.annotations.MessageHeader#charset()
	 * @see io.github.mtrevisan.boxon.annotations.bindings.BindString#charset()
	 * @see io.github.mtrevisan.boxon.annotations.bindings.BindStringTerminated#charset()
	 * @see io.github.mtrevisan.boxon.annotations.configurations.ConfigurationMessage#charset()
	 * @see io.github.mtrevisan.boxon.annotations.configurations.ConfigurationField#charset()
	 */
	public static final String CHARSET_DEFAULT = "UTF-8";


	@InjectEventListener
	private final EventListener eventListener;


	private final Map<Class<?>, CodecInterface<?>> codecs = new HashMap<>(0);


	LoaderCodec(final EventListener eventListener){
		this.eventListener = eventListener;

		injectEventListener();
	}

	private void injectEventListener(){
		final ReflectiveClassLoader reflectiveClassLoader = ReflectiveClassLoader.createFrom(CodecInterface.class);
		reflectiveClassLoader.scan(CodecInterface.class);
		final Collection<Class<?>> classes = reflectiveClassLoader.getImplementationsOf(CodecInterface.class);
		for(final Class<?> cl : classes)
			ReflectionHelper.setStaticFieldValue(cl, EventListener.class, eventListener);
	}

	/**
	 * Loads all the codecs that extends {@link CodecInterface}.
	 * <p>This method SHOULD BE called from a method inside a class that lies on a parent of all the codecs.</p>
	 */
	void loadDefaultCodecs(){
		loadCodecs(ReflectionHelper.extractCallerClasses());
	}

	/**
	 * Loads all the codecs that extends {@link CodecInterface}.
	 *
	 * @param basePackageClasses	Classes to be used ase starting point from which to load codecs.
	 */
	void loadCodecs(final Class<?>... basePackageClasses){
		eventListener.loadingCodecs(basePackageClasses);

		/** extract all classes that implements {@link CodecInterface}. */
		final Collection<Class<?>> derivedClasses = LoaderHelper.extractClasses(CodecInterface.class, basePackageClasses);
		final List<CodecInterface<?>> codecs = extractCodecs(derivedClasses);
		addCodecsInner(codecs);

		eventListener.loadedCodecs(codecs.size());
	}

	private List<CodecInterface<?>> extractCodecs(final Collection<Class<?>> derivedClasses){
		final List<CodecInterface<?>> codecs = new ArrayList<>(derivedClasses.size());
		for(final Class<?> type : derivedClasses){
			//for each extracted class, try to create an instance
			final CodecInterface<?> codec = (CodecInterface<?>)ReflectionHelper.getCreator(type)
				.get();
			if(codec != null)
				//if the codec was created successfully instanced, add it to the list of codecs...
				codecs.add(codec);
			else
				//... otherwise warn
				eventListener.cannotCreateCodec(type.getSimpleName());
		}
		return codecs;
	}

	/**
	 * Loads all the given codecs that extends {@link CodecInterface}.
	 * <p>NOTE: If the loader previously contains a codec for a given key, the old codec is replaced by the new one.</p>
	 *
	 * @param codecs	The list of codecs to be loaded.
	 */
	void addCodecs(final CodecInterface<?>... codecs){
		Objects.requireNonNull(codecs, "Codecs cannot be null");

		eventListener.loadingCodec(codecs);

		addCodecsInner(codecs);

		eventListener.loadedCodecs(codecs.length);
	}

	private void addCodecsInner(@SuppressWarnings("BoundedWildcard") final List<CodecInterface<?>> codecs){
		//load each codec into the available codec list
		for(int i = 0; i < codecs.size(); i ++)
			if(codecs.get(i) != null)
				addCodecInner(codecs.get(i));
	}

	private void addCodecsInner(final CodecInterface<?>... codecs){
		//load each codec into the available codec list
		for(int i = 0; i < codecs.length; i ++)
			if(codecs[i] != null)
				addCodecInner(codecs[i]);
	}

	private void addCodecInner(final CodecInterface<?> codec){
		final Class<?> codecType = ReflectionHelper.resolveGenericTypes(codec.getClass(), CodecInterface.class).get(0);
		codecs.put(codecType, codec);
	}

	boolean hasCodec(final Class<?> type){
		return codecs.containsKey(type);
	}

	CodecInterface<?> getCodec(final Class<?> type){
		return codecs.get(type);
	}

}