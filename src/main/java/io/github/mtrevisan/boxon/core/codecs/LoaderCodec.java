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
package io.github.mtrevisan.boxon.core.codecs;

import io.github.mtrevisan.boxon.helpers.ConstructorHelper;
import io.github.mtrevisan.boxon.helpers.GenericHelper;
import io.github.mtrevisan.boxon.helpers.JavaHelper;
import io.github.mtrevisan.boxon.helpers.ReflectionHelper;
import io.github.mtrevisan.boxon.helpers.ReflectiveClassLoader;
import io.github.mtrevisan.boxon.io.CodecInterface;
import io.github.mtrevisan.boxon.logs.EventListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;


/**
 * Loader for the codecs.
 */
public final class LoaderCodec implements LoaderCodecInterface{

	private final Map<Class<?>, CodecInterface<?>> codecs = new ConcurrentHashMap<>(0);

	private EventListener eventListener;


	/**
	 * Create a codec loader.
	 *
	 * @return	A codec loader.
	 */
	public static LoaderCodec create(){
		return new LoaderCodec();
	}


	private LoaderCodec(){
		eventListener = EventListener.getNoOpInstance();
	}


	/**
	 * Assign an event listener.
	 *
	 * @param eventListener	The event listener.
	 * @return	The current instance.
	 */
	public LoaderCodec withEventListener(final EventListener eventListener){
		this.eventListener = JavaHelper.nonNullOrDefault(eventListener, EventListener.getNoOpInstance());

		return this;
	}

	/**
	 * Loads all the codecs that extends {@link CodecInterface}.
	 * <p>This method SHOULD BE called from a method inside a class that lies on a parent of all the codecs.</p>
	 */
	public void loadDefaultCodecs(){
		loadCodecsFrom(ReflectiveClassLoader.extractCallerClasses());
	}

	/**
	 * Loads all the codecs that extends {@link CodecInterface}.
	 *
	 * @param basePackageClasses	Classes to be used ase starting point from which to load codecs.
	 */
	public void loadCodecsFrom(final Class<?>... basePackageClasses){
		eventListener.loadingCodecsFrom(basePackageClasses);

		final ReflectiveClassLoader reflectiveClassLoader = ReflectiveClassLoader.createFrom(basePackageClasses);
		/** extract all classes that implements {@link CodecInterface}. */
		final List<Class<?>> derivedClasses = reflectiveClassLoader.extractClassesImplementing(CodecInterface.class);
		final List<CodecInterface<?>> codecs = extractCodecs(derivedClasses);
		addCodecsInner(codecs);

		eventListener.loadedCodecs(codecs.size());
	}

	private List<CodecInterface<?>> extractCodecs(final List<Class<?>> derivedClasses){
		final int length = derivedClasses.size();
		final List<CodecInterface<?>> codecs = new ArrayList<>(length);
		for(int i = 0; i < length; i ++){
			final Class<?> type = derivedClasses.get(i);

			//for each extracted class, try to create an instance
			final CodecInterface<?> codec = (CodecInterface<?>)ConstructorHelper.getEmptyCreator(type)
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
	 * Loads the given codec that extends {@link CodecInterface}.
	 * <p>NOTE: If the loader previously contains a codec for a given key, the old codec is replaced by the new one.</p>
	 *
	 * @param codec	The codec to be loaded.
	 */
	public void addCodec(final CodecInterface<?> codec){
		Objects.requireNonNull(codec, "Codec cannot be null");

		eventListener.loadingCodec(codec.getClass());

		addCodecInner(codec);

		eventListener.loadedCodecs(1);
	}

	/**
	 * Loads all the given codecs that extends {@link CodecInterface}.
	 * <p>NOTE: If the loader previously contains a codec for a given key, the old codec is replaced by the new one.</p>
	 *
	 * @param codecs	The list of codecs to be loaded.
	 */
	public void addCodecs(final CodecInterface<?>... codecs){
		Objects.requireNonNull(codecs, "Codecs cannot be null");

		final int length = codecs.length;
		final Class<?>[] codecClasses = new Class<?>[length];
		for(int i = 0; i < length; i ++)
			codecClasses[i] = codecs[i].getClass();
		eventListener.loadingCodec(codecClasses);

		addCodecsInner(codecs);

		eventListener.loadedCodecs(length);
	}

	private void addCodecsInner(@SuppressWarnings("BoundedWildcard") final List<CodecInterface<?>> codecs){
		//load each codec into the available codec list
		for(int i = 0, length = codecs.size(); i < length; i ++){
			final CodecInterface<?> codec = codecs.get(i);

			if(codec != null)
				addCodecInner(codec);
		}
	}

	private void addCodecsInner(final CodecInterface<?>... codecs){
		//load each codec into the available codec list
		for(int i = 0, length = codecs.length; i < length; i ++){
			final CodecInterface<?> codec = codecs[i];

			if(codec != null)
				addCodecInner(codec);
		}
	}

	private void addCodecInner(final CodecInterface<?> codec){
		final Class<?> codecType = GenericHelper.resolveGenericTypes(codec.getClass(), CodecInterface.class)
			.getFirst();
		codecs.put(codecType, codec);
	}

	/**
	 * Inject the give object in all the codecs.
	 *
	 * @param type	The class type of the object to be injected.
	 * @param object	The object to be injected.
	 * @param <T>	The class type of the object.
	 */
	//FIXME is injection an ugliness?
	public <T> void injectFieldInCodecs(final Class<T> type, final T object){
		for(final CodecInterface<?> codec : codecs.values())
			ReflectionHelper.injectValue(codec, type, object);
	}

	@Override
	public boolean hasCodec(final Class<?> type){
		return codecs.containsKey(type);
	}

	@Override
	public CodecInterface<?> getCodec(final Class<?> type){
		return codecs.get(type);
	}

}
