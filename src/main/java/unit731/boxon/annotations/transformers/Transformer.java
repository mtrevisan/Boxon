package unit731.boxon.annotations.transformers;


public interface Transformer<IN, OUT>{

	OUT decode(final IN value);

	IN encode(final OUT value);

}
