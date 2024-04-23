package io.github.mtrevisan.boxon.core.helpers.describer;

import io.github.mtrevisan.boxon.exceptions.AnnotationException;
import io.github.mtrevisan.boxon.exceptions.BoxonException;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
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
	 * @param mapper	A function that takes an entity of type T and returns its corresponding map representation.
	 * @return	The list of descriptions for the entities.
	 * @throws BoxonException   If a field exception occurs during the mapping process.
	 * @param <T>	The type of the entities.
	 */
	static <T> List<Map<String, Object>> describeEntities(final Collection<T> entities,
			final ThrowingFunction<T, Map<String, Object>, BoxonException> mapper) throws BoxonException{
		final List<Map<String, Object>> descriptions = new ArrayList<>(entities.size());
		for(final T entity : entities)
			descriptions.add(mapper.apply(entity));
		return Collections.unmodifiableList(descriptions);
	}

	/**
	 * Describes the entities by mapping them to a list of maps using the provided mapper function.
	 *
	 * @param <T>	The type of the entities.
	 * @param <E>	The type of the exception.
	 * @param annotationClass	The annotation class to check for annotated entities.
	 * @param entitiesClass	The array of entity classes to be described.
	 * @param extractor	The function to extract the entity.
	 * @param mapper	The function that maps the entity to its corresponding map representation.
	 * @return	The list of descriptions for the entities.
	 * @throws BoxonException   If a field exception occurs during the mapping process.
	 * @throws E	If any other exception occurs during the extraction or mapping process.
	 */
	static <T, E extends Exception> List<Map<String, Object>> describeEntities(final Class<? extends Annotation> annotationClass,
			final Class<?>[] entitiesClass, final ThrowingFunction<Class<?>, T, E> extractor,
			final ThrowingFunction<T, Map<String, Object>, BoxonException> mapper) throws BoxonException, E{
		final List<Map<String, Object>> description = new ArrayList<>(entitiesClass.length);
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
	 * @return	The map representation of the entity.
	 * @throws BoxonException   If a field exception occurs during the mapping process.
	 * @throws E	If any other exception occurs during the extraction or mapping process.
	 * @param <T>	The type of the entity.
	 * @param <E>	The type of the exception.
	 */
	static <T, E extends Exception> Map<String, Object> describeEntity(final Class<? extends Annotation> annotationClass,
			final Class<?> entityClass, final ThrowingFunction<Class<?>, T, E> extractor,
			final ThrowingFunction<T, Map<String, Object>, BoxonException> mapper) throws BoxonException, E{
		if(!entityClass.isAnnotationPresent(annotationClass))
			throw AnnotationException.create("Entity {} didn't have the `{}` annotation", entityClass.getSimpleName(),
				annotationClass.getSimpleName());

		final T entity = extractor.apply(entityClass);
		return Collections.unmodifiableMap(mapper.apply(entity));
	}


	@FunctionalInterface
	interface ThrowingFunction<T, R, E extends Exception>{

		R apply(T t) throws E;

	}

}
