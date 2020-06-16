package unit731.boxon.other;

import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;


public class ReversedIterator<T> implements Iterable<T>{

	private final List<T> list;


	public static <T> ReversedIterator<T> reversed(final List<T> list){
		return new ReversedIterator<>(list);
	}

	private ReversedIterator(final List<T> list){
		this.list = list;
	}

	@Override
	public Iterator<T> iterator(){
		final ListIterator<T> itr = list.listIterator(list.size());

		return new Iterator<>(){
			@Override
			public boolean hasNext(){
				return itr.hasPrevious();
			}

			@Override
			public T next(){
				return itr.previous();
			}

			@Override
			public void remove(){
				itr.remove();
			}
		};
	}

}
