package org.reflections.adapters;

import org.reflections.vfs.Vfs;

import java.util.List;


public interface MetadataAdapter<C, M>{

	String getClassName(final C cls);

	String getSuperclassName(final C cls);

	List<String> getInterfacesNames(final C cls);

	//
	List<String> getClassAnnotationNames(final C aClass);

	C getOrCreateClassObject(Vfs.File file) throws Exception;

	default boolean acceptsInput(final String file){
		return file.endsWith(".class");
	}

}
