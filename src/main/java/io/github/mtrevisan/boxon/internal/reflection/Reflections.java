/**
 * Copyright (c) 2020 Mauro Trevisan
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
package io.github.mtrevisan.boxon.internal.reflection;

import io.github.mtrevisan.boxon.internal.JavaHelper;
import io.github.mtrevisan.boxon.internal.reflection.adapters.MetadataAdapter;
import io.github.mtrevisan.boxon.internal.reflection.scanners.Scanner;
import io.github.mtrevisan.boxon.internal.reflection.scanners.SubTypesScanner;
import io.github.mtrevisan.boxon.internal.reflection.scanners.TypeAnnotationsScanner;
import io.github.mtrevisan.boxon.internal.reflection.util.Utils;
import io.github.mtrevisan.boxon.internal.reflection.vfs.Vfs;
import org.slf4j.Logger;

import java.lang.annotation.Annotation;
import java.lang.annotation.Inherited;
import java.net.URL;
import java.util.Collection;
import java.util.Objects;
import java.util.Set;


public class Reflections{

	public static final Logger LOGGER = JavaHelper.getLoggerFor(Reflections.class);

	private final Scanner[] scanners = new Scanner[]{new TypeAnnotationsScanner(), new SubTypesScanner()};
	protected final ClassStore classStore = new ClassStore();


	/**
	 * constructs a Reflections instance and scan according to given {@link Configuration}
	 * <p>it is preferred to use {@link ConfigurationBuilder}
	 *
	 * @param configuration	The configuration.
	 */
	public Reflections(final Configuration configuration){
		Objects.requireNonNull(configuration);
		final MetadataAdapter<?> metadataAdapter = configuration.getMetadataAdapter();
		Objects.requireNonNull(metadataAdapter);
		final Set<URL> urls = configuration.getUrls();
		if(urls == null || urls.isEmpty())
			throw new IllegalArgumentException("Given scan URLs are empty");

		//inject to scanners
		for(int i = 0; i < scanners.length; i ++)
			scanners[i].setMetadataAdapter(metadataAdapter);

		scan(urls);

		if(configuration.shouldExpandSuperTypes())
			expandSuperTypes();
	}

	private void scan(final Set<URL> urls){
		for(final URL url : urls){
			try{
				scan(url);
			}
			catch(final ReflectionsException e){
				if(LOGGER != null)
					LOGGER.warn("Could not create Vfs.Dir from URL, ignoring the exception and continuing", e);
			}
		}
	}

	private void scan(final URL url){
		final Vfs.Directory directory = Vfs.fromURL(url);
		for(final Vfs.File file : directory.getFiles()){
			//scan if inputs filter accepts file relativePath or packageName
			final String relativePath = file.getRelativePath();
			final String packageName = relativePath.replace('/', '.');
			Object classObject = null;
			for(final Scanner scanner : scanners){
				try{
					if(scanner.acceptsInput(relativePath) || scanner.acceptsInput(packageName))
						classObject = scanner.scan(file, classObject, classStore);
				}
				catch(final Exception e){
					if(LOGGER != null)
						LOGGER.debug("Could not scan file {} in URL {} with scanner {}", relativePath, url.toExternalForm(), scanner.getClass().getSimpleName(), e);
				}
			}
		}
	}

	/**
	 * Expand super types after scanning (for super types that were not scanned).
	 * <p>This is helpful in finding the transitive closure without scanning all third party dependencies.</p>
	 * <p>It uses {@link ReflectionUtils#getSuperTypes(Class)}.</p>
	 * <p>
	 * for example, for classes {@code A, B, C} where {@code A} supertype of {@code B}, and {@code B} supertype of {@code C}:
	 * <ul>
	 *     <li>if scanning {@code C} resulted in {@code B} ({@code B -> C} in class store), but {@code A} was not scanned (although {@code A} supertype of {@code B}) - then {@code getSubTypesOf(A)} will not return {@code C}.</li>
	 *     <li>if expanding supertypes, {@code B} will be expanded with {@code A} ({@code A -> B} in class store) - then {@code getSubTypesOf(A)} will return {@code C}.</li>
	 * </ul>
	 */
	private void expandSuperTypes(){
		final Set<String> keys = classStore.keys(SubTypesScanner.class);
		keys.removeAll(classStore.values(SubTypesScanner.class));
		for(final String key : keys){
			final Class<?> type = ReflectionUtils.forName(key);
			if(type != null)
				expandSupertypes(classStore, key, type);
		}
	}

	private void expandSupertypes(final ClassStore classStore, final String key, final Class<?> type){
		for(final Class<?> superType : ReflectionUtils.getSuperTypes(type))
			if(classStore.put(SubTypesScanner.class, superType.getName(), key)){
				if(LOGGER != null)
					LOGGER.trace("Expanded subtype {} into {}", superType.getName(), key);

				expandSupertypes(classStore, superType.getName(), superType);
			}
	}

	/**
	 * Gets all sub types in hierarchy of a given type.
	 *
	 * @param type	The type to search for.
	 * @return	The set of classes.
	 * @param <T>	The type of {@code type}.
	 */
	public <T> Set<Class<? extends T>> getSubTypesOf(final Class<T> type){
		return ReflectionUtils.forNames(classStore.getAll(SubTypesScanner.class, type.getName()));
	}

	/**
	 * Get types annotated with a given annotation, both classes and annotations.
	 * <p>{@link Inherited} is not honored by default.</p>
	 * <p>When honoring {@link Inherited}, meta-annotation should only effect annotated super classes and its sub types.</p>
	 * <p><i>Note that this ({@link Inherited}) meta-annotation type has no effect if the annotated type is used for anything other then a class.
	 * Also, this meta-annotation causes annotations to be inherited only from superclasses; annotations on implemented interfaces have no effect.</i></p>
	 *
	 * @param annotation	The annotation to search for.
	 * @return	The set of classes.
	 */
	public Set<Class<?>> getTypesAnnotatedWith(final Class<? extends Annotation> annotation){
		return getTypesAnnotatedWith(annotation, false);
	}

	/**
	 * Get types annotated with a given annotation, both classes and annotations.
	 * <p>{@link Inherited} is not honored by default.</p>
	 * <p>When honoring {@link Inherited}, meta-annotation should only effect annotated super classes and its sub types.</p>
	 * <p>When <b>not</b> honoring {@link Inherited}, meta annotation effects all subtypes, including annotations interfaces and classes.</p>
	 * <p><i>Note that this ({@link Inherited}) meta-annotation type has no effect if the annotated type is used for anything other then a class.
	 * Also, this meta-annotation causes annotations to be inherited only from superclasses; annotations on implemented interfaces have no effect.</i></p>
	 *
	 * @param annotation	The annotation to search for.
	 * @return	The set of classes.
	 */
	public Set<Class<?>> getTypesAnnotatedWithHonorInherited(final Class<? extends Annotation> annotation){
		return getTypesAnnotatedWith(annotation, true);
	}

	private Set<Class<?>> getTypesAnnotatedWith(final Class<? extends Annotation> annotation, final boolean honorInherited){
		final Set<String> annotated = classStore.get(TypeAnnotationsScanner.class, annotation.getName());
		annotated.addAll(getAllAnnotated(annotated, annotation, honorInherited));
		return ReflectionUtils.forNames(annotated);
	}

	/**
	 * Get types annotated with a given annotation, both classes and annotations, including annotation member values matching.
	 * <p>{@link Inherited} is not honored by default.</p>
	 * <p>When honoring {@link Inherited}, meta-annotation should only effect annotated super classes and its sub types.</p>
	 * <p><i>Note that this ({@link Inherited}) meta-annotation type has no effect if the annotated type is used for anything other then a class.
	 * Also, this meta-annotation causes annotations to be inherited only from superclasses; annotations on implemented interfaces have no effect.</i></p>
	 *
	 * @param annotation	The annotation.
	 * @return	The set of classes.
	 */
	public Set<Class<?>> getTypesAnnotatedWith(final Annotation annotation){
		return getTypesAnnotatedWith(annotation, false);
	}

	/**
	 * Get types annotated with a given annotation, both classes and annotations, including annotation member values matching.
	 * <p>{@link Inherited} is not honored by default.</p>
	 * <p>When honoring {@link Inherited}, meta-annotation should only effect annotated super classes and its sub types.</p>
	 * <p>When <b>not</b> honoring {@link Inherited}, meta annotation effects all subtypes, including annotations interfaces and classes.</p>
	 * <p><i>Note that this ({@link Inherited}) meta-annotation type has no effect if the annotated type is used for anything other then a class.
	 * Also, this meta-annotation causes annotations to be inherited only from superclasses; annotations on implemented interfaces have no effect.</i></p>
	 *
	 * @param annotation	The annotation.
	 * @return	The set of classes.
	 */
	public Set<Class<?>> getTypesAnnotatedWithHonorInherited(final Annotation annotation){
		return getTypesAnnotatedWith(annotation, true);
	}

	private Set<Class<?>> getTypesAnnotatedWith(final Annotation annotation, final boolean honorInherited){
		final Set<String> annotated = classStore.get(TypeAnnotationsScanner.class, annotation.annotationType().getName());
		final Set<Class<?>> allAnnotated = Utils.filter(ReflectionUtils.forNames(annotated), ReflectionUtils.withAnnotation(annotation));
		final Set<Class<?>> classes = ReflectionUtils.forNames(Utils.filter(getAllAnnotated(Utils.names(allAnnotated), annotation.annotationType(), honorInherited), s -> !annotated.contains(s)));
		allAnnotated.addAll(classes);
		return allAnnotated;
	}

	private Collection<String> getAllAnnotated(final Collection<String> annotated, final Class<? extends Annotation> annotation, final boolean honorInherited){
		if(honorInherited){
			if(annotation.isAnnotationPresent(Inherited.class)){
				final Set<String> subTypes = classStore.get(SubTypesScanner.class, Utils.filter(annotated, input -> {
					final Class<?> type = ReflectionUtils.forName(input);
					return (type != null && !type.isInterface());
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

}
