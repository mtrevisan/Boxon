package io.github.mtrevisan.boxon.core;


interface LoaderCodecInterface{

	boolean hasCodec(final Class<?> type);

	CodecInterface<?> getCodec(final Class<?> type);

}
