package io.github.mtrevisan.boxon.codecs;


import io.github.mtrevisan.boxon.external.CodecInterface;


interface LoaderCodecInterface{

	boolean hasCodec(final Class<?> type);

	CodecInterface<?> getCodec(final Class<?> type);

}
