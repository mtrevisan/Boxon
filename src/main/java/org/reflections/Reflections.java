package org.reflections;

import org.reflections.scanners.Scanner;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.scanners.TypeAnnotationsScanner;
import org.reflections.util.ConfigurationBuilder;
import org.reflections.vfs.Vfs;
import org.slf4j.Logger;

import java.lang.annotation.Annotation;
import java.lang.annotation.Inherited;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import static java.lang.String.format;
import static org.reflections.ReflectionUtils.forName;
import static org.reflections.ReflectionUtils.forNames;
import static org.reflections.ReflectionUtils.withAnnotation;
import static org.reflections.util.Utils.filter;
import static org.reflections.util.Utils.findLogger;
import static org.reflections.util.Utils.index;
import static org.reflections.util.Utils.names;


/**
 * Reflections one-stop-shop object
 * <p>Reflections scans your classpath, indexes the metadata, allows you to query it on runtime and may save and collect that information for many modules within your project.
 * <p>Using Reflections you can query your metadata such as:
 * <ul>
 *     <li>get all subtypes of some type
 *     <li>get all types/constructors/methods/fields annotated with some annotation, optionally with annotation parameters matching
 *     <li>get all resources matching matching a regular expression
 *     <li>get all methods with specific signature including parameters, parameter annotations and return type
 *     <li>get all methods parameter names
 *     <li>get all fields/methods/constructors usages in code
 * </ul>
 * <p>A typical use of Reflections would be:
 * <pre>
 *      Reflections reflections = new Reflections("my.project.prefix");
 *
 *      Set&#60Class&#60? extends SomeType>> subTypes = reflections.getSubTypesOf(SomeType.class);
 *
 *      Set&#60Class&#60?>> annotated = reflections.getTypesAnnotatedWith(SomeAnnotation.class);
 * </pre>
 * <p>Basically, to use Reflections first instantiate it with one of the constructors, then depending on the scanners, use the convenient query methods:
 * <pre>
 *      Reflections reflections = new Reflections("my.package.prefix");
 *      //or
 *      Reflections reflections = new Reflections(ClasspathHelper.forPackage("my.package.prefix"),
 *            new SubTypesScanner(), new TypesAnnotationScanner(), new FilterBuilder().include(...), ...);
 *
 *       //or using the ConfigurationBuilder
 *       new Reflections(new ConfigurationBuilder()
 *            .filterInputsBy(new FilterBuilder().include(FilterBuilder.prefix("my.project.prefix")))
 *            .setUrls(ClasspathHelper.forPackage("my.project.prefix"))
 *            .setScanners(new SubTypesScanner(), new TypeAnnotationsScanner().filterResultsBy(optionalFilter), ...));
 * </pre>
 * And then query, for example:
 * <pre>
 *       Set&#60Class&#60? extends Module>> modules = reflections.getSubTypesOf(com.google.inject.Module.class);
 *       Set&#60Class&#60?>> singletons =             reflections.getTypesAnnotatedWith(javax.inject.Singleton.class);
 *
 *       Set&#60String> properties =       reflections.getResources(Pattern.compile(".*\\.properties"));
 *       Set&#60Constructor> injectables = reflections.getConstructorsAnnotatedWith(javax.inject.Inject.class);
 *       Set&#60Method> deprecateds =      reflections.getMethodsAnnotatedWith(javax.ws.rs.Path.class);
 *       Set&#60Field> ids =               reflections.getFieldsAnnotatedWith(javax.persistence.Id.class);
 *
 *       Set&#60Method> someMethods =      reflections.getMethodsMatchParams(long.class, int.class);
 *       Set&#60Method> voidMethods =      reflections.getMethodsReturn(void.class);
 *       Set&#60Method> pathParamMethods = reflections.getMethodsWithAnyParamAnnotated(PathParam.class);
 *       Set&#60Method> floatToString =    reflections.getConverters(Float.class, String.class);
 *       List&#60String> parameterNames =  reflections.getMethodsParamNames(Method.class);
 *
 *       Set&#60Member> fieldUsage =       reflections.getFieldUsage(Field.class);
 *       Set&#60Member> methodUsage =      reflections.getMethodUsage(Method.class);
 *       Set&#60Member> constructorUsage = reflections.getConstructorUsage(Constructor.class);
 * </pre>
 * <p>You can use other scanners defined in Reflections as well, such as: SubTypesScanner, TypeAnnotationsScanner (both default),
 * ResourcesScanner, MethodAnnotationsScanner, ConstructorAnnotationsScanner, FieldAnnotationsScanner,
 * MethodParameterScanner, MethodParameterNamesScanner, MemberUsageScanner or any custom scanner.
 * <p>Use {@link #getStore()} to access and query the store directly
 * <p>In order to collect pre saved metadata and avoid re-scanning, use {@ link #collect(String, Predicate, Serializer...)}}
 * <p><i>Make sure to scan all the transitively relevant packages.
 * <br>for instance, given your class C extends B extends A, and both B and A are located in another package than C,
 * when only the package of C is scanned - then querying for sub types of A returns nothing (transitive), but querying for sub types of B returns C (direct).
 * In that case make sure to scan all relevant packages a priori.</i>
 * <p><p><p>For Javadoc, source code, and more information about Reflections Library, see http://github.com/ronmamo/reflections/
 */
