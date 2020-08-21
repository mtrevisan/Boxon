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
import io.github.mtrevisan.boxon.internal.reflection.adapters.JavaReflectionAdapter;
import io.github.mtrevisan.boxon.internal.reflection.adapters.JavassistAdapter;
import io.github.mtrevisan.boxon.internal.reflection.adapters.MetadataAdapterInterface;
import io.github.mtrevisan.boxon.internal.reflection.scanners.AbstractScanner;
import io.github.mtrevisan.boxon.internal.reflection.scanners.ScannerInterface;
import io.github.mtrevisan.boxon.internal.reflection.scanners.SubTypesScanner;
import io.github.mtrevisan.boxon.internal.reflection.scanners.TypeAnnotationsScanner;
import io.github.mtrevisan.boxon.internal.reflection.vfs.VFSDirectory;
import io.github.mtrevisan.boxon.internal.reflection.vfs.VFSException;
import io.github.mtrevisan.boxon.internal.reflection.vfs.VFSFile;
import io.github.mtrevisan.boxon.internal.reflection.vfs.VirtualFileSystem;
import org.slf4j.Logger;

import java.lang.annotation.Annotation;
import java.lang.annotation.Inherited;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;


/**
 * @see <a href="https://github.com/ronmamo/reflections">Reflections</a>
 */
public final class Reflections{

	private static final Logger LOGGER = JavaHelper.getLoggerFor(Reflections.class);

	private static MetadataAdapterInterface<?> METADATA_ADAPTER;
	static{
		/**
		 * if javassist library exists in the classpath, this method returns {@link JavassistAdapter} otherwise defaults to {@link JavaReflectionAdapter}.
		 * <p>the {@link JavassistAdapter} is preferred in terms of performance and class loading.
		 */
		try{
			METADATA_ADAPTER = new JavassistAdapter();
		}
		catch(final Throwable e){
			if(LOGGER != null)
				LOGGER.warn("could not create JavassistAdapter, using JavaReflectionAdapter", e);

			METADATA_ADAPTER = new JavaReflectionAdapter();
		}
	}

	private final TypeAnnotationsScanner typeAnnotationsScanner = new TypeAnnotationsScanner();
	private final SubTypesScanner subTypesScanner = new SubTypesScanner();


	public static Reflections create(final URL... urls){
		return new Reflections(false, urls);
	}

	public static Reflections create(final Class<?>... classes){
		final URL[] urls = extractDistinctURLs(classes);
		return new Reflections(false, urls);
	}

	public static Reflections createExpandSuperTypes(final Class<?>... classes){
		final URL[] urls = extractDistinctURLs(classes);
		return new Reflections(true, urls);
	}

	/**
	 * Sometimes simple calls have unexpected side effects. I wanted to update some plugins, but the update manager was hanging my UI. Looking at the stack trace reveals:
	 * <pre><code>
	 * at java.net.Inet4AddressImpl.lookupAllHostAddr(Native Method)
	 * at java.net.InetAddress$1.lookupAllHostAddr(Unknown Source)
	 * at java.net.InetAddress.getAddressFromNameService(Unknown Source)
	 * at java.net.InetAddress.getAllByName0(Unknown Source)
	 * at java.net.InetAddress.getAllByName0(Unknown Source)
	 * at java.net.InetAddress.getAllByName(Unknown Source)
	 * at java.net.InetAddress.getByName(Unknown Source)
	 * at java.net.URLStreamHandler.getHostAddress(Unknown Source)
	 * - locked <0x15ce1280> (a sun.net.www.protocol.http.Handler)
	 * at java.net.URLStreamHandler.hashCode(Unknown Source)
	 * at java.net.URL.hashCode(Unknown Source)
	 * - locked <0x1a3100d0> (a java.net.URL)
	 *</code></pre>
	 * <p>Hmm, I must say that it is very dangerous that {@link URL#hashCode()} and {@link URL#equals(Object)} makes an Internet connection. {@link URL} has the worst
	 * equals/hasCode implementation I have ever seen: equality DEPENDS on the state of the Internet.</p>
	 * <p>Do not put {@link URL} into collections unless you can live with the fact that comparing makes calls to the Internet. Use {@link java.net.URI} instead.</p>
	 * <p>URL is an aggressive beast that can slow down and hang your application by making unexpected network traffic.</p>
	 *
	 * @see <a href="http://michaelscharf.blogspot.co.il/2006/11/javaneturlequals-and-hashcode-make.html">java.net.URL.equals and hashCode make (blocking) Internet connections</a>
	 */
	private static URL[] extractDistinctURLs(final Class<?>[] classes){
		final Set<URI> uris = new HashSet<>(classes.length);
		for(int i = 0; i < classes.length; i ++){
			final Collection<URL> urls = ClasspathHelper.forPackage(classes[i].getPackageName());
			for(final URL url : urls){
				try{
					uris.add(url.toURI());
				}
				catch(final URISyntaxException e){
					LOGGER.warn("Invalid URL, cannot convert into URI", e);
				}
			}
		}

		final List<URL> urls = new ArrayList<>(uris.size());
		try{
			for(final URI uri : uris)
				urls.add(uri.toURL());
		}
		catch(final MalformedURLException ignored){
			//cannot happen
		}
		return urls.toArray(URL[]::new);
	}

