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
package io.github.mtrevisan.boxon.helpers;

import io.github.mtrevisan.boxon.annotations.exceptions.AnnotationException;
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
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.function.Function;
import java.util.function.Supplier;


public final class ReflectionHelper{

	private static final Function<Class<?>, Supplier<?>> CREATORS = Memoizer.memoizeThreadAndRecursionSafe(ReflectionHelper::getCreatorInner);


	private ReflectionHelper(){}


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
			throw new IllegalArgumentException("Can not set " + field.getType().getSimpleName() + " field to " + value.getClass().getSimpleName());
		}
		catch(final IllegalAccessException ignored){}
	}

	public static <T> void setFieldValue(final Object obj, final Class<T> fieldType, final T value){
		try{
			final Field[] fields = getAccessibleFields(obj.getClass(), fieldType);
			for(int i = 0; i < fields.length; i ++)
				fields[i].set(obj, value);
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

	private static Field[] getAccessibleFields(Class<?> cls, final Class<?> fieldType){
		final Collection<Field> result = new ArrayList<>(0);
		while(cls != Object.class){
			final Field[] fields = cls.getDeclaredFields();
			result.addAll(filterAccessibleFields(fields, fieldType));

			//go up to parent class
			cls = cls.getSuperclass();
		}
		return result.toArray(Field[]::new);
	}

	private static Collection<Field> filterAccessibleFields(final Field[] fields, final Class<?> fieldType){
		final Collection<Field> result = new ArrayList<>(fields.length);
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
	public static <T> T getMethodResponse(final Object obj, final String methodName){
		try{
			final Method method = getAccessibleMethod(obj.getClass(), methodName);
			return (method != null? (T)method.invoke(obj): null);
		}
		catch(final IllegalAccessException | InvocationTargetException ignored){
			return null;
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


	/**
	 * Returns whether the given {@code type} is a primitive.
	 * <p>NOTE: {@code void} is NOT considered as primitive!</p>
	 *
	 * @param type	The class to query.
	 * @return	Whether the given {@code type} is a primitive.
	 */
	public static boolean isPrimitive(final Class<?> type){
		return (type.isPrimitive() && type != void.class);
	}

	/**
	 * Returns whether the given {@code type} is a primitive or primitive wrapper.
	 * <p>NOTE: {@code Character} and {@code void}/{@code Void} are NOT considered as primitives!</p>
	 *
	 * @param type	The class to query.
	 * @return	Whether the given {@code type} is a primitive or primitive wrapper.
	 */
	public static boolean isPrimitiveOrPrimitiveWrapper(final Class<?> type){
		return (isPrimitive(type) || DataType.isObjectivePrimitive(type));
	}


	public static Object createArrayPrimitive(final Class<?> type, final int length){
		if(!ReflectionHelper.isPrimitive(type))
			throw new AnnotationException("Argument cannot be a non-primitive: {}", type);

		return Array.newInstance(type, length);
	}

	@SuppressWarnings("unchecked")
	public static <T> T[] createArray(final Class<? extends T> type, final int length){
		if(ReflectionHelper.isPrimitive(type))
			throw new AnnotationException("Argument cannot be a primitive: {}", type);

		return (T[])Array.newInstance(type, length);
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
	 * @see <a href="https://github.com/easymock/objenesis">objenesis</a>
	 *
	 * @param type Class to instantiate
	 * @return The ObjectInstantiator for the class
	 */
	private static <T> ObjectInstantiator<T> instantiatorOf(final Class<T> type){
		ObjectInstantiator<T> instantiator;
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
