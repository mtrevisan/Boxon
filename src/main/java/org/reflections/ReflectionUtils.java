package org.reflections;

import org.reflections.util.ClasspathHelper;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;


/**
 * convenient java reflection helper methods
 * <p>
 * 1. some helper methods to get type by name: {@link #forName(String, ClassLoader...)} and {@link #forNames(Collection, ClassLoader...)} )}
 * <p>
 * 2. some helper methods to get all types/methods/fields/constructors/properties matching some predicates, generally:
 * > Set&#60?> result = getAllXXX(type/s, withYYY) </pre>
 * here get methods are:
 * <ul>
 *     <li>{@ link #getAllSuperTypes(Class, Predicate...)}
 *     <li>{@ link #getAllFields(Class, Predicate...)}
 *     <li>{@ link #getAllMethods(Class, Predicate...)}
 *     <li>{@ link #getAllConstructors(Class, Predicate...)}
 * </ul>
 * <p>and predicates included here all starts with "with", such as
 * <ul>
 *     <li>{@link #withAnnotation(Annotation)}
 * </ul>
 *
 * <p><br>
 *  for example, getting all getters would be:
 * <pre>
 *  Set&#60Method> getters = getAllMethods(someClasses,
 *          Predicates.and(
 *                  withModifier(Modifier.PUBLIC),
 *                  withPrefix("get"),
 *                  withParametersCount(0)));
 * </pre>
 */
@SuppressWarnings("unchecked")
public abstract class ReflectionUtils{

	/**
	 * would include {@code Object.class} when {@ link #getAllSuperTypes(Class, Predicate[])}. default is false.
	 */
	public static boolean includeObject = false;


	/**
	 * get the immediate supertype and interfaces of the given {@code type}
	 */
	public static Set<Class<?>> getSuperTypes(Class<?> type){
		Set<Class<?>> result = new LinkedHashSet<>();
		Class<?> superclass = type.getSuperclass();
		Class<?>[] interfaces = type.getInterfaces();
		if(superclass != null && (includeObject || !superclass.equals(Object.class)))
			result.add(superclass);
		if(interfaces != null && interfaces.length > 0)
			result.addAll(Arrays.asList(interfaces));
		return result;
	}

	//predicates

	/**
	 * where element is annotated with given {@code annotation}
	 */
	public static <T extends AnnotatedElement> Predicate<T> withAnnotation(final Class<? extends Annotation> annotation){
		return input -> input != null && input.isAnnotationPresent(annotation);
	}

	/**
	 * where element is annotated with given {@code annotation}, including member matching
	 */
	public static <T extends AnnotatedElement> Predicate<T> withAnnotation(final Annotation annotation){
		return input -> input != null && input.isAnnotationPresent(annotation.annotationType()) && areAnnotationMembersMatching(input.getAnnotation(annotation.annotationType()), annotation);
	}

	/**
	 * tries to resolve a java type name to a Class
	 * <p>if optional {@link ClassLoader}s are not specified, then both {@link ClasspathHelper#contextClassLoader()} and {@link ClasspathHelper#staticClassLoader()} are used
	 */
	public static Class<?> forName(String typeName, ClassLoader... classLoaders){
		if(getPrimitiveNames().contains(typeName)){
			return getPrimitiveTypes().get(getPrimitiveNames().indexOf(typeName));
		}
		else{
			String type;
			if(typeName.contains("[")){
				int i = typeName.indexOf("[");
				type = typeName.substring(0, i);
				String array = typeName.substring(i).replace("]", "");

				if(getPrimitiveNames().contains(type)){
					type = getPrimitiveDescriptors().get(getPrimitiveNames().indexOf(type));
				}
				else{
					type = "L" + type + ";";
				}

				type = array + type;
			}
			else{
				type = typeName;
			}

			List<ReflectionsException> reflectionsExceptions = new ArrayList<>();
			for(ClassLoader classLoader : ClasspathHelper.classLoaders(classLoaders)){
				if(type.contains("[")){
					try{
						return Class.forName(type, false, classLoader);
					}catch(Throwable e){
						reflectionsExceptions.add(new ReflectionsException("could not get type for name " + typeName, e));
					}
				}
				try{
					return classLoader.loadClass(type);
				}catch(Throwable e){
					reflectionsExceptions.add(new ReflectionsException("could not get type for name " + typeName, e));
				}
			}

			if(Reflections.log != null){
				for(ReflectionsException reflectionsException : reflectionsExceptions){
					Reflections.log.warn("could not get type for name " + typeName + " from any class loader", reflectionsException);
				}
			}

			return null;
		}
	}

	/**
	 * try to resolve all given string representation of types to a list of java types
	 */
	public static <T> Set<Class<? extends T>> forNames(final Collection<String> classes, ClassLoader... classLoaders){
		return classes.stream().map(className -> (Class<? extends T>) forName(className, classLoaders)).filter(Objects::nonNull).collect(Collectors.toCollection(LinkedHashSet::new));
	}


	private static List<String> primitiveNames;
	private static List<Class> primitiveTypes;
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

	private static List<Class> getPrimitiveTypes(){
		initPrimitives();
		return primitiveTypes;
	}

	private static List<String> getPrimitiveDescriptors(){
		initPrimitives();
		return primitiveDescriptors;
	}

	private static boolean areAnnotationMembersMatching(Annotation annotation1, Annotation annotation2){
		if(annotation2 != null && annotation1.annotationType() == annotation2.annotationType()){
			for(Method method : annotation1.annotationType().getDeclaredMethods()){
				try{
					if(!method.invoke(annotation1).equals(method.invoke(annotation2)))
						return false;
				}catch(Exception e){
					throw new ReflectionsException(String.format("could not invoke method %s on annotation %s", method.getName(), annotation1.annotationType()), e);
				}
			}
			return true;
		}
		return false;
	}

}
