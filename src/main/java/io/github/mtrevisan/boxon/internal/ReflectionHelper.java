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
package io.github.mtrevisan.boxon.internal;

import org.springframework.objenesis.instantiator.ObjectInstantiator;
import org.springframework.objenesis.instantiator.android.Android10Instantiator;
import org.springframework.objenesis.instantiator.android.Android17Instantiator;
import org.springframework.objenesis.instantiator.android.Android18Instantiator;
import org.springframework.objenesis.instantiator.basic.AccessibleInstantiator;
import org.springframework.objenesis.instantiator.basic.ObjectInputStreamInstantiator;
import org.springframework.objenesis.instantiator.gcj.GCJInstantiator;
import org.springframework.objenesis.instantiator.perc.PercInstantiator;
import org.springframework.objenesis.instantiator.sun.SunReflectionFactoryInstantiator;
import org.springframework.objenesis.instantiator.sun.UnsafeFactoryInstantiator;
import org.springframework.objenesis.strategy.PlatformDescription;
import org.springframework.util.StringUtils;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;


/**
 * @see <a href="https://bill.burkecentral.com/2008/01/14/scanning-java-annotations-at-runtime/">Scanning Java Annotations at Runtime</a>
 */
public final class ReflectionHelper{

	private static final ClassLoader CLASS_LOADER = ReflectionHelper.class.getClassLoader();
	private static final String ARRAY_VARIABLE = "[]";

	/**
	 * Primitive type name to class map.
	 */
	private static final Map<String, Class<?>> PRIMITIVE_NAME_TO_TYPE = new HashMap<>(8);
	static{
		PRIMITIVE_NAME_TO_TYPE.put("boolean", Boolean.TYPE);
		PRIMITIVE_NAME_TO_TYPE.put("byte", Byte.TYPE);
		PRIMITIVE_NAME_TO_TYPE.put("char", Character.TYPE);
		PRIMITIVE_NAME_TO_TYPE.put("short", Short.TYPE);
		PRIMITIVE_NAME_TO_TYPE.put("int", Integer.TYPE);
		PRIMITIVE_NAME_TO_TYPE.put("long", Long.TYPE);
		PRIMITIVE_NAME_TO_TYPE.put("float", Float.TYPE);
		PRIMITIVE_NAME_TO_TYPE.put("double", Double.TYPE);
	}


	private static final Function<Class<?>, Supplier<?>> CREATORS = Memoizer.memoize(ReflectionHelper::getCreatorInner);


	private ReflectionHelper(){}


	public static Class<?>[] extractCallerClasses(){
		final StackWalker walker = StackWalker.getInstance();
		final List<String> classNames = walker.walk(frames -> frames.skip(1)
			.limit(2)
			.map(StackWalker.StackFrame::getClassName)
			.collect(Collectors.toList()));

		final Class<?>[] classes = new Class[classNames.size()];
		try{
			for(int i = 0; i < classNames.size(); i ++)
				classes[i] = Class.forName(classNames.get(i));
		}
		catch(final ClassNotFoundException ignored){}
		return classes;
	}

	/**
	 * Scans all classes accessible from the context class loader which belong to the given package.
	 *
	 * @param type	Whether a class or an interface (for example).
	 * @param basePackageClasses	A list of classes that resides in a base package(s).
	 * @return	The classes.
	 */
	public static Collection<Class<?>> extractClasses(final Class<?> type, final Class<?>... basePackageClasses){
		final Collection<Class<?>> classes = new HashSet<>(0);

		final ReflectiveClassLoader reflectiveClassLoader = ReflectiveClassLoader.createFrom(basePackageClasses);
		reflectiveClassLoader.scan(type);
		final Collection<Class<?>> modules = ReflectiveClassLoader.getImplementationsOf(type);
		@SuppressWarnings("unchecked")
		final Collection<Class<?>> singletons = ReflectiveClassLoader.getTypesAnnotatedWith((Class<? extends Annotation>)type);
		classes.addAll(modules);
		classes.addAll(singletons);
		return classes;
	}


