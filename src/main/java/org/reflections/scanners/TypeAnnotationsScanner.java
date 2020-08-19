package org.reflections.scanners;

import org.reflections.ClassStore;


/**
 * Scans for class's annotations, where the annotation is marked with {@code @Retention(RetentionPolicy.RUNTIME)}.
 */
public class TypeAnnotationsScanner extends AbstractScanner{

	@SuppressWarnings("unchecked")
	public void scan(final Object cls, final ClassStore classStore){
		final String className = metadataAdapter.getClassName(cls);

		final String[] classAnnotationNames = metadataAdapter.getClassAnnotationNames(cls);
		for(int i = 0; i < classAnnotationNames.length; i ++)
			put(classStore, classAnnotationNames[i], className);
	}

}
