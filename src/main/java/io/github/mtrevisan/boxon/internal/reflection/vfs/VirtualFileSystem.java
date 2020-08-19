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
package io.github.mtrevisan.boxon.internal.reflection.vfs;

import io.github.mtrevisan.boxon.internal.JavaHelper;
import io.github.mtrevisan.boxon.internal.reflection.ReflectionsException;
import io.github.mtrevisan.boxon.internal.reflection.util.ClasspathHelper;
import io.github.mtrevisan.boxon.internal.reflection.util.Utils;
import org.slf4j.Logger;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.JarURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;
import java.util.jar.JarFile;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;


/**
 * A simple Virtual File System bridge.
 * <p>use the {@link VirtualFileSystem#fromURL(URL)} to get a {@link Directory},
 * then use {@link Directory#getFiles()} to iterate over the {@link File}
 * <p>for example:
 * <pre><code>
 *      VirtualFileSystem.Directory dir = VirtualFileSystem.fromURL(url);
 *      Iterable&lt;VirtualFileSystem.File&gt; files = dir.getFiles();
 *      for(VirtualFileSystem.File file : files)
 *          InputStream is = file.openInputStream();
 * </code></pre>
 * <p>{@link VirtualFileSystem#fromURL(URL)} uses static {@link DefaultUrlTypes} to resolve URLs.
 * It contains VfsTypes for handling for common resources such as local jar file, local directory, jar url, jar input stream and more.
 * <p>It can be plugged in with other {@link UrlType} using {@link VirtualFileSystem#addDefaultURLTypes(UrlType)} or {@link VirtualFileSystem#setDefaultURLTypes(List)}.
 * <p>for example:
 * <pre><code>
 *      VirtualFileSystem.addDefaultURLTypes(new VirtualFileSystem.UrlType(){
 *          public boolean matches(URL url){
 *              return url.getProtocol().equals("http");
 *          }
 *          public VirtualFileSystem.Directory createDir(final URL url){
 *              return new HttpDir(url); //implement this type... (check out a naive implementation on VfsTest)
 *          }
 *      });
 *
 *      VirtualFileSystem.Directory dir = VirtualFileSystem.fromURL(new URL("http://mirrors.ibiblio.org/pub/mirrors/maven2/org/slf4j/slf4j-api/1.5.6/slf4j-api-1.5.6.jar"));
 * </code></pre>
 * <p>use {@link VirtualFileSystem#findFiles(Collection, Predicate)} to get an
 * iteration of files matching given name predicate over given list of urls
 */
public abstract class VirtualFileSystem{

	public static final Logger LOGGER = JavaHelper.getLoggerFor(VirtualFileSystem.class);


	private static List<UrlType> defaultUrlTypes = new ArrayList<>(Arrays.asList(DefaultUrlTypes.values()));


	public interface Directory{
		String getPath();

		Iterable<File> getFiles();
	}

	public interface File{
		String getName();

		String getRelativePath();

		InputStream openInputStream() throws IOException;
	}

	/**
	 * a matcher and factory for a url
	 */
	public interface UrlType{
		boolean matches(URL url);

		Directory createDir(URL url) throws Exception;
	}

	/**
	 * The default url types that will be used when issuing {@link VirtualFileSystem#fromURL(URL)}
	 *
	 * @return	The URL types.
	 */
	public static List<UrlType> getDefaultUrlTypes(){
		return defaultUrlTypes;
	}

	/**
	 * Sets the static default url types. can be used to statically plug in urlTypes
	 *
	 * @param urlTypes	The URL types.
	 */
	public static void setDefaultURLTypes(final List<UrlType> urlTypes){
		defaultUrlTypes = urlTypes;
	}

	/**
	 * Add a static default url types to the beginning of the default url types list. can be used to statically plug in urlTypes
	 *
	 * @param urlType	The URL type.
	 */
	public static void addDefaultURLTypes(final UrlType urlType){
		defaultUrlTypes.add(0, urlType);
	}

	/**
	 * Tries to create a Dir from the given url, using the defaultUrlTypes
	 *
	 * @param url	The URL.
	 * @return	The Dir from the given {@code url}.
	 */
	public static Directory fromURL(final URL url){
		return fromURL(url, defaultUrlTypes);
	}

