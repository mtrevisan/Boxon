/**
 * Copyright (c) 2020 Mauro Trevisan
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;


/**
 * @see <a href="https://bill.burkecentral.com/2008/01/14/scanning-java-annotations-at-runtime/">Scanning Java Annotations at Runtime</a>
 */
public final class ReflectionHelper{

	private static final Logger LOGGER = LoggerFactory.getLogger(ReflectionHelper.class);

	private static final ClassLoader CLASS_LOADER = ReflectionHelper.class.getClassLoader();
	private static final String ARRAY_VARIABLE = "[]";

	/**
	 * Primitive type name to class map.
	 */
	private static final Map<String, Class<?>> PRIMITIVE_NAME_TO_TYPE = new HashMap<>();
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
		Class<?>[] classes = new Class[0];
		try{
			final StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
			final Class<?> callerClass1 = Class.forName(stackTrace[2].getClassName());
			final Class<?> callerClass2 = Class.forName(stackTrace[3].getClassName());
			classes = new Class[]{callerClass1, callerClass2};
		}
		catch(final ClassNotFoundException ignored){}
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
	 * @see <a href="https://stackoverflow.com/questions/17297308/how-do-i-resolve-the-actual-type-for-a-generic-return-type-using-reflection">How do I resolve the actual type for a generic return type using reflection?</>
	 */
	public static <T> Class<?>[] resolveGenericTypes(final Class<? extends T> offspring, final Class<T> base, Type... actualArgs){
		//if actual types are omitted, the type parameters will be used instead
		if(actualArgs.length == 0)
			actualArgs = offspring.getTypeParameters();

		//map type parameters into the actual types
		final Map<String, Type> typeVariables = mapParameterTypes(offspring, actualArgs);

		//find direct ancestors (superclass and interfaces)
		final Queue<Type> ancestorsQueue = extractAncestors(offspring);

		//iterate over ancestors
		@SuppressWarnings("rawtypes")
		final DynamicArray<Class> types = DynamicArray.create(Class.class);
		while(!ancestorsQueue.isEmpty()){
			final Type ancestorType = ancestorsQueue.poll();

			if(ancestorType instanceof ParameterizedType)
				//ancestor is parameterized: process only if the raw type matches the base class
				types.addAll(manageParameterizedAncestor((ParameterizedType)ancestorType, base, typeVariables));
			else if(ancestorType instanceof Class<?> && base.isAssignableFrom((Class<?>)ancestorType))
				//ancestor is non-parameterized: process only if it matches the base class
				ancestorsQueue.add(ancestorType);
		}
		if(types.isEmpty() && offspring.equals(base))
			//there is a result if the base class is reached
			for(final Type actualArg : actualArgs)
				types.addIfNotNull(toClass(actualArg.getTypeName()));
		return types.extractCopy();
	}

	@SuppressWarnings("rawtypes")
	private static <T> DynamicArray<Class> manageParameterizedAncestor(final ParameterizedType ancestorType, final Class<T> base,
			final Map<String, Type> typeVariables){
		final DynamicArray<Class> types = DynamicArray.create(Class.class);
		final Type rawType = ancestorType.getRawType();
		if(rawType instanceof Class<?> && base.isAssignableFrom((Class<?>)rawType)){
			final Type resolvedType = resolveArgumentType(ancestorType.getActualTypeArguments()[0], typeVariables);
			types.addIfNotNull(toClass(resolvedType.getTypeName()));
		}
		return types;
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

	private static Type resolveArgumentType(Type actualTypeArgument, final Map<String, Type> typeVariables){
		if(actualTypeArgument instanceof TypeVariable<?>)
			actualTypeArgument = typeVariables.getOrDefault(((TypeVariable<?>)actualTypeArgument).getName(), actualTypeArgument);
		return actualTypeArgument;
	}

	/**
	 * Convert a given String into the appropriate Class.
	 *
	 * @param name Name of class.
	 * @return The class for the given name, {@code null} if some error happens.
	 */
	private static Class<?> toClass(String name){
		final int arraysCount = StringUtils.countOccurrencesOf(name, ARRAY_VARIABLE);
		name = name.substring(0, name.length() - arraysCount * ARRAY_VARIABLE.length());

		//check for a primitive type
		Class<?> cls = PRIMITIVE_NAME_TO_TYPE.get(name);

		if(cls == null){
			//not a primitive, try to load it through the ClassLoader
			try{
				cls = CLASS_LOADER.loadClass(name);
			}
			catch(final ClassNotFoundException e){
				LOGGER.warn("Cannot convert type name to class: {}", name, e);
			}
		}

		//if we have an array get the array class
		if(cls != null && arraysCount > 0)
			cls = addArrayToType(cls, arraysCount);

		return cls;
	}

	public static Class<?> addArrayToType(final Class<?> cls, final int arraysCount){
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
			//should not happen
			throw new IllegalArgumentException(e);
		}
	}

	public static void setFieldValue(final Field field, final Object obj, final Object value){
		try{
			field.set(obj, value);
		}
		catch(final IllegalArgumentException ignored){
			throw new IllegalArgumentException("Can not set " + field.getType().getSimpleName() + " field to "
				+ value.getClass().getSimpleName());
		}
		catch(final IllegalAccessException ignored){}
	}

	public static <T> void setFieldValue(final Object obj, final Class<T> fieldType, final T value){
		try{
			final DynamicArray<Field> fields = getAccessibleFields(obj.getClass(), fieldType);
			for(int i = 0; i < fields.limit; i ++)
				fields.data[i].set(obj, value);
		}
		catch(final IllegalArgumentException | IllegalAccessException ignored){}
	}

	/**
	 * Retrieve all declared fields in the current class AND in the parent classes.
	 *
	 * @param cls	The class from which to extract the declared fields.
	 * @return	An array of all the fields of the given class.
	 */
	public static DynamicArray<Field> getAccessibleFields(final Class<?> cls){
		return getAccessibleFields(cls, null);
	}

	/**
	 * Retrieve all declared fields in the current class AND in the parent classes.
	 *
	 * @param cls	The class from which to extract the declared fields.
	 * @param fieldType	The class for which to extract all the fields.
	 * @return	An array of all the fields of the given class.
	 */
	private static DynamicArray<Field> getAccessibleFields(Class<?> cls, final Class<?> fieldType){
		final DynamicArray<Field> fields = DynamicArray.create(Field.class, 0);

		//recurse classes:
		final Predicate<Field> filterPredicate = (fieldType != null? field -> (field.getType() == fieldType): null);
		final Consumer<DynamicArray<Field>> filter = (filterPredicate != null?
			subfields -> subfields.filter(filterPredicate):
			subfields -> {});
		while(cls != null && cls != Object.class){
			final DynamicArray<Field> subfields = DynamicArray.wrap(cls.getDeclaredFields());
			//apply filter on field type if needed
			filter.accept(subfields);
			//place parent's fields before all the child's fields
			fields.addAll(0, subfields);

			//go up to parent class
			cls = cls.getSuperclass();
		}

		//make fields accessible
		for(int i = 0; i < fields.limit; i ++)
			fields.data[i].setAccessible(true);
		return fields;
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
			//try create an instance
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
			catch(final Exception e){
				//should not happen
				LOGGER.error("Error while creating supplier", e);
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
		//Java 7 GAE was under a security manager so we use a degraded system
		if(PlatformDescription.isGoogleAppEngine() && "1.7".equals(PlatformDescription.SPECIFICATION_VERSION))
			instantiator = (Serializable.class.isAssignableFrom(type)?
				new ObjectInputStreamInstantiator<>(type):
				new AccessibleInstantiator<>(type));
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