	public static Reflections createExpandSuperTypes(final URL... urls){
		return new Reflections(true, urls);
	}

	private Reflections(final boolean expandSuperTypes, final URL... urls){
		Objects.requireNonNull(urls);
		if(urls.length == 0)
			throw new IllegalArgumentException("Packages list cannot be empty");

		//inject to scanners
		typeAnnotationsScanner.setMetadataAdapter(METADATA_ADAPTER);
		subTypesScanner.setMetadataAdapter(METADATA_ADAPTER);

		scan(urls);

		if(expandSuperTypes)
			expandSuperTypes();
	}

	private void scan(final URL... urls){
		for(final URL url : urls){
			try{
				scan(url);
			}
			catch(final VFSException | ReflectionsException e){
				if(LOGGER != null)
					LOGGER.warn("Could not create VFSDirectory from URL, ignoring the exception and continuing", e);
			}
		}
	}

	private void scan(final URL url){
		final VFSDirectory directory = VirtualFileSystem.fromURL(url);
		try{
			for(final VFSFile file : directory.getFiles()){
				final String relativePath = file.getRelativePath();
				final String packageName = relativePath.replace('/', '.');
				Object classObject = null;
				for(final ScannerInterface scanner : new ScannerInterface[]{typeAnnotationsScanner, subTypesScanner})
					//scan only if inputs filter accepts file `relativePath` or `packageName`
					if(scanner.acceptsInput(relativePath) || scanner.acceptsInput(packageName)){
						try{
							classObject = scanner.scan(directory, file, classObject);

							if(LOGGER != null)
								LOGGER.trace("Scanned file {} in URL {} with scanner {}", relativePath, url.toExternalForm(), scanner.getClass().getSimpleName());
						}
						catch(final Exception e){
							if(LOGGER != null)
								LOGGER.debug("Could not scan file {} in URL {} with scanner {}", relativePath, url.toExternalForm(), scanner.getClass().getSimpleName(), e);
						}
					}
			}
		}
		finally{
			directory.close();
		}
	}

	/**
	 * Expand super types after scanning (for super types that were not scanned).
	 * <p>This is helpful in finding the transitive closure without scanning all third party dependencies.</p>
	 * <p>It uses {@link ReflectionHelper#getSuperTypes(Class)}.</p>
	 * <p>For example, for classes {@code A, B, C} where {@code A} supertype of {@code B}, and {@code B} supertype of {@code C}:
	 * <ul>
	 *     <li>if scanning {@code C} resulted in {@code B} ({@code B -> C} in class store), but {@code A} was not scanned (although {@code A} supertype of {@code B}) - then {@code getSubTypesOf(A)} will not return {@code C}.</li>
	 *     <li>if expanding supertypes, {@code B} will be expanded with {@code A} ({@code A -> B} in class store) - then {@code getSubTypesOf(A)} will return {@code C}.</li>
	 * </ul>
	 * </p>
	 */
	private void expandSuperTypes(){
		final Set<String> keys = subTypesScanner.keys();
		keys.removeAll(subTypesScanner.values());
		for(final String key : keys){
			final Class<?> type = ReflectionHelper.getClassFromName(key);
			if(type != null)
				expandSupertypes(subTypesScanner, key, type);
		}
	}

