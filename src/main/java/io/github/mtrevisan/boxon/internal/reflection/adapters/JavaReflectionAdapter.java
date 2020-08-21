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

import io.github.mtrevisan.boxon.internal.reflection.ClasspathHelper;
import io.github.mtrevisan.boxon.internal.reflection.vfs.VFSDirectory;
import io.github.mtrevisan.boxon.internal.reflection.vfs.VFSFile;

import java.lang.annotation.Annotation;


class JavaReflectionAdapter implements MetadataAdapterInterface<Class<?>>{

	private static final String EMPTY_STRING = "";
	private static final String SLASH = "/";
	private static final String DOT = ".";
	private static final String DOT_CLASS = ".class";


	@Override
	public String getClassName(final Class<?> cls){
		return cls.getName();
	}

	@Override
	public String getSuperclassName(final Class<?> cls){
		final Class<?> superclass = cls.getSuperclass();
		return (superclass != null? superclass.getName(): EMPTY_STRING);
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
	public Class<?> createClassObject(final VFSDirectory root, final VFSFile file){
		final String name = file.getRelativePath()
			.replace(SLASH, DOT)
			.replace(DOT_CLASS, EMPTY_STRING);
		return ClasspathHelper.getClassFromName(name);
	}

}
