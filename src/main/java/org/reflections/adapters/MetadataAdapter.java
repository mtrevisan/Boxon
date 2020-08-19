package org.reflections.adapters;

import org.reflections.vfs.Vfs;


public interface MetadataAdapter<C>{

	String getClassName(final C type);

	String getSuperclassName(final C type);

	String[] getInterfacesNames(final C type);

	String[] getClassAnnotationNames(final C type);

	C getOrCreateClassObject(final Vfs.File file);

	default boolean acceptsInput(final String file){
		return file.endsWith(".class");
	}

}