	/**
	 * Tries to create a Dir from the given {@code url}, using the given {@code urlTypes}.
	 *
	 * @param url	The URL.
	 * @param urlTypes	The URL types.
	 * @return	The Dir from the given {@code url}, using the given {@code urlTypes}.
	 */
	public static Directory fromURL(final URL url, final List<UrlType> urlTypes){
		for(final UrlType type : urlTypes){
			try{
				if(type.matches(url)){
					final Directory directory = type.createDir(url);
					if(directory != null)
						return directory;
				}
			}
			catch(final Throwable e){
				if(LOGGER != null)
					LOGGER.warn("could not create Dir using " + type + " from url " + url.toExternalForm() + ". skipping.", e);
			}
		}

		throw new ReflectionsException("could not create Vfs.Dir from url, no matching UrlType was found [" + url.toExternalForm() + "]\n" + "either use fromURL(final URL url, final List<UrlType> urlTypes) or " + "use the static setDefaultURLTypes(final List<UrlType> urlTypes) or addDefaultURLTypes(UrlType urlType) " + "with your specialized UrlType.");
	}

	/**
	 * Create a Dir from the given {@code url}, using the given {@code urlTypes}.
	 *
	 * @param url	The URL.
	 * @param urlTypes	The URL types.
	 * @return	The Dir from the given {@code url}, using the given {@code urlTypes}.
	 */
	public static Directory fromURL(final URL url, final UrlType... urlTypes){
		return fromURL(url, Arrays.asList(urlTypes));
	}

	/**
	 * Return an iterable of all {@link File} in given urls, starting with given {@code packagePrefix} and matching {@code nameFilter}.
	 *
	 * @param inUrls	URLs.
	 * @param packagePrefix	The prefix.
	 * @param nameFilter	The name filter.
	 * @return	An iterable of all {@link File} in given urls, starting with given {@code packagePrefix} and matching {@code nameFilter}.
	 */
	public static Iterable<File> findFiles(final Collection<URL> inUrls, final String packagePrefix, final Predicate<String> nameFilter){
		final Predicate<File> fileNamePredicate = file -> {
			final String path = file.getRelativePath();
			if(path.startsWith(packagePrefix)){
				final String filename = path.substring(path.indexOf(packagePrefix) + packagePrefix.length());
				return !Utils.isEmpty(filename) && nameFilter.test(filename.substring(1));
			}
			else
				return false;
		};

		return findFiles(inUrls, fileNamePredicate);
	}

	/**
	 * Return an iterable of all {@link File} in given {@code urls}, matching {@code filePredicate}.
	 *
	 * @param urls	The URLs.
	 * @param filePredicate	The file predicate.
	 * @return	An iterable of all {@link File} in given {@code urls}, matching {@code filePredicate}.
	 */
	public static Iterable<File> findFiles(final Collection<URL> urls, final Predicate<File> filePredicate){
		return () -> urls.stream().flatMap(url -> {
			try{
				return StreamSupport.stream(fromURL(url).getFiles().spliterator(), false);
			}
			catch(final Throwable e){
				if(LOGGER != null)
					LOGGER.error("could not findFiles for url. continuing. [" + url + "]", e);

				return Stream.of();
			}
		}).filter(filePredicate).iterator();
	}

	/**
	 * Get {@link java.io.File} from URL.
	 *
	 * @param url	The URL to get the file from.
	 * @return	{@link java.io.File} from URL.
	 */
	public static java.io.File resourceToFile(final URL url){
		try{
			final String path = url.toURI().getSchemeSpecificPart();
			return getFileIfExists(path);
		}
		catch(final URISyntaxException | FileNotFoundException ignored){}

		try{
			final String path = extractDecodedPath(url);
			return getFileIfExists(path);
		}
		catch(final FileNotFoundException ignored){}

		try{
			String path = extractExternalFormPath(url);
			final java.io.File file = new java.io.File(path);
			if(file.exists())
				return file;

			path = path.replace("%20", " ");
			return getFileIfExists(path);
		}
		catch(final Exception ignored){}

		final String path = url.getFile();
		return new java.io.File(path);
	}

	private static String extractDecodedPath(final URL resource){
		String path = URLDecoder.decode(resource.getPath(), StandardCharsets.UTF_8);
		final int idx = path.lastIndexOf(".jar!");
		if(idx >= 0)
			path = path.substring(0, idx + ".jar".length());
		return path;
	}

	private static String extractExternalFormPath(final URL resource){
		String path = resource.toExternalForm();
		if(path.startsWith("jar:"))
			path = path.substring("jar:".length());
		else if(path.startsWith("wsjar:"))
			path = path.substring("wsjar:".length());
		else if(path.startsWith("file:"))
			path = path.substring("file:".length());
		int idx = path.indexOf(".jar!");
		if(idx >= 0)
			path = path.substring(0, idx + ".jar".length());
		idx = path.indexOf(".war!");
		if(idx >= 0)
			path = path.substring(0, idx + ".war".length());
		return path;
	}

