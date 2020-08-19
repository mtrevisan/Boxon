package io.github.mtrevisan.boxon.internal.reflection;

import io.github.mtrevisan.boxon.internal.reflection.util.ClasspathHelper;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;


@SuppressWarnings("unchecked")
public abstract class ReflectionUtils{

	/**
	 * would include {@code Object.class} when {@link #getSuperTypes(Class)}. default is false.
	 */
	private static final boolean includeObject = false;


	/**
	 * get the immediate supertype and interfaces of the given {@code type}
	 *
	 * @param type	The class.
	 * @return	The set of classes.
	 */
	static Set<Class<?>> getSuperTypes(final Class<?> type){
		final Set<Class<?>> result = new LinkedHashSet<>();
		final Class<?> superclass = type.getSuperclass();
		final Class<?>[] interfaces = type.getInterfaces();
		if(superclass != null && (includeObject || !superclass.equals(Object.class)))
			result.add(superclass);
		if(interfaces.length > 0)
			result.addAll(Arrays.asList(interfaces));
		return result;
	}

	//predicates

	/**
	 * where element is annotated with given {@code annotation}, including member matching
	 *
	 * @param annotation	The annotation.
	 * @return	The predicate.
	 * @param <T>	The type of the returned predicate.
	 */
	static <T extends AnnotatedElement> Predicate<T> withAnnotation(final Annotation annotation){
		return input -> input != null && input.isAnnotationPresent(annotation.annotationType()) && areAnnotationMembersMatching(input.getAnnotation(annotation.annotationType()), annotation);
	}

	public static Class<?> forName(final String typeName, final ClassLoader... classLoaders){
		final List<String> primitiveNames = getPrimitiveNames();
		int index = primitiveNames.indexOf(typeName);
		if(index >= 0)
			return getPrimitiveTypes().get(index);
		else{
			String type = typeName;
			index = typeName.indexOf("[");
			if(index >= 0){
				final String array = typeName.substring(index).replace("]", "");

				type = typeName.substring(0, index);
				index = primitiveNames.indexOf(type);
				type = array + (index >= 0? getPrimitiveDescriptors().get(index): "L" + type + ";");
			}


			final List<ReflectionsException> reflectionsExceptions = new ArrayList<>();
			for(final ClassLoader classLoader : ClasspathHelper.classLoaders(classLoaders)){
				if(type.contains("[")){
					try{
						return Class.forName(type, false, classLoader);
					}
					catch(final Throwable e){
						reflectionsExceptions.add(new ReflectionsException("could not get type for name " + typeName, e));
					}
				}
				try{
					return classLoader.loadClass(type);
				}
				catch(final Throwable e){
					reflectionsExceptions.add(new ReflectionsException("could not get type for name " + typeName, e));
				}
			}

			if(Reflections.LOGGER != null)
				for(final ReflectionsException reflectionsException : reflectionsExceptions)
					Reflections.LOGGER.warn("could not get type for name " + typeName + " from any class loader", reflectionsException);

			return null;
		}
	}

	/**
	 * Try to resolve all given string representation of types to a list of java types
	 *
	 * @param classes	The classes.
	 * @param classLoaders	The class loaders.
	 * @return	The classes.
	 * @param <T>	The type of the returned classes.
	 */
	static <T> Set<Class<? extends T>> forNames(final Collection<String> classes, final ClassLoader... classLoaders){
		return classes.stream()
			.map(className -> (Class<? extends T>)forName(className, classLoaders))
			.filter(Objects::nonNull)
			.collect(Collectors.toCollection(HashSet::new));
	}


	private static List<String> primitiveNames;
	private static List<Class<?>> primitiveTypes;
	private static List<String> primitiveDescriptors;

	private static void initPrimitives(){
		if(primitiveNames == null){
			primitiveNames = Arrays.asList("boolean", "char", "byte", "short", "int", "long", "float", "double", "void");
			primitiveTypes = Arrays.asList(boolean.class, char.class, byte.class, short.class, int.class, long.class, float.class, double.class, void.class);
			primitiveDescriptors = Arrays.asList("Z", "C", "B", "S", "I", "J", "F", "D", "V");
		}
	}

	private static List<String> getPrimitiveNames(){
		initPrimitives();
		return primitiveNames;
	}

	private static List<Class<?>> getPrimitiveTypes(){
		initPrimitives();
		return primitiveTypes;
	}

	private static List<String> getPrimitiveDescriptors(){
		initPrimitives();
		return primitiveDescriptors;
	}

	private static boolean areAnnotationMembersMatching(final Annotation annotation1, final Annotation annotation2){
		if(annotation2 != null && annotation1.annotationType() == annotation2.annotationType()){
			for(final Method method : annotation1.annotationType().getDeclaredMethods()){
				try{
					if(!method.invoke(annotation1).equals(method.invoke(annotation2)))
						return false;
				}
				catch(final Exception e){
					throw new ReflectionsException(String.format("could not invoke method %s on annotation %s", method.getName(), annotation1.annotationType()), e);
				}
			}
			return true;
		}
		return false;
	}

}
