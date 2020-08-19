package org.reflections.adapters;

import io.github.mtrevisan.boxon.internal.DynamicArray;
import javassist.bytecode.AnnotationsAttribute;
import javassist.bytecode.ClassFile;
import javassist.bytecode.annotation.Annotation;
import org.reflections.ReflectionsException;
import org.reflections.vfs.Vfs;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;


public class JavassistAdapter implements MetadataAdapter<ClassFile>{

	/**
	 * Setting this to false will result in returning only visible annotations from the relevant methods here
	 * (only {@link java.lang.annotation.RetentionPolicy#RUNTIME}).
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
	public String[] getInterfacesNames(final ClassFile cls){
		return cls.getInterfaces();
	}

	@Override
	public String[] getClassAnnotationNames(final ClassFile type){
		final DynamicArray<String> list = DynamicArray.create(String.class, 2);

		AnnotationsAttribute attribute = (AnnotationsAttribute)type.getAttribute(AnnotationsAttribute.visibleTag);
		extractAnnotationNames(attribute, list);

		if(includeInvisibleTag){
			attribute = (AnnotationsAttribute)type.getAttribute(AnnotationsAttribute.invisibleTag);
			extractAnnotationNames(attribute, list);
		}

		return list.extractCopy();
	}

	private void extractAnnotationNames(final AnnotationsAttribute attribute, final DynamicArray<String> list){
		if(attribute != null){
			final Annotation[] annotations = attribute.getAnnotations();
			for(int i = 0; i < annotations.length; i ++)
				list.add(annotations[i].getTypeName());
		}
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
