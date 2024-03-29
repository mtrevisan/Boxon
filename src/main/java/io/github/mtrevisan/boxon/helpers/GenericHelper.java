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
package io.github.mtrevisan.boxon.helpers;

import org.springframework.util.StringUtils;

import java.lang.reflect.Array;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;


/**
 * A collection of convenience methods for working with generics.
 */
public final class GenericHelper{

	private static final ClassLoader CLASS_LOADER = GenericHelper.class.getClassLoader();

	private static final String ARRAY_VARIABLE = "[]";

	/** An empty {@code Class} array. */
	private static final Class<?>[] EMPTY_CLASS_ARRAY = new Class[0];

	/**
	 * Primitive type name to class map.
	 */
	private static final Map<String, Class<?>> PRIMITIVE_NAME_TO_TYPE = Map.of(
		"boolean", Boolean.TYPE,
		"byte", Byte.TYPE,
		"char", Character.TYPE,
		"short", Short.TYPE,
		"int", Integer.TYPE,
		"long", Long.TYPE,
		"float", Float.TYPE,
		"double", Double.TYPE);


	private GenericHelper(){}


	/**
	 * Resolves the actual generic type arguments for a base class, as viewed from a subclass or implementation.
	 *
	 * @param offspring	The class or interface subclassing or extending the base type.
	 * @param base	The base class.
	 * @param actualArgs	The actual type arguments passed to the offspring class.
	 * 	If no arguments are given, then the type parameters of the offspring will be used.
	 * @param <T>	The base type.
	 * @return	The actual generic type arguments, must match the type parameters of the offspring class.
	 * 	If omitted, the type parameters will be used instead.
	 *
	 * @see <a href="https://stackoverflow.com/questions/17297308/how-do-i-resolve-the-actual-type-for-a-generic-return-type-using-reflection">How do I resolve the actual type for a generic return type using reflection?</a>
	 */
	public static <T> List<Class<?>> resolveGenericTypes(final Class<? extends T> offspring, final Class<T> base, Type... actualArgs){
		//if actual types are omitted, the type parameters will be used instead
		if(actualArgs.length == 0)
			actualArgs = offspring.getTypeParameters();

		final List<Class<?>> types = processAncestors(offspring, base, actualArgs);

		if(types.isEmpty() && offspring.equals(base))
			types.addAll(processBase(actualArgs));

		return types;
	}

	private static <T> List<Class<?>> processAncestors(final Class<? extends T> offspring, final Class<T> base, final Type[] actualArgs){
		//map type parameters into the actual types
		final Map<String, Type> typeVariables = mapParameterTypes(offspring, actualArgs);

		//find direct ancestors (superclass and interfaces)
		final Queue<Type> ancestorsQueue = extractAncestors(offspring);

		//iterate over ancestors
		final List<Class<?>> types = new ArrayList<>(0);
		while(!ancestorsQueue.isEmpty()){
			final Type ancestorType = ancestorsQueue.poll();
			if(ancestorType instanceof final ParameterizedType t)
				//ancestor is parameterized: process only if the raw type matches the base class
				types.addAll(manageParameterizedAncestor(t, base, typeVariables));
			else if(ancestorType instanceof final Class<?> c && base.isAssignableFrom(c))
				//ancestor is non-parameterized: process only if it matches the base class
				ancestorsQueue.add(ancestorType);
		}
		return types;
	}

	private static List<Class<?>> processBase(final Type[] actualArgs){
		//there is a result if the base class is reached
		final int length = actualArgs.length;
		final List<Class<?>> types = new ArrayList<>(length);
		for(int i = 0; i < length; i ++){
			final Class<?> cls = toClass(actualArgs[i].getTypeName());
			if(cls != null)
				types.add(cls);
		}
		return types;
	}

	@SuppressWarnings("unchecked")
	private static <T> List<Class<?>> manageParameterizedAncestor(final ParameterizedType ancestorType, final Class<T> base,
			final Map<String, Type> typeVariables){
		final List<Class<?>> types = new ArrayList<>(0);
		final Type rawType = ancestorType.getRawType();
		if(rawType instanceof final Class<?> c && base.isAssignableFrom(c)){
			final Class<?>[] resolvedTypes = populateResolvedTypes(ancestorType, typeVariables);
			final List<Class<?>> result = resolveGenericTypes((Class<? extends T>)rawType, base, resolvedTypes);
			types.addAll(result);
		}
		return types;
	}

	private static Class<?>[] populateResolvedTypes(final ParameterizedType ancestorType, final Map<String, Type> typeVariables){
		final Type[] types = ancestorType.getActualTypeArguments();
		final int length = types.length;
		final Collection<Class<?>> resolvedTypes = new ArrayList<>(length);
		//loop through all type arguments and replace type variables with the actually known types
		for(int i = 0; i < length; i ++){
			final String typeName = resolveArgumentType(typeVariables, types[i])
				.getTypeName();

			final Class<?> cls = toClass(typeName);
			if(cls != null)
				resolvedTypes.add(cls);
		}
		return resolvedTypes.toArray(EMPTY_CLASS_ARRAY);
	}

	private static <T> Map<String, Type> mapParameterTypes(final Class<? extends T> offspring, final Type[] actualArgs){
		final int length = actualArgs.length;
		final Map<String, Type> typeVariables = new HashMap<>(length);
		final TypeVariable<? extends Class<? extends T>>[] typeParameters = offspring.getTypeParameters();
		for(int i = 0; i < length; i ++){
			final String key = typeParameters[i].getName();

			typeVariables.put(key, actualArgs[i]);
		}
		return typeVariables;
	}

	private static <T> Queue<Type> extractAncestors(final Class<? extends T> offspring){
		final Type[] genericInterfaces = offspring.getGenericInterfaces();
		final Queue<Type> ancestorsQueue = new ArrayDeque<>(genericInterfaces.length + 1);
		ancestorsQueue.addAll(Arrays.asList(genericInterfaces));
		final Type genericSuperclass = offspring.getGenericSuperclass();
		if(genericSuperclass != null)
			ancestorsQueue.add(genericSuperclass);
		return ancestorsQueue;
	}

	private static Type resolveArgumentType(final Map<String, Type> typeVariables, final Type actualTypeArgument){
		final String key = (actualTypeArgument instanceof final TypeVariable<?> v
			? v.getName()
			: null);
		return typeVariables.getOrDefault(key, actualTypeArgument);
	}

	/**
	 * Convert a given String into the appropriate Class.
	 *
	 * @param name Name of class.
	 * @return	The class for the given name, {@code null} if some error happens.
	 */
	private static Class<?> toClass(final String name){
		final int arraysCount = StringUtils.countOccurrencesOf(name, ARRAY_VARIABLE);
		final String baseName = name.substring(0, name.length() - arraysCount * ARRAY_VARIABLE.length());

		//check for a primitive type
		Class<?> cls = PRIMITIVE_NAME_TO_TYPE.get(baseName);

		if(cls == null){
			//not a primitive, try to load it through the `ClassLoader`
			try{
				cls = CLASS_LOADER.loadClass(baseName);
			}
			catch(final ClassNotFoundException ignored){}
		}

		//if we have an array, get the array class
		if(cls != null && arraysCount > 0)
			cls = addArrayToType(cls, arraysCount);

		return cls;
	}

	private static Class<?> addArrayToType(final Class<?> cls, final int arraysCount){
		final int[] dimensions = new int[arraysCount];
		Arrays.fill(dimensions, 1);
		return Array.newInstance(cls, dimensions)
			.getClass();
	}

}
