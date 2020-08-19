package io.github.mtrevisan.boxon.internal.reflection;


public class ReflectionsException extends RuntimeException{

	public ReflectionsException(final String message){
		super(message);
	}

	public ReflectionsException(final String message, final Throwable cause){
		super(message, cause);
	}

}