	private static java.io.File getFileIfExists(final String path) throws FileNotFoundException{
		final java.io.File file = new java.io.File(path);
		if(!file.exists())
			throw new FileNotFoundException();

		return file;
	}

	private static boolean hasJarFileInPath(final URL url){
		return url.toExternalForm().matches(".*\\.jar(!.*|$)");
	}

	/**
	 * default url types used by {@link VirtualFileSystem#fromURL(URL)}
	 * <p>jarFile - creates a {@link ZipDirectory} over jar file
	 * <p>jarUrl - creates a {@link ZipDirectory} over a jar url (contains ".jar!/" in it's name), using Java's {@link JarURLConnection}
	 * <p>directory - creates a {@link SystemDirectory} over a file system directory
	 * <p>jboss vfs - for protocols vfs, using jboss vfs (should be provided in classpath)
	 * <p>jboss vfsfile - creates a {@link UrlTypeVFS} for protocols vfszip and vfsfile.
	 * <p>bundle - for bundle protocol, using eclipse FileLocator (should be provided in classpath)
	 * <p>jarInputStream - creates a {@link JarInputDirectory} over jar files, using Java's JarInputStream
	 */
	public enum DefaultUrlTypes implements UrlType{
		jarFile{
			public boolean matches(final URL url){
				return url.getProtocol().equals("file") && hasJarFileInPath(url);
			}

			public Directory createDir(final URL url) throws Exception{
				return new ZipDirectory(new JarFile(resourceToFile(url)));
			}
		},

		jarUrl{
			public boolean matches(final URL url){
				return "jar".equals(url.getProtocol()) || "zip".equals(url.getProtocol()) || "wsjar".equals(url.getProtocol());
			}

			public Directory createDir(final URL url) throws Exception{
				try{
					final URLConnection urlConnection = url.openConnection();
					if(urlConnection instanceof JarURLConnection){
						urlConnection.setUseCaches(false);
						return new ZipDirectory(((JarURLConnection) urlConnection).getJarFile());
					}
				}catch(final Throwable e){ /*fallback*/ }
				final java.io.File file = resourceToFile(url);
				if(file != null){
					return new ZipDirectory(new JarFile(file));
				}
				return null;
			}
		},

		directory{
			public boolean matches(final URL url){
				if(url.getProtocol().equals("file") && !hasJarFileInPath(url)){
					final java.io.File file = resourceToFile(url);
					return file != null && file.isDirectory();
				}
				else
					return false;
			}

			public Directory createDir(final URL url){
				return new SystemDirectory(resourceToFile(url));
			}
		},

		jboss_vfs{
			public boolean matches(final URL url){
				return url.getProtocol().equals("vfs");
			}

			public Directory createDir(final URL url) throws Exception{
				final Object content = url.openConnection().getContent();
				final Class<?> virtualFile = ClasspathHelper.contextClassLoader().loadClass("org.jboss.vfs.VirtualFile");
				final java.io.File physicalFile = (java.io.File) virtualFile.getMethod("getPhysicalFile").invoke(content);
				final String name = (String) virtualFile.getMethod("getName").invoke(content);
				java.io.File file = new java.io.File(physicalFile.getParentFile(), name);
				if(!file.exists() || !file.canRead())
					file = physicalFile;
				return file.isDirectory()? new SystemDirectory(file): new ZipDirectory(new JarFile(file));
			}
		},

		jboss_vfsfile{
			public boolean matches(final URL url){
				return "vfszip".equals(url.getProtocol()) || "vfsfile".equals(url.getProtocol());
			}

			public Directory createDir(final URL url){
				return new UrlTypeVFS().createDir(url);
			}
		},

		bundle{
			public boolean matches(final URL url){
				return url.getProtocol().startsWith("bundle");
			}

			public Directory createDir(final URL url) throws Exception{
				return fromURL((URL)ClasspathHelper.contextClassLoader().
					loadClass("org.eclipse.core.runtime.FileLocator").getMethod("resolve", URL.class).invoke(null, url));
			}
		},

		jarInputStream{
			public boolean matches(final URL url){
				return url.toExternalForm().contains(".jar");
			}

			public Directory createDir(final URL url){
				return new JarInputDirectory(url);
			}
		}
	}
}
