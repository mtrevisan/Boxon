/**
 * Copyright (c) 2020 Mauro Trevisan
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */
package io.github.mtrevisan.boxon.internal.reflection.adapters;

import io.github.mtrevisan.boxon.internal.DynamicArray;
import io.github.mtrevisan.boxon.internal.reflection.vfs.VFSDirectory;
import io.github.mtrevisan.boxon.internal.reflection.vfs.VFSException;
import io.github.mtrevisan.boxon.internal.reflection.vfs.VFSFile;
import javassist.bytecode.AnnotationsAttribute;
import javassist.bytecode.ClassFile;
import javassist.bytecode.annotation.Annotation;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.InputStream;


class JavassistAdapter implements MetadataAdapterInterface<ClassFile>{

	/**
	 * Setting this to {@code false} will result in returning only visible annotations from the relevant methods here
	 * (only {@link java.lang.annotation.RetentionPolicy#RUNTIME RetentionPolicy.RUNTIME}).
	 */
	public static final boolean INCLUDE_INVISIBLE_TAG = true;


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

		if(INCLUDE_INVISIBLE_TAG){
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
	public ClassFile createClassObject(final VFSDirectory root, final VFSFile file){
		try(final InputStream is = root.openInputStream(file)){
			return new ClassFile(new DataInputStream(new BufferedInputStream(is)));
		}
		catch(final Exception e){
			throw new VFSException("Could not create class file from {}", file.getName())
				.withCause(e);
		}
	}

}