public class Reflections{
	public static Logger log = findLogger(Reflections.class);

	protected final transient Configuration configuration;
	private final Set<Scanner> scanners = new HashSet<>(Arrays.asList(new TypeAnnotationsScanner(), new SubTypesScanner()));
	protected Store store;


	/**
	 * constructs a Reflections instance and scan according to given {@link Configuration}
	 * <p>it is preferred to use {@link ConfigurationBuilder}
	 */
	public Reflections(final Configuration configuration){
		this.configuration = configuration;
		store = new Store();

		if(scanners != null && !scanners.isEmpty()){
			//inject to scanners
			for(Scanner scanner : scanners)
				scanner.setConfiguration(configuration);

			scan();

			if(configuration.shouldExpandSuperTypes())
				expandSuperTypes();
		}
	}

	protected Reflections(){
		configuration = new ConfigurationBuilder();
		store = new Store();
	}

	protected void scan(){
		if(configuration.getUrls() == null || configuration.getUrls().isEmpty()){
			if(log != null)
				log.warn("given scan urls are empty. set urls in the configuration");
			return;
		}

		if(log != null && log.isDebugEnabled()){
			log.debug("going to scan these urls: {}", configuration.getUrls());
		}

		long time = System.currentTimeMillis();
		int scannedUrls = 0;

		for(final URL url : configuration.getUrls()){
			try{
				scan(url);
				scannedUrls++;
			}catch(ReflectionsException e){
				if(log != null){
					log.warn("could not create Vfs.Dir from url. ignoring the exception and continuing", e);
				}
			}
		}

		if(log != null){
			log.info(format("Reflections took %d ms to scan %d urls", System.currentTimeMillis() - time, scannedUrls));
		}
	}

	protected void scan(URL url){
		Vfs.Dir dir = Vfs.fromURL(url);

		try{
			for(final Vfs.File file : dir.getFiles()){
				// scan if inputs filter accepts file relative path or fqn
				String path = file.getRelativePath();
				String fqn = path.replace('/', '.');
				Object classObject = null;
				for(Scanner scanner : scanners){
					try{
						if(scanner.acceptsInput(path) || scanner.acceptsInput(fqn)){
							classObject = scanner.scan(file, classObject, store);
						}
					}catch(Exception e){
						if(log != null){
							// SLF4J will filter out Throwables from the format string arguments.
							log.debug("could not scan file {} in url {} with scanner {}", file.getRelativePath(), url.toExternalForm(), scanner.getClass().getSimpleName(), e);
						}
					}
				}
			}
		}finally{
			dir.close();
		}
	}

	/**
	 * expand super types after scanning, for super types that were not scanned.
	 * this is helpful in finding the transitive closure without scanning all 3rd party dependencies.
	 * it uses {@link ReflectionUtils#getSuperTypes(Class)}.
	 * <p>
	 * for example, for classes A,B,C where A supertype of B, B supertype of C:
	 * <ul>
	 *     <li>if scanning C resulted in B (B->C in store), but A was not scanned (although A supertype of B) - then getSubTypes(A) will not return C</li>
	 *     <li>if expanding supertypes, B will be expanded with A (A->B in store) - then getSubTypes(A) will return C</li>
	 * </ul>
	 */
	public void expandSuperTypes(){
		String index = index(SubTypesScanner.class);
		Set<String> keys = store.keys(index);
		keys.removeAll(store.values(index));
		for(String key : keys){
			final Class<?> type = forName(key, loaders());
			if(type != null)
				expandSupertypes(store, key, type);
		}
	}

	private void expandSupertypes(Store store, String key, Class<?> type){
		for(Class<?> supertype : ReflectionUtils.getSuperTypes(type)){
			if(store.put(SubTypesScanner.class, supertype.getName(), key)){
				if(log != null)
					log.debug("expanded subtype {} -> {}", supertype.getName(), key);

				expandSupertypes(store, supertype.getName(), supertype);
			}
		}
	}

	//query

	/**
	 * gets all sub types in hierarchy of a given type
	 * <p/>depends on SubTypesScanner configured
	 */
	public <T> Set<Class<? extends T>> getSubTypesOf(final Class<T> type){
		return forNames(store.getAll(SubTypesScanner.class, type.getName()), loaders());
	}

