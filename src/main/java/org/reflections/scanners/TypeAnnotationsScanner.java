package org.reflections.scanners;

import org.reflections.Store;
import org.reflections.adapters.MetadataAdapter;


/**
 * Scans for class's annotations, where the annotation is marked with {@code @Retention(RetentionPolicy.RUNTIME)}.
 */
@SuppressWarnings({"unchecked"})
public class TypeAnnotationsScanner extends AbstractScanner{

	public void scan(final Object cls, final Store store){
		@SuppressWarnings("rawtypes")
		final MetadataAdapter metadataAdapter = getMetadataAdapter();
		final String className = metadataAdapter.getClassName(cls);

		final String[] classAnnotationNames = metadataAdapter.getClassAnnotationNames(cls);
		for(int i = 0; i < classAnnotationNames.length; i ++)
			put(store, classAnnotationNames[i], className);
	}

}
