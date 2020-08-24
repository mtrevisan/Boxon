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
package io.github.mtrevisan.boxon.internal.reflection.helpers;

import io.github.mtrevisan.boxon.internal.DynamicArray;
import io.github.mtrevisan.boxon.internal.Memoizer;
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

import java.io.Serializable;
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
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;


/**
 * @see <a href="https://bill.burkecentral.com/2008/01/14/scanning-java-annotations-at-runtime/">Scanning Java Annotations at Runtime</a>
 */
public final class ReflectionHelper{

	private static final Function<Class<?>, Supplier<?>> CREATORS = Memoizer.memoizeThreadAndRecursionSafe(ReflectionHelper::getCreatorInner);

	/** Would include {@code Object.class} when calling {@link #getSuperTypes(Class)}. */
	private static final boolean INCLUDE_OBJECT = false;


	private ReflectionHelper(){}


	/**
	 * Get the immediate supertype and interfaces of the given {@code type}.
	 *
	 * @param type	The class.
	 * @return	The set of classes.
	 */
	public static Set<Class<?>> getSuperTypes(final Class<?> type){
		final Set<Class<?>> result = new LinkedHashSet<>();
		final Class<?> superclass = type.getSuperclass();
		final Class<?>[] interfaces = type.getInterfaces();
		if(superclass != null && (INCLUDE_OBJECT || !superclass.equals(Object.class)))
			result.add(superclass);
		if(interfaces.length > 0)
			result.addAll(Arrays.asList(interfaces));
		return result;
	}


	/**
	 * Retrieve fields list of specified class.
	 *
	 * @param cls	The class from which to extract the declared fields.
	 * @param recursively	If {@code true}, it retrieves fields from all class hierarchy.
	 * @return	An array of all the fields of the given class.
	 */
	public static DynamicArray<Field> getDeclaredFields(final Class<?> cls, final boolean recursively){
		final DynamicArray<Field> fields;
		if(recursively){
			fields = DynamicArray.create(Field.class);
			Class<?> currentType = cls;
			while(currentType != Object.class){
				final Field[] subfields = currentType.getDeclaredFields();
				//place parent's fields before all the child's fields
				fields.addAll(0, subfields);

				currentType = currentType.getSuperclass();
			}
		}
		else
			fields = DynamicArray.createFrom(cls.getDeclaredFields());
		return fields;
	}

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
	 */
	public static <T> Class<?> resolveGenericType(final Class<? extends T> offspring, final Class<T> base, Type... actualArgs){
		//if actual types are omitted, the type parameters will be used instead
		if(actualArgs.length == 0)
			actualArgs = offspring.getTypeParameters();

		//map type parameters into the actual types
		final Map<String, Type> typeVariables = mapParameterTypes(offspring, actualArgs);

		//find direct ancestors (superclass and interfaces)
		final Queue<Type> ancestorsQueue = extractAncestors(offspring);

		//iterate over ancestors
		Class<?> type = null;
		while(type == null && !ancestorsQueue.isEmpty()){
			final Type ancestorType = ancestorsQueue.poll();

			if(ancestorType instanceof ParameterizedType)
				//ancestor is parameterized: process only if the raw type matches the base class
				type = manageParameterizedAncestor((ParameterizedType)ancestorType, base, typeVariables);
			else if(ancestorType instanceof Class<?> && base.isAssignableFrom((Class<?>)ancestorType))
				//ancestor is non-parameterized: process only if it matches the base class
				ancestorsQueue.add(ancestorType);
		}
		if(type == null && offspring.equals(base))
			//there is a result if the base class is reached
			type = getClassFromName(actualArgs[0]);
		return type;
	}