	/**
	 * get types annotated with a given annotation, both classes and annotations
	 * <p>{@link Inherited} is not honored by default.
	 * <p>when honoring @Inherited, meta-annotation should only effect annotated super classes and its sub types
	 * <p><i>Note that this (@Inherited) meta-annotation type has no effect if the annotated type is used for anything other then a class.
	 * Also, this meta-annotation causes annotations to be inherited only from superclasses; annotations on implemented interfaces have no effect.</i>
	 * <p/>depends on TypeAnnotationsScanner and SubTypesScanner configured
	 */
	public Set<Class<?>> getTypesAnnotatedWith(final Class<? extends Annotation> annotation){
		return getTypesAnnotatedWith(annotation, false);
	}

	/**
	 * get types annotated with a given annotation, both classes and annotations
	 * <p>{@link Inherited} is honored according to given honorInherited.
	 * <p>when honoring @Inherited, meta-annotation should only effect annotated super classes and it's sub types
	 * <p>when not honoring @Inherited, meta annotation effects all subtypes, including annotations interfaces and classes
	 * <p><i>Note that this (@Inherited) meta-annotation type has no effect if the annotated type is used for anything other then a class.
	 * Also, this meta-annotation causes annotations to be inherited only from superclasses; annotations on implemented interfaces have no effect.</i>
	 * <p/>depends on TypeAnnotationsScanner and SubTypesScanner configured
	 */
	public Set<Class<?>> getTypesAnnotatedWith(final Class<? extends Annotation> annotation, boolean honorInherited){
		Set<String> annotated = store.get(TypeAnnotationsScanner.class, annotation.getName());
		annotated.addAll(getAllAnnotated(annotated, annotation, honorInherited));
		return forNames(annotated, loaders());
	}

	/**
	 * get types annotated with a given annotation, both classes and annotations, including annotation member values matching
	 * <p>{@link Inherited} is not honored by default
	 * <p/>depends on TypeAnnotationsScanner configured
	 */
	public Set<Class<?>> getTypesAnnotatedWith(final Annotation annotation){
		return getTypesAnnotatedWith(annotation, false);
	}

	/**
	 * get types annotated with a given annotation, both classes and annotations, including annotation member values matching
	 * <p>{@link Inherited} is honored according to given honorInherited
	 * <p/>depends on TypeAnnotationsScanner configured
	 */
	public Set<Class<?>> getTypesAnnotatedWith(final Annotation annotation, boolean honorInherited){
		Set<String> annotated = store.get(TypeAnnotationsScanner.class, annotation.annotationType().getName());
		Set<Class<?>> allAnnotated = filter(forNames(annotated, loaders()), withAnnotation(annotation));
		Set<Class<?>> classes = forNames(filter(getAllAnnotated(names(allAnnotated), annotation.annotationType(), honorInherited), s -> !annotated.contains(s)), loaders());
		allAnnotated.addAll(classes);
		return allAnnotated;
	}

	protected Collection<String> getAllAnnotated(Collection<String> annotated, Class<? extends Annotation> annotation, boolean honorInherited){
		if(honorInherited){
			if(annotation.isAnnotationPresent(Inherited.class)){
				Set<String> subTypes = store.get(SubTypesScanner.class, filter(annotated, input -> {
					final Class<?> type = forName(input, loaders());
					return type != null && !type.isInterface();
				}));
				return store.getAllIncludingKeys(SubTypesScanner.class, subTypes);
			}
			else{
				return annotated;
			}
		}
		else{
			Collection<String> subTypes = store.getAllIncludingKeys(TypeAnnotationsScanner.class, annotated);
			return store.getAllIncludingKeys(SubTypesScanner.class, subTypes);
		}
	}

	/**
	 * get all types scanned. this is effectively similar to getting all subtypes of Object.
	 * <p>depends on SubTypesScanner configured with {@code SubTypesScanner(false)}, otherwise {@code ReflectionsException} is thrown
	 * <p><i>note using this might be a bad practice. it is better to get types matching some criteria,
	 * such as {@link #getSubTypesOf(Class)} or {@link #getTypesAnnotatedWith(Class)}</i>
	 *
	 * @return Set of String, and not of Class, in order to avoid definition of all types in PermGen
	 */
	public Set<String> getAllTypes(){
		Set<String> allTypes = new HashSet<>(store.getAll(SubTypesScanner.class, Object.class.getName()));
		if(allTypes.isEmpty()){
			throw new ReflectionsException("Couldn't find subtypes of Object. " + "Make sure SubTypesScanner initialized to include Object class - new SubTypesScanner(false)");
		}
		return allTypes;
	}

	/**
	 * returns the {@link Store} used for storing and querying the metadata
	 */
	public Store getStore(){
		return store;
	}

	/**
	 * returns the {@link Configuration} object of this instance
	 */
	public Configuration getConfiguration(){
		return configuration;
	}

	private ClassLoader[] loaders(){ return configuration.getClassLoaders(); }

}
