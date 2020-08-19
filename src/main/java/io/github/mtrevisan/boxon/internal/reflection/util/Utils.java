package io.github.mtrevisan.boxon.internal.reflection.util;

import java.util.Collection;
import java.util.HashSet;
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

	public static <T> Set<T> filter(final Collection<T> result, final Predicate<? super T> predicate){
		final Set<T> set = new HashSet<>();
		for(final T t : result)
			if(predicate.test(t))
				set.add(t);
		return set;
	}

}
