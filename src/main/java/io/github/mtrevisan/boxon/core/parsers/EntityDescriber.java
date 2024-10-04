/*
 * Copyright (c) 2024 Mauro Trevisan
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
package io.github.mtrevisan.boxon.core.parsers;

import io.github.mtrevisan.boxon.exceptions.AnnotationException;
import io.github.mtrevisan.boxon.exceptions.BoxonException;
import io.github.mtrevisan.boxon.helpers.JavaHelper;
import io.github.mtrevisan.boxon.helpers.ThrowingFunction;

import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;


final class EntityDescriber{

	private EntityDescriber(){}


	/**
	 * Describes the entities by mapping them to a list of maps using the provided mapper function.
	 * <p>Each entity is transformed into a map representation where the keys are the field names and the values are the corresponding field
	 * values.</p>
	 *
	 * @param entities	The collection of entities to be described.
	 * @param mapper	A function that takes an entity of type {@code T} and returns its corresponding map representation.
	 * @param <T>	The type of the entities.
	 * @throws BoxonException	If a field exception occurs during the mapping process.
	 * @return	The list of descriptions for the entities.
	 */
	static <T> List<Map<String, Object>> describeEntities(final Collection<T> entities,
			final ThrowingFunction<T, Map<String, Object>, BoxonException> mapper) throws BoxonException{
		final List<Map<String, Object>> descriptions = JavaHelper.createListOrEmpty(entities.size());
		for(final T entity : entities)
			descriptions.add(mapper.apply(entity));
		return Collections.unmodifiableList(descriptions);
	}

	/**
	 * Describes the entities by mapping them to a list of maps using the provided mapper function.
	 *
	 * @param annotationClass	The annotation class to check for annotated entities.
	 * @param entitiesClass	The array of entity classes to be described.
	 * @param extractor	The function to extract the entity.
	 * @param mapper	The function that maps the entity to its corresponding map representation.
	 * @param <T>	The type of the entities.
	 * @param <E>	The type of the exception.
	 * @throws BoxonException	If a field exception occurs during the mapping process.
	 * @throws E	If any other exception occurs during the extraction or mapping process.
	 * @return	The list of descriptions for the entities.
	 */
	static <T, E extends Exception> List<Map<String, Object>> describeEntities(final Class<? extends Annotation> annotationClass,
			final Class<?>[] entitiesClass, final ThrowingFunction<Class<?>, T, E> extractor,
			final ThrowingFunction<T, Map<String, Object>, BoxonException> mapper) throws BoxonException, E{
		final List<Map<String, Object>> description = JavaHelper.createListOrEmpty(entitiesClass.length);
		for(final Class<?> entityClass : entitiesClass)
			if(entityClass.isAnnotationPresent(annotationClass)){
				final T entity = extractor.apply(entityClass);
				description.add(mapper.apply(entity));
			}
		return Collections.unmodifiableList(description);
	}

	/**
	 * Describes an entity by mapping it to a map representation using the provided mapper function.
	 *
	 * @param annotationClass	The annotation class to check for the entity.
	 * @param entityClass	The entity class to be described.
	 * @param extractor	The function to extract the entity.
	 * @param mapper	The function that maps the entity to its corresponding map representation.
	 * @param <T>	The type of the entity.
	 * @param <E>	The type of the exception.
	 * @throws BoxonException	If a field exception occurs during the mapping process.
	 * @throws E	If any other exception occurs during the extraction or mapping process.
	 * @return	The map representation of the entity.
	 */
	static <T, E extends Exception> Map<String, Object> describeEntity(final Class<? extends Annotation> annotationClass,
			final Class<?> entityClass, final ThrowingFunction<Class<?>, T, E> extractor,
			final ThrowingFunction<T, Map<String, Object>, BoxonException> mapper) throws BoxonException, E{
		if(!entityClass.isAnnotationPresent(annotationClass))
			throw AnnotationException.create("Entity {} didn't have the `{}` annotation",
				entityClass.getSimpleName(), annotationClass.getSimpleName());

		final T entity = extractor.apply(entityClass);
		return mapper.apply(entity);
	}

}
