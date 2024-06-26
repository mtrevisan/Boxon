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

import io.github.mtrevisan.boxon.annotations.bindings.BindBitSet;
import io.github.mtrevisan.boxon.annotations.bindings.BindInteger;
import io.github.mtrevisan.boxon.annotations.bindings.BindString;
import io.github.mtrevisan.boxon.annotations.bindings.BindStringTerminated;
import io.github.mtrevisan.boxon.core.helpers.ConstructorHelper;
import io.github.mtrevisan.boxon.core.helpers.FieldAccessor;
import io.github.mtrevisan.boxon.exceptions.CodecException;
import io.github.mtrevisan.boxon.helpers.ReflectiveClassLoader;
import io.github.mtrevisan.boxon.io.AnnotationValidator;
import io.github.mtrevisan.boxon.io.Codec;
import io.github.mtrevisan.boxon.logs.EventListener;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;


/**
 * Loader for the codecs.
 */
public final class LoaderCodec{

	private static final Set<Type> DEFAULT_BIND_TYPES = Set.of(
		BindBitSet.class,
		BindInteger.class,
		BindString.class,
		BindStringTerminated.class);


	private final Map<Type, Codec> codecs = new ConcurrentHashMap<>(0);
	private final Map<Type, AnnotationValidator> customCodecValidators = new ConcurrentHashMap<>(0);

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
		withEventListener(null);
	}


	/**
	 * Assign an event listener.
	 *
	 * @param eventListener	The event listener.
	 * @return	The current instance.
	 */
	public LoaderCodec withEventListener(final EventListener eventListener){
		this.eventListener = (eventListener != null? eventListener: EventListener.getNoOpInstance());

		return this;
	}

	/**
	 * Loads all the codecs.
	 */
	public void loadDefaultCodecs(){
		try{
			loadCodecsFrom(CodecDefault.class);
		}
		catch(final CodecException ignored){}
	}

	/**
	 * Loads all the codecs that extends {@link Codec}.
	 *
	 * @param basePackageClasses	Classes to be used ase starting point from which to load codecs.
	 * @throws CodecException	If a codec was already loaded.
	 */
	public void loadCodecsFrom(final Class<?>... basePackageClasses) throws CodecException{
		eventListener.loadingCodecsFrom(basePackageClasses);

		final ReflectiveClassLoader reflectiveClassLoader = ReflectiveClassLoader.createFrom(basePackageClasses);
		/** extract all classes that implements {@link Codec}. */
		final List<Class<?>> derivedClasses = reflectiveClassLoader.extractClassesImplementing(Codec.class);
		final List<Codec> codecs = extractCodecs(derivedClasses);
		addCodecsInner(codecs);

		eventListener.loadedCodecs(codecs.size());
	}

	private List<Codec> extractCodecs(final List<Class<?>> derivedClasses){
		final int length = derivedClasses.size();
		final List<Codec> codecs = new ArrayList<>(length);
		for(int i = 0; i < length; i ++){
			final Class<?> type = derivedClasses.get(i);

			//for each extracted class, try to create an instance
			final Codec codec = (Codec)ConstructorHelper.getEmptyCreator(type)
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
	 * Loads the given codec.
	 * <p>NOTE: If the loader previously contains a codec for a given key, the old codec is replaced by the new one.</p>
	 *
	 * @param codec	The codec to be loaded.
	 * @throws CodecException	If the codec was already loaded.
	 */
	public void addCodec(final Codec codec) throws CodecException{
		Objects.requireNonNull(codec, "Codec cannot be null");

		eventListener.loadingCodec(codec.getClass());

		addCodecInner(codec, null);

		eventListener.loadedCodecs(1);
	}

	/**
	 * Loads the given codec.
	 * <p>NOTE: If the loader previously contains a codec for a given key, the old codec is replaced by the new one.</p>
	 *
	 * @param codec	The codec to be loaded.
	 * @param validator	The codec validator.
	 * @throws CodecException	If the codec was already loaded.
	 */
	public void addCodec(final Codec codec, final AnnotationValidator validator) throws CodecException{
		Objects.requireNonNull(codec, "Codec cannot be null");

		eventListener.loadingCodec(codec.getClass(), validator);

		addCodecInner(codec, validator);

		eventListener.loadedCodecs(1);
	}

	/**
	 * Loads all the given codecs.
	 * <p>NOTE: If the loader previously contains a codec for a given key, the old codec is replaced by the new one.</p>
	 *
	 * @param codecs	The list of codecs to be loaded.
	 * @throws CodecException	If a codec was already loaded.
	 */
	public void addCodecs(final Codec... codecs) throws CodecException{
		Objects.requireNonNull(codecs, "Codecs cannot be null");

		final int length = codecs.length;
		final Class<?>[] codecClasses = new Class<?>[length];
		for(int i = 0; i < length; i ++)
			codecClasses[i] = codecs[i].getClass();
		eventListener.loadingCodec(codecClasses);

		addCodecsInner(codecs);

		eventListener.loadedCodecs(length);
	}

	private void addCodecsInner(final List<Codec> codecs) throws CodecException{
		//load each codec into the available codec list
		for(int i = 0, length = codecs.size(); i < length; i ++){
			final Codec codec = codecs.get(i);

			if(codec != null)
				addCodecInner(codec, null);
		}
	}

	private void addCodecsInner(final Codec... codecs) throws CodecException{
		//load each codec into the available codec list
		for(int i = 0, length = codecs.length; i < length; i ++){
			final Codec codec = codecs[i];

			if(codec != null)
				addCodecInner(codec, null);
		}
	}

	private void addCodecInner(final Codec codec, final AnnotationValidator validator) throws CodecException{
		final Class<?> codecType = codec.annotationType();
		if(codecs.containsKey(codecType))
			throw CodecException.create("Codec with type {} already added", codecType);

		codecs.put(codecType, codec);
		if(validator != null)
			customCodecValidators.put(codecType, validator);
	}

	/**
	 * Inject the give object in all the codecs.
	 *
	 * @param dependencies	The object to be injected.
	 */
	//FIXME is injection an ugliness?
	public void injectDependenciesIntoCodecs(final Object... dependencies){
		for(final Codec codec : codecs.values())
			injectDependenciesIntoCodec(codec, dependencies);
	}

	/**
	 * Inject the give object in the given codec.
	 *
	 * @param codec	The codec to be injected into.
	 * @param dependencies	The object(s) to be injected.
	 */
	public static void injectDependenciesIntoCodec(final Codec codec, final Object... dependencies){
		FieldAccessor.injectValues(codec, dependencies);
	}

	/**
	 * Whether there is a codec for the given class type.
	 *
	 * @param type	The class type.
	 * @return	Whether there is a codec for the given class type.
	 */
	public boolean hasCodec(final Type type){
		return codecs.containsKey(isDefaultBind(type)? CodecDefault.DefaultCodecIdentifier.class: type);
	}

	/**
	 * Get the codec for the given class type.
	 *
	 * @param type	The class type.
	 * @return	The codec for the given class type.
	 */
	public Codec getCodec(final Type type){
		return codecs.get(isDefaultBind(type)? CodecDefault.DefaultCodecIdentifier.class: type);
	}

	/**
	 * Get the codec validator for the given class type.
	 *
	 * @param type	The class type.
	 * @return	The codec validator for the given class type.
	 */
	public AnnotationValidator getCustomCodecValidator(final Type type){
		return customCodecValidators.get(isDefaultBind(type)? CodecDefault.DefaultCodecIdentifier.class: type);
	}

	private static boolean isDefaultBind(final Type type){
		return DEFAULT_BIND_TYPES.contains(type);
	}

}
