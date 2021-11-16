package io.github.mtrevisan.boxon.core.codecs;

import io.github.mtrevisan.boxon.exceptions.AnnotationException;


interface LoaderTemplateInterface{

	<T> Template<T> createTemplate(final Class<T> type) throws AnnotationException;

}
