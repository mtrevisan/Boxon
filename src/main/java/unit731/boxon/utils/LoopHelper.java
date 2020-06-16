package unit731.boxon.utils;

import java.util.function.Predicate;


public class LoopHelper{

	private LoopHelper(){}


	public static <T> T match(final T[] array, final Predicate<T> condition){
		final int size = (array != null? array.length: 0);
		for(int i = 0; i < size; i ++){
			final T elem = array[i];
			if(condition.test(elem))
				return elem;
		}
		return null;
	}

}