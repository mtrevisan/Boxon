package org.reflections.util;

import org.reflections.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;


/**
 * a garbage can of convenient methods
 */
public abstract class Utils{

	public static String repeat(String string, int times){
		return IntStream.range(0, times).mapToObj(i -> string).collect(Collectors.joining());
	}

	public static boolean isEmpty(String s){
		return s == null || s.length() == 0;
	}

	public static void close(InputStream closeable){
		try{
			if(closeable != null)
				closeable.close();
		}catch(IOException e){
			if(Reflections.log != null){
				Reflections.log.warn("Could not close InputStream", e);
			}
		}
	}

	public static Logger findLogger(Class<?> aClass){
		try{
			// This is to check whether an optional SLF4J binding is available. While SLF4J recommends that libraries
			// "should not declare a dependency on any SLF4J binding but only depend on slf4j-api", doing so forces
			// users of the library to either add a binding to the classpath (even if just slf4j-nop) or to set the
			// "slf4j.suppressInitError" system property in order to avoid the warning, which both is inconvenient.
			Class.forName("org.slf4j.impl.StaticLoggerBinder");
			return LoggerFactory.getLogger(aClass);
		}catch(Throwable e){
			return null;
		}
	}

	public static String name(Class type){
		if(!type.isArray()){
			return type.getName();
		}
		else{
			int dim = 0;
			while(type.isArray()){
				dim++;
				type = type.getComponentType();
			}
			return type.getName() + repeat("[]", dim);
		}
	}


	public static List<String> names(Collection<Class<?>> types){
		return types.stream().map(Utils::name).collect(Collectors.toList());
	}

	public static List<String> names(Class<?>... types){
		return names(Arrays.asList(types));
	}

	public static String name(Constructor constructor){
		return constructor.getName() + "." + "<init>" + "(" + join(names(constructor.getParameterTypes()), ", ") + ")";
	}

	public static String name(Method method){
		return method.getDeclaringClass().getName() + "." + method.getName() + "(" + join(names(method.getParameterTypes()), ", ") + ")";
	}

	public static String name(Field field){
		return field.getDeclaringClass().getName() + "." + field.getName();
	}

	public static String index(Class<?> scannerClass){ return scannerClass.getSimpleName(); }

	public static <T> Predicate<T> and(Predicate... predicates){
		return Arrays.stream(predicates).reduce(t -> true, Predicate::and);
	}

	public static String join(Collection<?> elements, String delimiter){
		return elements.stream().map(Object::toString).collect(Collectors.joining(delimiter));
	}

	public static <T> Set<T> filter(Collection<T> result, Predicate<? super T>... predicates){
		return result.stream().filter(and(predicates)).collect(Collectors.toSet());
	}

	public static <T> Set<T> filter(Collection<T> result, Predicate<? super T> predicate){
		return result.stream().filter(predicate).collect(Collectors.toSet());
	}

	public static <T> Set<T> filter(T[] result, Predicate<? super T>... predicates){
		return Arrays.stream(result).filter(and(predicates)).collect(Collectors.toSet());
	}
}
