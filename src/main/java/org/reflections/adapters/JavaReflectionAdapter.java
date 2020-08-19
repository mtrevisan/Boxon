package org.reflections.adapters;

import org.reflections.vfs.Vfs;

import java.lang.annotation.Annotation;
import java.lang.reflect.Member;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.reflections.ReflectionUtils.forName;


public class JavaReflectionAdapter implements MetadataAdapter<Class<?>, Member>{

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
	public List<String> getInterfacesNames(final Class<?> cls){
		final Class<?>[] classes = cls.getInterfaces();
		return Arrays.stream(classes)
			.map(Class::getName)
			.collect(Collectors.toList());
	}


	@Override
	public List<String> getClassAnnotationNames(final Class<?> cls){
		return getAnnotationNames(cls.getDeclaredAnnotations());
	}

	private List<String> getAnnotationNames(final Annotation[] annotations){
		return Arrays.stream(annotations)
			.map(annotation -> annotation.annotationType().getName())
			.collect(Collectors.toList());
	}

	@Override
	public Class<?> getOrCreateClassObject(final Vfs.File file){
		return getOrCreateClassObject(file, (ClassLoader)null);
	}

	public Class<?> getOrCreateClassObject(final Vfs.File file, final ClassLoader... loaders){
		final String name = file.getRelativePath()
			.replace("/", ".")
			.replace(".class", "");
		return forName(name, loaders);
	}

}
