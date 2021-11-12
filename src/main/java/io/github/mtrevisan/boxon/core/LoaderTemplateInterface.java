package io.github.mtrevisan.boxon.core;

import io.github.mtrevisan.boxon.exceptions.AnnotationException;


interface LoaderTemplateInterface{

	<T> Template<T> createTemplate(final Class<T> type) throws AnnotationException;

}
