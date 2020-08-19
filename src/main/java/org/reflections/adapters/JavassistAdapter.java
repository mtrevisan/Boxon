package org.reflections.adapters;

import javassist.bytecode.AnnotationsAttribute;
import javassist.bytecode.ClassFile;
import javassist.bytecode.annotation.Annotation;
import org.reflections.ReflectionsException;
import org.reflections.vfs.Vfs;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;


public class JavassistAdapter implements MetadataAdapter<ClassFile>{

	/**
	 * setting this to false will result in returning only visible annotations from the relevant methods here (only {@link java.lang.annotation.RetentionPolicy#RUNTIME})
	 */
	public static final boolean includeInvisibleTag = true;


	@Override
	public String getClassName(final ClassFile cls){
		return cls.getName();
	}

	@Override
	public String getSuperclassName(final ClassFile cls){
		return cls.getSuperclass();
	}

	@Override
	public List<String> getInterfacesNames(final ClassFile cls){
		return Arrays.asList(cls.getInterfaces());
	}


	@Override
	public List<String> getClassAnnotationNames(final ClassFile aClass){
		return getAnnotationNames(
			(AnnotationsAttribute)aClass.getAttribute(AnnotationsAttribute.visibleTag),
			(includeInvisibleTag? (AnnotationsAttribute)aClass.getAttribute(AnnotationsAttribute.invisibleTag): null)
		);
	}

	private List<String> getAnnotationNames(final AnnotationsAttribute... annotationsAttributes){
		if(annotationsAttributes != null)
			return Arrays.stream(annotationsAttributes)
				.filter(Objects::nonNull)
				.flatMap(annotationsAttribute -> Arrays.stream(annotationsAttribute.getAnnotations()))
				.map(Annotation::getTypeName)
				.collect(Collectors.toList());
		else
			return Collections.emptyList();
	}

	@Override
	public ClassFile getOrCreateClassObject(final Vfs.File file){
		try(final InputStream is = file.openInputStream()){
			return new ClassFile(new DataInputStream(new BufferedInputStream(is)));
		}
		catch(final IOException e){
			throw new ReflectionsException("could not create class file from " + file.getName(), e);
		}
	}

}
