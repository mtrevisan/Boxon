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
package io.github.mtrevisan.boxon.codecs.managers;

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
import java.util.function.Function;
import java.util.function.Supplier;


public final class ConstructorHelper{

	private static final Function<Class<?>, Supplier<?>> CREATORS = Memoizer.memoize(ConstructorHelper::getCreatorInner);


	private ConstructorHelper(){}

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

	@SuppressWarnings("ReturnOfNull")
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
