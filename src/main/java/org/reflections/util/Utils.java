package org.reflections.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;


public final class Utils{

	private Utils(){}

	private static String repeat(final int times){
		return IntStream.range(0, times).mapToObj(i -> "[]").collect(Collectors.joining());
	}

	public static boolean isEmpty(final String s){
		return s == null || s.length() == 0;
	}

	public static Logger getLogger(final Class<?> type){
		try{
			// This is to check whether an optional SLF4J binding is available. While SLF4J recommends that libraries
			// "should not declare a dependency on any SLF4J binding but only depend on slf4j-api", doing so forces
			// users of the library to either add a binding to the classpath (even if just slf4j-nop) or to set the
			// "slf4j.suppressInitError" system property in order to avoid the warning, which both is inconvenient.
			Class.forName("org.slf4j.impl.StaticLoggerBinder");
			return LoggerFactory.getLogger(type);
		}
		catch(final Throwable e){
			return null;
		}
	}

	private static String name(Class<?> type){
		if(!type.isArray())
			return type.getName();
		else{
			int dim = 0;
			while(type.isArray()){
				dim ++;
				type = type.getComponentType();
			}
			return type.getName() + repeat(dim);
		}
	}


	public static List<String> names(final Collection<Class<?>> types){
		return types.stream().map(Utils::name).collect(Collectors.toList());
	}

	public static String index(final Class<?> scannerClass){ return scannerClass.getSimpleName(); }

	public static <T> Set<T> filter(final Collection<T> result, final Predicate<? super T> predicate){
		return result.stream().filter(predicate).collect(Collectors.toSet());
	}

}