	private void expandSupertypes(final AbstractScanner scanner, final String key, final Class<?> type){
		for(final Class<?> superType : ReflectionHelper.getSuperTypes(type))
			if(scanner.put(superType.getName(), key)){
				if(LOGGER != null)
					LOGGER.trace("Expanded subtype {} into {}", superType.getName(), key);

				expandSupertypes(scanner, superType.getName(), superType);
			}
	}

	/**
	 * Gets all sub types in hierarchy of a given type.
	 *
	 * @param type	The type to search for.
	 * @return	The set of classes.
	 */
	public Set<Class<?>> getSubTypesOf(final Class<?> type){
		return ReflectionHelper.getClassesFromNames(subTypesScanner.getAll(type.getName()));
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
		final Set<String> annotated = typeAnnotationsScanner.get(annotation.getName());
		annotated.addAll(getAllAnnotatedClasses(annotated, annotation, honorInherited));
		return ReflectionHelper.getClassesFromNames(annotated);
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
		final Set<String> annotated = typeAnnotationsScanner.get(annotation.annotationType().getName());
		final Set<Class<?>> allAnnotated = JavaHelper.filter(ReflectionHelper.getClassesFromNames(annotated), getFilterWithAnnotation(annotation));
		final Set<Class<?>> classes = ReflectionHelper.getClassesFromNames(JavaHelper.filter(getAllAnnotatedClasses(ReflectionHelper.getClassNames(allAnnotated), annotation.annotationType(), honorInherited), s -> !annotated.contains(s)));
		allAnnotated.addAll(classes);
		return allAnnotated;
	}

	/**
	 * where element is annotated with given {@code annotation}, including member matching
	 *
	 * @param annotation	The annotation.
	 * @return	The predicate.
	 * @param <T>	The type of the returned predicate.
	 */
	private <T extends AnnotatedElement> Predicate<T> getFilterWithAnnotation(final Annotation annotation){
		return input -> (input != null
			&& input.isAnnotationPresent(annotation.annotationType())
			&& areAnnotationMembersMatching(input.getAnnotation(annotation.annotationType()), annotation));
	}

	private boolean areAnnotationMembersMatching(final Annotation annotation1, final Annotation annotation2){
		if(annotation2 != null && annotation1.annotationType() == annotation2.annotationType()){
			for(final Method method : annotation1.annotationType().getDeclaredMethods()){
				try{
					if(!method.invoke(annotation1).equals(method.invoke(annotation2)))
						return false;
				}
				catch(final Exception e){
					throw new ReflectionsException("Could not invoke method " + method.getName() + " on annotation " + annotation1.annotationType(), e);
				}
			}
			return true;
		}
		return false;
	}

	private Collection<String> getAllAnnotatedClasses(final Collection<String> annotated, final Class<? extends Annotation> annotation, final boolean honorInherited){
		if(honorInherited){
			if(annotation.isAnnotationPresent(Inherited.class)){
				final Set<String> subTypes = subTypesScanner.get(JavaHelper.filter(annotated, input -> {
					final Class<?> type = ReflectionHelper.getClassFromName(input);
					return (type != null && !type.isInterface());
				}));
				return subTypesScanner.getAllIncludingKeys(subTypes);
			}
			else
				return annotated;
		}
		else{
			final Collection<String> subTypes = typeAnnotationsScanner.getAllIncludingKeys(annotated);
			return subTypesScanner.getAllIncludingKeys(subTypes);
		}
	}

}
