package org.reflections.adapters;

import org.reflections.ReflectionUtils;
import org.reflections.vfs.Vfs;

import java.lang.annotation.Annotation;


public class JavaReflectionAdapter implements MetadataAdapter<Class<?>>{

	@Override
	public String getClassName(final Class<?> cls){
		return cls.getName();
	}

	@Override
	public String getSuperclassName(final Class<?> cls){
		final Class<?> superclass = cls.getSuperclass();
		return (superclass != null? superclass.getName(): "");
	}

	@Override
	public String[] getInterfacesNames(final Class<?> cls){
		final Class<?>[] classes = cls.getInterfaces();
		final String[] result = new String[classes.length];
		for(int i = 0; i < classes.length; i ++)
			result[i] = classes[i].getName();
		return result;
	}


	@Override
	public String[] getClassAnnotationNames(final Class<?> cls){
		final Annotation[] annotations = cls.getDeclaredAnnotations();
		final String[] result = new String[annotations.length];
		for(int i = 0; i < annotations.length; i ++)
			result[i] = annotations[i].annotationType().getName();
		return result;
	}

	@Override
	public Class<?> getOrCreateClassObject(final Vfs.File file){
		final String name = file.getRelativePath()
			.replace("/", ".")
			.replace(".class", "");
		return ReflectionUtils.forName(name);
	}

}
