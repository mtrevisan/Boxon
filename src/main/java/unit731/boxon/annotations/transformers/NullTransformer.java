package unit731.boxon.annotations.transformers;


public class NullTransformer<T> implements Transformer<T, T>{

	@Override
	public T decode(final T value){
		return value;
	}

	@Override
	public T encode(final T value){
		return value;
	}

}
