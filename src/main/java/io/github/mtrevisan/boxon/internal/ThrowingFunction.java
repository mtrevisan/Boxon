package io.github.mtrevisan.boxon.internal;

@FunctionalInterface
public interface ThrowingFunction<IN, OUT, E extends Exception>{
	OUT apply(final IN in) throws E;
}
