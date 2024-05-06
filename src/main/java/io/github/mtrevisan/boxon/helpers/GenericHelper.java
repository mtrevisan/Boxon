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

	private static final ParameterizedTypeAncestorHandler PARAMETERIZED_TYPE_ANCESTOR_HANDLER = new ParameterizedTypeAncestorHandler();
	private static final ClassAncestorHandler CLASS_ANCESTOR_HANDLER = new ClassAncestorHandler();

	private static final Type[] EMPTY_TYPE_ARRAY = new Type[0];


	private GenericHelper(){}


	/**
	 * Resolves the actual generic type arguments for a base class, as viewed from a subclass or implementation.
	 *
	 * @param offspring	The class or interface subclassing or extending the base type.
	 * @param base	The base class.
	 * @param argumentsType	The actual type arguments passed to the offspring class.
	 * 	If no arguments are given, then the type parameters of the offspring will be used.
	 * @param <T>	The base type.
	 * @return	The actual generic type arguments, must match the type parameters of the offspring class.
	 * 	If omitted, the type parameters will be used instead.
	 *
	 * @see <a href="https://stackoverflow.com/questions/17297308/how-do-i-resolve-the-actual-type-for-a-generic-return-type-using-reflection">How do I resolve the actual type for a generic return type using reflection?</a>
	 */
	public static <T> List<Type> resolveGenericTypes(final Class<? extends T> offspring, final Class<T> base, final Type... argumentsType){
		//initialize list to store resolved types
		final List<Type> types = new ArrayList<>(0);

		final Queue<Class<?>> classStack = new ArrayDeque<>(1);
		final Queue<Type[]> argumentsStack = new ArrayDeque<>(1);
		classStack.add(offspring);
		argumentsStack.add(argumentsType);
		while(!classStack.isEmpty()){
			final Class<?> currentOffspring = classStack.poll();
			final Type[] currentArgumentsTypes = argumentsStack.poll();

			//find direct ancestors (superclass and interfaces)
			final List<Type> ancestors = extractAncestors(currentOffspring);

			//map type parameters into the actual types
			final TypeVariable<? extends Class<?>>[] typeParameters = currentOffspring.getTypeParameters();
			final Map<String, Type> typeVariables = mapParameterTypes(typeParameters);

			//process ancestors
			processAncestors(ancestors, typeVariables, base, classStack, argumentsStack);

			//if there are no resolved types and offspring is equal to base (or the last class before `Object` if `base` is `Object`),
			//process the base
			if((base != Object.class? currentOffspring: classStack.peek()) == base){
				processBase(currentOffspring, currentArgumentsTypes, types);

				//stop the search once reached the `base` class
				break;
			}
		}

		return types;
	}

	private static Map<String, Type> mapParameterTypes(final Type[] actualTypes){
		final int length = actualTypes.length;
		final Map<String, Type> typeVariables = new HashMap<>(length);
		for(int i = 0; i < length; i ++){
			final Type actualType = actualTypes[i];
			final String key = ((TypeVariable<?>)actualType).getName();
			typeVariables.put(key, actualType);
		}
		return typeVariables;
	}

	private static List<Type> extractAncestors(final Class<?> offspring){
		final Type[] genericInterfaces = offspring.getGenericInterfaces();
		final List<Type> ancestors = new ArrayList<>(genericInterfaces.length + 1);
		for(int i = 0, length = genericInterfaces.length; i < length; i ++)
			ancestors.add(genericInterfaces[i]);
		final Type genericSuperclass = offspring.getGenericSuperclass();
		if(genericSuperclass != null)
			ancestors.add(genericSuperclass);
		return ancestors;
	}

	private static void processAncestors(final List<Type> ancestors, final Map<String, Type> typeVariables, final Class<?> base,
			final Collection<Class<?>> classStack, final Collection<Type[]> argumentsStack){
		for(int i = 0, length = ancestors.size(); i < length; i ++){
			final Type ancestorType = ancestors.get(i);

			processAncestor(ancestorType, typeVariables, base, classStack, argumentsStack);
		}
	}

	private static void processAncestor(final Type ancestorType, final Map<String, Type> typeVariables, final Class<?> base,
			final Collection<Class<?>> classStack, final Collection<Type[]> argumentsStack){
		final AncestorHandler handler = selectAncestorHandler(ancestorType);
		final Type type = handler.getRawType(ancestorType);
		if(type instanceof final Class<?> rawType && base.isAssignableFrom(rawType)){
			final Type[] resolvedTypes = handler.getResolvedTypes(ancestorType, typeVariables);

			classStack.add(rawType);
			argumentsStack.add(resolvedTypes);
		}
	}

	private static AncestorHandler selectAncestorHandler(final Type ancestorType){
		return (ancestorType instanceof ParameterizedType
			? PARAMETERIZED_TYPE_ANCESTOR_HANDLER
			: CLASS_ANCESTOR_HANDLER
		);
	}

	private interface AncestorHandler{
		Type getRawType(Type ancestorType);

		Type[] getResolvedTypes(Type parameterizedType, Map<String, Type> typeVariables);
	}

	//ancestor is parameterized: process only if the raw type matches the base class
	private static class ParameterizedTypeAncestorHandler implements AncestorHandler{
		@Override
		public final Type getRawType(final Type ancestorType){
			return ((ParameterizedType)ancestorType).getRawType();
		}

		@Override
		public final Type[] getResolvedTypes(final Type parameterizedType, final Map<String, Type> typeVariables){
			final Type[] actualTypeArguments = ((ParameterizedType)parameterizedType).getActualTypeArguments();
			final int length = actualTypeArguments.length;
			final Type[] resolvedTypes = new Type[length];
			//loop through all type arguments and replace type variables with the actually known types
			for(int i = 0; i < length; i ++)
				resolvedTypes[i] = resolveArgumentType(typeVariables, actualTypeArguments[i]);
			return resolvedTypes;
		}

		private static Type resolveArgumentType(final Map<String, Type> typeVariables, final Type actualTypeArgument){
			final String key = (actualTypeArgument instanceof final TypeVariable<?> v
				? v.getName()
				: null
			);
			return typeVariables.getOrDefault(key, actualTypeArgument);
		}
	}

	//ancestor is non-parameterized: process only if it matches the base class
	private static class ClassAncestorHandler implements AncestorHandler{
		@Override
		public final Type getRawType(final Type ancestorType){
			return ancestorType;
		}

		@Override
		public final Type[] getResolvedTypes(final Type parameterizedType, final Map<String, Type> typeVariables){
			return EMPTY_TYPE_ARRAY;
		}
	}

	private static void processBase(final Type currentOffspring, final Type[] actualArgs, final Collection<Type> types){
		//there is a result if the base class is reached
		for(int i = 0, length = actualArgs.length; i < length; i ++)
			types.add(actualArgs[i]);

		if(types.isEmpty())
			types.add(currentOffspring);
	}


	public static Class<?> addArrayToType(final Class<?> cls, final int arraysCount){
		final int[] dimensions = new int[arraysCount];
		Arrays.fill(dimensions, 1);
		return Array.newInstance(cls, dimensions)
			.getClass();
	}

}