	/**
	 * Resolves the actual generic type arguments for a base class, as viewed from a subclass or implementation.
	 *
	 * @param <T>	The base type.
	 * @param offspring	The class or interface subclassing or extending the base type.
	 * @param base	The base class.
	 * @param actualArgs	The actual type arguments passed to the offspring class.
	 * 	If no arguments are given, then the type parameters of the offspring will be used.
	 * @return	The actual generic type arguments, must match the type parameters of the offspring class.
	 * 	If omitted, the type parameters will be used instead.
	 *
	 * @see <a href="https://stackoverflow.com/questions/17297308/how-do-i-resolve-the-actual-type-for-a-generic-return-type-using-reflection">How do I resolve the actual type for a generic return type using reflection?</a>
	 */
	public static <T> List<Class<?>> resolveGenericTypes(final Class<? extends T> offspring, final Class<T> base, Type... actualArgs){
		//if actual types are omitted, the type parameters will be used instead
		if(actualArgs.length == 0)
			actualArgs = offspring.getTypeParameters();

		//map type parameters into the actual types
		final Map<String, Type> typeVariables = mapParameterTypes(offspring, actualArgs);

		//find direct ancestors (superclass and interfaces)
		final Queue<Type> ancestorsQueue = extractAncestors(offspring);

		//iterate over ancestors
		final List<Class<?>> types = new ArrayList<>(0);
		while(!ancestorsQueue.isEmpty()){
			final Type ancestorType = ancestorsQueue.poll();
			if(ParameterizedType.class.isInstance(ancestorType))
				//ancestor is parameterized: process only if the raw type matches the base class
				types.addAll(manageParameterizedAncestor((ParameterizedType)ancestorType, base, typeVariables));
			else if(Class.class.isInstance(ancestorType) && base.isAssignableFrom((Class<?>)ancestorType))
				//ancestor is non-parameterized: process only if it matches the base class
				ancestorsQueue.add(ancestorType);
		}

		if(types.isEmpty() && offspring.equals(base))
			//there is a result if the base class is reached
			for(int i = 0; i < actualArgs.length; i ++){
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
		if(Class.class.isInstance(rawType) && base.isAssignableFrom((Class<?>)rawType)){
			final List<Class<?>> resolvedTypes = populateResolvedTypes(ancestorType, typeVariables);

			final List<Class<?>> result = resolveGenericTypes((Class<? extends T>)rawType, base, resolvedTypes.toArray(Class[]::new));
			types.addAll(result);
		}
		return types;
	}

	private static List<Class<?>> populateResolvedTypes(final ParameterizedType ancestorType, final Map<String, Type> typeVariables){
		//loop through all type arguments and replace type variables with the actually known types
		final List<Class<?>> resolvedTypes = new ArrayList<>(0);
		final Type[] types = ancestorType.getActualTypeArguments();
		for(int i = 0; i < types.length; i ++){
			final String typeName = resolveArgumentType(typeVariables, types[i]).getTypeName();
			final Class<?> cls = toClass(typeName);
			if(cls != null)
				resolvedTypes.add(cls);
		}
		return resolvedTypes;
	}

	private static <T> Map<String, Type> mapParameterTypes(final Class<? extends T> offspring, final Type[] actualArgs){
		final Map<String, Type> typeVariables = new HashMap<>(actualArgs.length);
		for(int i = 0; i < actualArgs.length; i ++)
			typeVariables.put(offspring.getTypeParameters()[i].getName(), actualArgs[i]);
		return typeVariables;
	}

	private static <T> Queue<Type> extractAncestors(final Class<? extends T> offspring){
		final Type[] genericInterfaces = offspring.getGenericInterfaces();
		final Queue<Type> ancestorsQueue = new ArrayDeque<>(genericInterfaces.length + 1);
		ancestorsQueue.addAll(Arrays.asList(genericInterfaces));
		if(offspring.getGenericSuperclass() != null)
			ancestorsQueue.add(offspring.getGenericSuperclass());
		return ancestorsQueue;
	}

	private static Type resolveArgumentType(final Map<String, Type> typeVariables, final Type actualTypeArgument){
		return (TypeVariable.class.isInstance(actualTypeArgument)
			? typeVariables.getOrDefault(((TypeVariable<?>)actualTypeArgument).getName(), actualTypeArgument)
			: actualTypeArgument);
	}

	/**
	 * Convert a given String into the appropriate Class.
	 *
	 * @param name Name of class.
	 * @return The class for the given name, {@code null} if some error happens.
	 */
	private static Class<?> toClass(final String name){
		final int arraysCount = StringUtils.countOccurrencesOf(name, ARRAY_VARIABLE);
		final String baseName = name.substring(0, name.length() - arraysCount * ARRAY_VARIABLE.length());

		//check for a primitive type
		Class<?> cls = PRIMITIVE_NAME_TO_TYPE.get(baseName);

		if(cls == null){
			//not a primitive, try to load it through the ClassLoader
			try{
				cls = CLASS_LOADER.loadClass(baseName);
			}
			catch(final ClassNotFoundException ignored){}
		}

		//if we have an array get the array class
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


	@SuppressWarnings("unchecked")
	public static <T> T getFieldValue(final Field field, final Object obj){
		try{
			return (T)field.get(obj);
		}
		catch(final IllegalAccessException e){
			//should never happen
			throw new IllegalArgumentException(e);
		}
	}

	public static void setFieldValue(final Field field, final Object obj, final Object value){
		try{
			field.set(obj, value);
		}
		catch(final IllegalArgumentException | IllegalAccessException e){
			throw new IllegalArgumentException("Can not set " + field.getType().getSimpleName() + " field to "
				+ value.getClass().getSimpleName(), e);
		}
	}

	public static <T> void setFieldValue(final Object obj, final Class<T> fieldType, final T value){
		try{
			final List<Field> fields = getAccessibleFields(obj.getClass(), fieldType);
			for(int i = 0; i < fields.size(); i ++)
				fields.get(i).set(obj, value);
		}
		catch(final IllegalArgumentException | IllegalAccessException ignored){}
	}

	public static <T> void setStaticFieldValue(final Class<?> cl, final Class<T> fieldType, final T value){
		try{
			final List<Field> fields = getAccessibleFields(cl, fieldType);
			for(int i = 0; i < fields.size(); i ++)
				fields.get(i).set(null, value);
		}
		catch(final IllegalArgumentException | IllegalAccessException ignored){}
	}

	/**
	 * Retrieve all declared fields in the current class AND in the parent classes.
	 *
	 * @param cls	The class from which to extract the declared fields.
	 * @return	An array of all the fields of the given class.
	 */
	public static List<Field> getAccessibleFields(final Class<?> cls){
		return getAccessibleFields(cls, null);
	}

	/**
	 * Retrieve all declared fields in the current class AND in the parent classes.
	 *
	 * @param cls	The class from which to extract the declared fields.
	 * @param fieldType	The class for which to extract all the fields.
	 * @return	An array of all the fields of the given class.
	 */
	private static List<Field> getAccessibleFields(Class<?> cls, final Class<?> fieldType){
		final List<Field> fields = new ArrayList<>(0);

		//recurse classes:
		final ArrayList<Field> childFields = new ArrayList<>(0);
		while(cls != null && cls != Object.class){
			final Field[] rawSubfields = cls.getDeclaredFields();
			extractChildFields(childFields, rawSubfields, fieldType);
			//place parent's fields before all the child's fields
			fields.addAll(0, childFields);

			//go up to parent class
			cls = cls.getSuperclass();
		}

		makeFieldsAccessible(fields);

		return fields;
	}

	private static void extractChildFields(final ArrayList<Field> childFields, final Field[] rawSubfields, final Class<?> fieldType){
		childFields.clear();
		childFields.ensureCapacity(rawSubfields.length);
		//apply filter on field type if needed
		for(int i = 0; i < rawSubfields.length; i ++)
			if(fieldType == null || rawSubfields[i].getType() == fieldType)
				childFields.add(rawSubfields[i]);
	}

	private static void makeFieldsAccessible(final List<Field> fields){
		for(int i = 0; i < fields.size(); i ++)
			fields.get(i).setAccessible(true);
	}


	@SuppressWarnings("unchecked")
	public static <T> T invokeMethod(final Object obj, final Method method, final T defaultValue){
		T result = defaultValue;
		try{
			result = (method != null? (T)method.invoke(obj): defaultValue);
		}
		catch(final IllegalAccessException | InvocationTargetException ignored){}
		return result;
	}

	public static Method getAccessibleMethod(Class<?> cls, final String methodName, final Class<?> returnType,
			final Class<?>... parameterTypes){
		Method method = null;
		while(cls != null && cls != Object.class){
			try{
				method = cls.getDeclaredMethod(methodName, parameterTypes);
				if(returnType == null || method.getReturnType() == returnType){
					method.setAccessible(true);
					break;
				}
			}
			catch(final NoSuchMethodException ignored){}

			//go up to parent class
			cls = cls.getSuperclass();
		}
		return method;
	}


	@SuppressWarnings("unchecked")
	public static <T> Supplier<T> getCreator(final Class<T> type){
		return (Supplier<T>)CREATORS.apply(type);
	}

	private static <T> Supplier<T> getCreatorInner(final Class<T> type){
		Supplier<T> creator;
		try{
			final Constructor<T> constructor = type.getDeclaredConstructor();
			constructor.setAccessible(true);
			//try creating an instance
			constructor.newInstance();

			creator = createSupplierIgnoreExceptions(constructor);
		}
		catch(final Exception ignored){
			creator = instantiatorOf(type)::newInstance;
		}
		return creator;
	}

	private static <T> Supplier<T> createSupplierIgnoreExceptions(final Constructor<? extends T> constructor){
		return () -> {
			try{
				return constructor.newInstance();
			}
			catch(final Exception ignored){
				//should never happen
				return null;
			}
		};
	}

	/**
	 * Return an {@link ObjectInstantiator} allowing to create instance without any constructor being
	 * called.
	 *
	 * @see <a href="https://github.com/easymock/objenesis">objenesis</a>
	 *
	 * @param type Class to instantiate.
	 * @return The ObjectInstantiator for the class.
	 */
	private static <T> ObjectInstantiator<T> instantiatorOf(final Class<T> type){
		final ObjectInstantiator<T> instantiator;
		if(PlatformDescription.isThisJVM(PlatformDescription.HOTSPOT) || PlatformDescription.isThisJVM(PlatformDescription.OPENJDK))
			instantiator = instantiatorForOpenJDK(type);
		else if(PlatformDescription.isThisJVM(PlatformDescription.DALVIK))
			instantiator = instantiatorForDalvik(type);
		else if(PlatformDescription.isThisJVM(PlatformDescription.GNU))
			instantiator = new GCJInstantiator<>(type);
		else if(PlatformDescription.isThisJVM(PlatformDescription.PERC))
			instantiator = new PercInstantiator<>(type);
		else
			//fallback instantiator, should work with most modern JVM
			instantiator = new UnsafeFactoryInstantiator<>(type);

		return instantiator;
	}

	private static <T> ObjectInstantiator<T> instantiatorForOpenJDK(final Class<T> type){
		final ObjectInstantiator<T> instantiator;
		//Java 7 GAE was under a security manager, so we use a degraded system
		if(PlatformDescription.isGoogleAppEngine() && "1.7".equals(PlatformDescription.SPECIFICATION_VERSION))
			instantiator = (Serializable.class.isAssignableFrom(type)
				? new ObjectInputStreamInstantiator<>(type)
				: new AccessibleInstantiator<>(type));
		else
			//the UnsafeFactoryInstantiator would also work, but according to benchmarks, it is 2.5 times slower
			instantiator = new SunReflectionFactoryInstantiator<>(type);
		return instantiator;
	}

	private static <T> ObjectInstantiator<T> instantiatorForDalvik(final Class<T> type){
		final ObjectInstantiator<T> instantiator;
		if(PlatformDescription.isAndroidOpenJDK())
			//starting at Android N which is based on OpenJDK
			instantiator = new UnsafeFactoryInstantiator<>(type);
		else if(PlatformDescription.ANDROID_VERSION <= 10)
			//Android 2.3 Gingerbread and lower
			instantiator = new Android10Instantiator<>(type);
		else if(PlatformDescription.ANDROID_VERSION <= 17)
			//Android 3.0 Honeycomb to 4.2 Jelly Bean
			instantiator = new Android17Instantiator<>(type);
		else
			//Android 4.3 until Android N
			instantiator = new Android18Instantiator<>(type);
		return instantiator;
	}

}
