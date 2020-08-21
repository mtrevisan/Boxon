package io.github.mtrevisan.boxon.internal.reflection.adapters;

import io.github.mtrevisan.boxon.internal.JavaHelper;
import org.slf4j.Logger;


public final class MetadataAdapterBuilder{

	private static final Logger LOGGER = JavaHelper.getLoggerFor(MetadataAdapterBuilder.class);

	private static MetadataAdapterInterface<?> INSTANCE;
	static{
		/**
		 * if javassist library exists in the classpath, this method returns {@link JavassistAdapter} otherwise defaults to {@link JavaReflectionAdapter}.
		 * <p>the {@link JavassistAdapter} is preferred in terms of performance and class loading.
		 */
		try{
			INSTANCE = new JavassistAdapter();
		}
		catch(final Throwable e){
			if(LOGGER != null)
				LOGGER.warn("could not create JavassistAdapter, using JavaReflectionAdapter", e);

			INSTANCE = new JavaReflectionAdapter();
		}
	}


	private MetadataAdapterBuilder(){}

	public static final MetadataAdapterInterface<?> getMetadataAdapter(){
		return INSTANCE;
	}

}