	private static <T> Class<?> manageParameterizedAncestor(final ParameterizedType ancestorType, final Class<T> base,
			final Map<String, Type> typeVariables){
		Class<?> type = null;
		final Type rawType = ancestorType.getRawType();
		if(rawType instanceof Class<?> && base.isAssignableFrom((Class<?>)rawType)){
			final Type resolvedType = resolveArgumentType(ancestorType.getActualTypeArguments()[0], typeVariables);
			type = getClassFromName(resolvedType);
		}
		return type;
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

	private static Class<?> getClassFromName(final Type type){
		try{
			return Class.forName(type.getTypeName());
		}
		catch(final ClassNotFoundException ignored){
			return null;
		}
	}


	//https://www.jboss.org/optaplanner/blog/2018/01/09/JavaReflectionButMuchFaster.html
	@SuppressWarnings("unchecked")
	public static <T> T getFieldValue(final Object obj, final String fieldName){
		try{
			final Field field = getAccessibleField(obj.getClass(), fieldName);
			return (T)field.get(obj);
		}
		catch(final IllegalArgumentException | IllegalAccessException ignored){
			//cannot happen
			return null;
		}
	}

	public static void setFieldValue(final Object obj, final String fieldName, final Object value){
		final Field field = getAccessibleField(obj.getClass(), fieldName);
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

	private static Field getAccessibleField(Class<?> cls, final String fieldName){
		Field field = null;
		while(cls != Object.class){
			try{
				field = cls.getDeclaredField(fieldName);
				field.setAccessible(true);
				break;
			}
			catch(final NoSuchFieldException | SecurityException e){
				//go up to parent class
				cls = cls.getSuperclass();
			}
		}
		return field;
	}

	private static DynamicArray<Field> getAccessibleFields(Class<?> cls, final Class<?> fieldType){
		final DynamicArray<Field> result = DynamicArray.create(Field.class, 0);
		while(cls != Object.class){
			final Field[] fields = cls.getDeclaredFields();
			result.addAll(filterAccessibleFields(fields, fieldType));

			//go up to parent class
			cls = cls.getSuperclass();
		}
		return result;
	}

	private static DynamicArray<Field> filterAccessibleFields(final Field[] fields, final Class<?> fieldType){
		final DynamicArray<Field> result = DynamicArray.create(Field.class, fields.length);
		for(int i = 0; i < fields.length; i ++){
			final Field field = fields[i];
			if(field.getType() == fieldType){
				field.setAccessible(true);
				result.add(field);
			}
		}
		return result;
	}


	@SuppressWarnings("unchecked")
	public static <T> T getMethodResponse(final Object obj, final String methodName, final T defaultValue){
		try{
			final Method method = getAccessibleMethod(obj.getClass(), methodName);
			return (method != null? (T)method.invoke(obj): defaultValue);
		}
		catch(final IllegalAccessException | InvocationTargetException ignored){
			return defaultValue;
		}
	}

	private static Method getAccessibleMethod(Class<?> cls, final String methodName){
		Method method = null;
		while(cls != Object.class){
			try{
				method = cls.getDeclaredMethod(methodName);
				method.setAccessible(true);
				break;
			}
			catch(final NoSuchMethodException e){
				//go up to parent class
				cls = cls.getSuperclass();
			}
		}
		return method;
	}


	@SuppressWarnings("unchecked")
	public static <T> Supplier<T> getCreator(final Class<T> type){
		return (Supplier<T>)CREATORS.apply(type);
	}

	private static <T> Supplier<T> getCreatorInner(final Class<T> type){
		try{
			final Constructor<T> constructor = type.getDeclaredConstructor();
			constructor.setAccessible(true);
			//try create an instance
			constructor.newInstance();

			return createSupplierIgnoreExceptions(constructor);
		}
		catch(final Exception ignored){
			return instantiatorOf(type)::newInstance;
		}
	}

	private static <T> Supplier<T> createSupplierIgnoreExceptions(final Constructor<T> constructor){
		return () -> {
			try{
				return constructor.newInstance();
			}
			catch(final Exception ignored){
				//cannot happen
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
		if(PlatformDescription.isGoogleAppEngine() && PlatformDescription.SPECIFICATION_VERSION.equals("1.7"))
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
