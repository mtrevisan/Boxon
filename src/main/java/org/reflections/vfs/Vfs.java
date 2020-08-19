package org.reflections.vfs;

import org.reflections.Reflections;
import org.reflections.ReflectionsException;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.Utils;

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
 * a simple virtual file system bridge
 * <p>use the {@link Vfs#fromURL(URL)} to get a {@link Dir},
 * then use {@link Dir#getFiles()} to iterate over the {@link File}
 * <p>for example:
 * <pre>
 *      Vfs.Dir dir = Vfs.fromURL(url);
 *      Iterable&lt;Vfs.File&gt; files = dir.getFiles();
 *      for (Vfs.File file : files) {
 *          InputStream is = file.openInputStream();
 *      }
 * </pre>
 * <p>{@link Vfs#fromURL(URL)} uses static {@link DefaultUrlTypes} to resolve URLs.
 * It contains VfsTypes for handling for common resources such as local jar file, local directory, jar url, jar input stream and more.
 * <p>It can be plugged in with other {@link UrlType} using {@link Vfs#addDefaultURLTypes(UrlType)} or {@link Vfs#setDefaultURLTypes(List)}.
 * <p>for example:
 * <pre>
 *      Vfs.addDefaultURLTypes(new Vfs.UrlType() {
 *          public boolean matches(URL url)         {
 *              return url.getProtocol().equals("http");
 *          }
 *          public Vfs.Dir createDir(final URL url) {
 *              return new HttpDir(url); //implement this type... (check out a naive implementation on VfsTest)
 *          }
 *      });
 *
 *      Vfs.Dir dir = Vfs.fromURL(new URL("http://mirrors.ibiblio.org/pub/mirrors/maven2/org/slf4j/slf4j-api/1.5.6/slf4j-api-1.5.6.jar"));
 * </pre>
 * <p>use {@link Vfs#findFiles(Collection, Predicate)} to get an
 * iteration of files matching given name predicate over given list of urls
 */
public abstract class Vfs{
	private static List<UrlType> defaultUrlTypes = new ArrayList<>(Arrays.asList(DefaultUrlTypes.values()));

	/**
	 * an abstract vfs dir
	 */
	public interface Dir{
		String getPath();

		Iterable<File> getFiles();
	}

	/**
	 * an abstract vfs file
	 */
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

		Dir createDir(URL url) throws Exception;
	}

	/**
	 * The default url types that will be used when issuing {@link Vfs#fromURL(URL)}
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
	public static Dir fromURL(final URL url){
		return fromURL(url, defaultUrlTypes);
	}

	/**
	 * Tries to create a Dir from the given {@code url}, using the given {@code urlTypes}.
	 *
	 * @param url	The URL.
	 * @param urlTypes	The URL types.
	 * @return	The Dir from the given {@code url}, using the given {@code urlTypes}.
	 */
	public static Dir fromURL(final URL url, final List<UrlType> urlTypes){
		for(final UrlType type : urlTypes){
			try{
				if(type.matches(url)){
					final Dir dir = type.createDir(url);
					if(dir != null)
						return dir;
				}
			}catch(final Throwable e){
				if(Reflections.LOGGER != null){
					Reflections.LOGGER.warn("could not create Dir using " + type + " from url " + url.toExternalForm() + ". skipping.", e);
				}
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
	public static Dir fromURL(final URL url, final UrlType... urlTypes){
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
			}catch(final Throwable e){
				if(Reflections.LOGGER != null){
					Reflections.LOGGER.error("could not findFiles for url. continuing. [" + url + "]", e);
				}
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
	public static java.io.File getFile(final URL url){
		java.io.File file;
		String path;

		try{
			path = url.toURI().getSchemeSpecificPart();
			if((file = new java.io.File(path)).exists())
				return file;
		}catch(final URISyntaxException ignored){
		}

		path = URLDecoder.decode(url.getPath(), StandardCharsets.UTF_8);
		if(path.contains(".jar!"))
			path = path.substring(0, path.lastIndexOf(".jar!") + ".jar".length());
		if((file = new java.io.File(path)).exists())
			return file;

		try{
			path = url.toExternalForm();
			if(path.startsWith("jar:"))
				path = path.substring("jar:".length());
			if(path.startsWith("wsjar:"))
				path = path.substring("wsjar:".length());
			if(path.startsWith("file:"))
				path = path.substring("file:".length());
			if(path.contains(".jar!"))
				path = path.substring(0, path.indexOf(".jar!") + ".jar".length());
			if(path.contains(".war!"))
				path = path.substring(0, path.indexOf(".war!") + ".war".length());
			if((file = new java.io.File(path)).exists())
				return file;

			path = path.replace("%20", " ");
			if((file = new java.io.File(path)).exists())
				return file;

		}catch(final Exception ignored){
		}

		return null;
	}

	private static boolean hasJarFileInPath(final URL url){
		return url.toExternalForm().matches(".*\\.jar(!.*|$)");
	}

	/**
	 * default url types used by {@link Vfs#fromURL(URL)}
	 * <p>jarFile - creates a {@link ZipDir} over jar file
	 * <p>jarUrl - creates a {@link ZipDir} over a jar url (contains ".jar!/" in it's name), using Java's {@link JarURLConnection}
	 * <p>directory - creates a {@link SystemDir} over a file system directory
	 * <p>jboss vfs - for protocols vfs, using jboss vfs (should be provided in classpath)
	 * <p>jboss vfsfile - creates a {@link UrlTypeVFS} for protocols vfszip and vfsfile.
	 * <p>bundle - for bundle protocol, using eclipse FileLocator (should be provided in classpath)
	 * <p>jarInputStream - creates a {@link JarInputDir} over jar files, using Java's JarInputStream
	 */
	public enum DefaultUrlTypes implements UrlType{
		jarFile{
			public boolean matches(final URL url){
				return url.getProtocol().equals("file") && hasJarFileInPath(url);
			}

			public Dir createDir(final URL url) throws Exception{
				return new ZipDir(new JarFile(getFile(url)));
			}
		},

		jarUrl{
			public boolean matches(final URL url){
				return "jar".equals(url.getProtocol()) || "zip".equals(url.getProtocol()) || "wsjar".equals(url.getProtocol());
			}

			public Dir createDir(final URL url) throws Exception{
				try{
					final URLConnection urlConnection = url.openConnection();
					if(urlConnection instanceof JarURLConnection){
						urlConnection.setUseCaches(false);
						return new ZipDir(((JarURLConnection) urlConnection).getJarFile());
					}
				}catch(final Throwable e){ /*fallback*/ }
				final java.io.File file = getFile(url);
				if(file != null){
					return new ZipDir(new JarFile(file));
				}
				return null;
			}
		},

		directory{
			public boolean matches(final URL url){
				if(url.getProtocol().equals("file") && !hasJarFileInPath(url)){
					final java.io.File file = getFile(url);
					return file != null && file.isDirectory();
				}
				else
					return false;
			}

			public Dir createDir(final URL url){
				return new SystemDir(getFile(url));
			}
		},

		jboss_vfs{
			public boolean matches(final URL url){
				return url.getProtocol().equals("vfs");
			}

			public Dir createDir(final URL url) throws Exception{
				final Object content = url.openConnection().getContent();
				final Class<?> virtualFile = ClasspathHelper.contextClassLoader().loadClass("org.jboss.vfs.VirtualFile");
				final java.io.File physicalFile = (java.io.File) virtualFile.getMethod("getPhysicalFile").invoke(content);
				final String name = (String) virtualFile.getMethod("getName").invoke(content);
				java.io.File file = new java.io.File(physicalFile.getParentFile(), name);
				if(!file.exists() || !file.canRead())
					file = physicalFile;
				return file.isDirectory()? new SystemDir(file): new ZipDir(new JarFile(file));
			}
		},

		jboss_vfsfile{
			public boolean matches(final URL url){
				return "vfszip".equals(url.getProtocol()) || "vfsfile".equals(url.getProtocol());
			}

			public Dir createDir(final URL url){
				return new UrlTypeVFS().createDir(url);
			}
		},

		bundle{
			public boolean matches(final URL url){
				return url.getProtocol().startsWith("bundle");
			}

			public Dir createDir(final URL url) throws Exception{
				return fromURL((URL)ClasspathHelper.contextClassLoader().
					loadClass("org.eclipse.core.runtime.FileLocator").getMethod("resolve", URL.class).invoke(null, url));
			}
		},

		jarInputStream{
			public boolean matches(final URL url){
				return url.toExternalForm().contains(".jar");
			}

			public Dir createDir(final URL url){
				return new JarInputDir(url);
			}
		}
	}
}
