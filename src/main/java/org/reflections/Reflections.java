package org.reflections;

import io.github.mtrevisan.boxon.internal.JavaHelper;
import org.reflections.scanners.Scanner;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.scanners.TypeAnnotationsScanner;
import org.reflections.util.Utils;
import org.reflections.vfs.Vfs;
import org.slf4j.Logger;

import java.lang.annotation.Annotation;
import java.lang.annotation.Inherited;
import java.net.URL;
import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import static java.lang.String.format;


public class Reflections{

	public static final Logger LOGGER = JavaHelper.getLoggerFor(Reflections.class);

	private final Scanner[] scanners = new Scanner[]{new TypeAnnotationsScanner(), new SubTypesScanner()};
	protected final ClassStore classStore;


	/**
	 * constructs a Reflections instance and scan according to given {@link Configuration}
	 * <p>it is preferred to use {@link ConfigurationBuilder}
	 *
	 * @param configuration	The configuration.
	 */
	public Reflections(final Configuration configuration){
		Objects.requireNonNull(configuration);
		if(configuration.getUrls() == null || configuration.getUrls().isEmpty())
			throw new IllegalArgumentException("Given scan URLs are empty");

		classStore = new ClassStore();

		//inject to scanners
		for(final Scanner scanner : scanners)
			scanner.setMetadataAdapter(configuration.getMetadataAdapter());

		final Set<URL> urls = configuration.getUrls();
		scan(urls);

		if(configuration.shouldExpandSuperTypes())
			expandSuperTypes();
	}

	protected void scan(final Set<URL> urls){
		if(LOGGER != null)
			LOGGER.debug("going to scan these urls: {}", urls);

		final long time = System.currentTimeMillis();
		int scannedUrls = 0;

		for(final URL url : urls){
			try{
				scan(url);
				scannedUrls ++;
			}
			catch(final ReflectionsException e){
				if(LOGGER != null)
					LOGGER.warn("could not create Vfs.Dir from url. ignoring the exception and continuing", e);
			}
		}

		if(LOGGER != null)
			LOGGER.info(format("Reflections took %d ms to scan %d urls", System.currentTimeMillis() - time, scannedUrls));
	}

	protected void scan(final URL url){
		final Vfs.Dir dir = Vfs.fromURL(url);

		for(final Vfs.File file : dir.getFiles()){
			// scan if inputs filter accepts file relative path or fqn
			final String path = file.getRelativePath();
			final String fqn = path.replace('/', '.');
			Object classObject = null;
			for(final Scanner scanner : scanners){
				try{
					if(scanner.acceptsInput(path) || scanner.acceptsInput(fqn))
						classObject = scanner.scan(file, classObject, classStore);
				}
				catch(final Exception e){
					if(LOGGER != null)
						// SLF4J will filter out Throwables from the format string arguments.
						LOGGER.debug("could not scan file {} in url {} with scanner {}", file.getRelativePath(), url.toExternalForm(), scanner.getClass().getSimpleName(), e);
				}
			}
		}
	}

	/**
	 * expand super types after scanning, for super types that were not scanned.
	 * this is helpful in finding the transitive closure without scanning all 3rd party dependencies.
	 * it uses {@link ReflectionUtils#getSuperTypes(Class)}.
	 * <p>
	 * for example, for classes A,B,C where A supertype of B, B supertype of C:
	 * <ul>
	 *     <li>if scanning C resulted in B (B-&gt;C in store), but A was not scanned (although A supertype of B) - then getSubTypes(A) will not return C</li>
	 *     <li>if expanding supertypes, B will be expanded with A (A-&gt;B in store) - then getSubTypes(A) will return C</li>
	 * </ul>
	 */
	public void expandSuperTypes(){
		final Set<String> keys = classStore.keys(SubTypesScanner.class);
		keys.removeAll(classStore.values(SubTypesScanner.class));
		for(final String key : keys){
			final Class<?> type = ReflectionUtils.forName(key);
			if(type != null)
				expandSupertypes(classStore, key, type);
		}
	}

	private void expandSupertypes(final ClassStore classStore, final String key, final Class<?> type){
		for(final Class<?> supertype : ReflectionUtils.getSuperTypes(type))
			if(classStore.put(SubTypesScanner.class, supertype.getName(), key)){
				if(LOGGER != null)
					LOGGER.debug("expanded subtype {} -> {}", supertype.getName(), key);

				expandSupertypes(classStore, supertype.getName(), supertype);
			}
	}

	//query

