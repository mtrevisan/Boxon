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
package unit731.boxon.helpers;

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
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;


public class ReflectionHelper{

	/** Map with primitive type as key and corresponding objective type as value, for example: "int.class" -> "Integer.class" */
	private static final Map<Class<?>, Class<?>> PRIMITIVE_TYPE = new HashMap<>(6);
	static{
		PRIMITIVE_TYPE.put(byte.class, Byte.class);
		PRIMITIVE_TYPE.put(short.class, Short.class);
		PRIMITIVE_TYPE.put(int.class, Integer.class);
		PRIMITIVE_TYPE.put(long.class, Long.class);
		PRIMITIVE_TYPE.put(float.class, Float.class);
		PRIMITIVE_TYPE.put(double.class, Double.class);
	}


	private ReflectionHelper(){}


	@SuppressWarnings("unchecked")
	public static <T> T getFieldValue(final Object obj, final String fieldName) throws NoSuchFieldException{
		try{
			final Field field = getAccessibleField(obj.getClass(), fieldName);
			return (T)field.get(obj);
		}
		catch(final IllegalAccessException ignored){
			//cannot happen
			return null;
		}
	}

	public static void setFieldValue(final Object obj, final String fieldName, final Object value) throws NoSuchFieldException{
		try{
			final Field field = getAccessibleField(obj.getClass(), fieldName);
			field.set(obj, value);
		}
		catch(final IllegalAccessException ignored){}
	}

	public static <T> void setFieldValue(final Object obj, final Class<T> fieldType, final T value) throws NoSuchFieldException{
		try{
			final Field[] fields = getAccessibleFields(obj.getClass(), fieldType);
			for(final Field field : fields)
				field.set(obj, value);
		}
		catch(final IllegalAccessException ignored){}
	}

	private static Field getAccessibleField(Class<?> cls, final String fieldName) throws NoSuchFieldException{
		Field field;
		while(true){
			try{
				field = cls.getDeclaredField(fieldName);
				field.setAccessible(true);
				break;
			}
			catch(final NoSuchFieldException e){
				//go up to parent class
				cls = cls.getSuperclass();
				if(cls == Object.class)
					throw e;
			}
		}
		return field;
	}

	private static <T> Field[] getAccessibleFields(Class<?> cls, final Class<?> fieldType){
		Field[] result = new Field[0];
		while(cls != Object.class){
			final Field[] fields = cls.getDeclaredFields();
			for(final Field f : fields)
				if(f.getType() == fieldType){
					f.setAccessible(true);
					result = add(result, f);
					break;
				}

			//go up to parent class
			cls = cls.getSuperclass();
		}
		return result;
	}

	private static <T> T[] add(final T[] array, final T element){
		//copy & grow
		final int arrayLength = Array.getLength(array);
		final Object newArray = Array.newInstance(array.getClass().getComponentType(), arrayLength + 1);
		System.arraycopy(array, 0, newArray, 0, arrayLength);
		Array.set(newArray, arrayLength, element);
		return (T[])newArray;
	}

	/**
	 * Get the class that extends {@link Object} that represent the given class.
	 *
	 * @param cls	Class to get the object class of
	 * @return the class that extends Object class and represent the given class
	 */
	public static Class<?> objectiveType(final Class<?> cls){
		return PRIMITIVE_TYPE.getOrDefault(cls, cls);
	}

	public static Object createArrayPrimitive(final Class<?> type, final int length){
		Objects.requireNonNull(type);
		if(!type.getComponentType().isPrimitive())
			throw new IllegalArgumentException("Argument cannot be a non-primitive: " + type);

		return Array.newInstance(type.getComponentType(), length);
	}

	@SuppressWarnings("unchecked")
	public static <T> T[] createArray(final Class<? extends T> type, final int length){
		Objects.requireNonNull(type);
		if(type.isPrimitive())
			throw new IllegalArgumentException("Argument cannot be a primitive: " + type);

		return (T[])Array.newInstance(type, length);
	}

	//https://github.com/easymock/objenesis
	public static <T> T createInstance(final Class<T> type){
		Objects.requireNonNull(type);

		return instantiatorOf(type).newInstance();
	}

	/**
	 * Return an {@link ObjectInstantiator} allowing to create instance without any constructor being
	 * called.
	 *
	 * @param type Class to instantiate
	 * @return The ObjectInstantiator for the class
	 */
	private static <T> ObjectInstantiator<T> instantiatorOf(final Class<T> type){
		ObjectInstantiator<T> instantiator;
		if(PlatformDescription.isThisJVM(PlatformDescription.HOTSPOT) || PlatformDescription.isThisJVM(PlatformDescription.OPENJDK)){
			//Java 7 GAE was under a security manager so we use a degraded system
			if(PlatformDescription.isGoogleAppEngine() && PlatformDescription.SPECIFICATION_VERSION.equals("1.7"))
				instantiator = (Serializable.class.isAssignableFrom(type)?
					new ObjectInputStreamInstantiator<>(type):
					new AccessibleInstantiator<>(type));
			else
				//the UnsafeFactoryInstantiator would also work, but according to benchmarks, it is 2.5 times slower
				instantiator = new SunReflectionFactoryInstantiator<>(type);
		}
		else if(PlatformDescription.isThisJVM(PlatformDescription.DALVIK)){
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
		}
		else if(PlatformDescription.isThisJVM(PlatformDescription.GNU))
			instantiator = new GCJInstantiator<>(type);
		else if(PlatformDescription.isThisJVM(PlatformDescription.PERC))
			instantiator = new PercInstantiator<>(type);
		else
			//fallback instantiator, should work with most modern JVM
			instantiator = new UnsafeFactoryInstantiator<>(type);

		return instantiator;
	}

}