	/**
	 * gets all sub types in hierarchy of a given type
	 * <p>depends on SubTypesScanner configured.</p>
	 *
	 * @param type	The type.
	 * @return	The set of classes.
	 * @param <T>	The type of {@code type}.
	 */
	public <T> Set<Class<? extends T>> getSubTypesOf(final Class<T> type){
		return ReflectionUtils.forNames(classStore.getAll(SubTypesScanner.class, type.getName()));
	}

	/**
	 * get types annotated with a given annotation, both classes and annotations
	 * <p>{@link Inherited} is not honored by default.
	 * <p>when honoring @Inherited, meta-annotation should only effect annotated super classes and its sub types
	 * <p><i>Note that this (@Inherited) meta-annotation type has no effect if the annotated type is used for anything other then a class.
	 * Also, this meta-annotation causes annotations to be inherited only from superclasses; annotations on implemented interfaces have no effect.</i>
	 * <p>depends on TypeAnnotationsScanner and SubTypesScanner configured.</p>
	 *
	 * @param annotation	The annotation.
	 * @return	The set of classes.
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
	 * <p>depends on TypeAnnotationsScanner and SubTypesScanner configured.</p>
	 *
	 * @param annotation	The annotation.
	 * @param honorInherited	Whether to honor inherited.
	 * @return	The set of classes.
	 */
	public Set<Class<?>> getTypesAnnotatedWith(final Class<? extends Annotation> annotation, final boolean honorInherited){
		final Set<String> annotated = classStore.get(TypeAnnotationsScanner.class, annotation.getName());
		annotated.addAll(getAllAnnotated(annotated, annotation, honorInherited));
		return ReflectionUtils.forNames(annotated);
	}

	/**
	 * get types annotated with a given annotation, both classes and annotations, including annotation member values matching
	 * <p>{@link Inherited} is not honored by default.</p>
	 * <p>depends on TypeAnnotationsScanner configured.</p>
	 *
	 * @param annotation	The annotation.
	 * @return	The set of classes.
	 */
	public Set<Class<?>> getTypesAnnotatedWith(final Annotation annotation){
		return getTypesAnnotatedWith(annotation, false);
	}

	/**
	 * get types annotated with a given annotation, both classes and annotations, including annotation member values matching
	 * <p>{@link Inherited} is honored according to given honorInherited.</p>
	 * <p>depends on TypeAnnotationsScanner configured.</p>
	 *
	 * @param annotation	The annotation.
	 * @param honorInherited	Whether to honor inherited.
	 * @return	The set of classes.
	 */
	public Set<Class<?>> getTypesAnnotatedWith(final Annotation annotation, final boolean honorInherited){
		final Set<String> annotated = classStore.get(TypeAnnotationsScanner.class, annotation.annotationType().getName());
		final Set<Class<?>> allAnnotated = Utils.filter(ReflectionUtils.forNames(annotated), ReflectionUtils.withAnnotation(annotation));
		final Set<Class<?>> classes = ReflectionUtils.forNames(Utils.filter(getAllAnnotated(Utils.names(allAnnotated), annotation.annotationType(), honorInherited), s -> !annotated.contains(s)));
		allAnnotated.addAll(classes);
		return allAnnotated;
	}

	protected Collection<String> getAllAnnotated(final Collection<String> annotated, final Class<? extends Annotation> annotation, final boolean honorInherited){
		if(honorInherited){
			if(annotation.isAnnotationPresent(Inherited.class)){
				final Set<String> subTypes = classStore.get(SubTypesScanner.class, Utils.filter(annotated, input -> {
					final Class<?> type = ReflectionUtils.forName(input);
					return type != null && !type.isInterface();
				}));
				return classStore.getAllIncludingKeys(SubTypesScanner.class, subTypes);
			}
			else
				return annotated;
		}
		else{
			final Collection<String> subTypes = classStore.getAllIncludingKeys(TypeAnnotationsScanner.class, annotated);
			return classStore.getAllIncludingKeys(SubTypesScanner.class, subTypes);
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
		final Set<String> allTypes = new HashSet<>(classStore.getAll(SubTypesScanner.class, Object.class.getName()));
		if(allTypes.isEmpty())
			throw new ReflectionsException("Couldn't find subtypes of Object. " + "Make sure SubTypesScanner initialized to include Object class - new SubTypesScanner(false)");

		return allTypes;
	}

	/**
	 * returns the {@link ClassStore} used for storing and querying the metadata
	 *
	 * @return	The store.
	 */
	public ClassStore getStore(){
		return classStore;
	}

}
