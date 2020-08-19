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
import org.slf4j.Logger;

import java.io.FileNotFoundException;
import java.net.JarURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.jar.JarFile;


/**
 * A simple Virtual File System bridge.
 * <p>use the {@link VirtualFileSystem.DefaultUrlTypes#fromURL(URL)} to get a {@link Directory},
 * then use {@link Directory#getFiles()} to iterate over the {@link File}
 * <p>for example:
 * <pre><code>
 *      VirtualFileSystem.Directory dir = VirtualFileSystem.fromURL(url);
 *      Iterable&lt;VirtualFileSystem.File&gt; files = dir.getFiles();
 *      for(VirtualFileSystem.File file : files)
 *          InputStream is = file.openInputStream();
 * </code></pre>
 * <p>{@link VirtualFileSystem.DefaultUrlTypes#fromURL(URL)} uses static {@link DefaultUrlTypes} to resolve URLs.
 * It contains VfsTypes for handling for common resources such as local jar file, local directory, jar url, jar input stream and more.
 * <p>It can be plugged in with other {@link UrlType} using {@link VirtualFileSystem#addDefaultURLTypes(UrlType)}.
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
 */
public abstract class VirtualFileSystem{

	public static final Logger LOGGER = JavaHelper.getLoggerFor(VirtualFileSystem.class);


	private static final List<UrlType> defaultUrlTypes = new ArrayList<>(Arrays.asList(DefaultUrlTypes.values()));


	/**
	 * Add a static default url types to the beginning of the default url types list. can be used to statically plug in urlTypes
	 *
	 * @param urlType	The URL type.
	 */
	public static void addDefaultURLTypes(final UrlType urlType){
		defaultUrlTypes.add(0, urlType);
	}


	/**
	 * default url types used by {@link VirtualFileSystem.DefaultUrlTypes#fromURL(URL)}
	 * <p>jarFile - creates a {@link ZipDirectory} over jar file
	 * <p>jarUrl - creates a {@link ZipDirectory} over a jar url (contains ".jar!/" in it's name), using Java's {@link JarURLConnection}
	 * <p>directory - creates a {@link SystemDirectory} over a file system directory
	 * <p>jboss vfs - for protocols vfs, using jboss vfs (should be provided in classpath)
	 * <p>jboss vfsfile - creates a {@link UrlTypeVFS} for protocols vfszip and vfsfile.
	 * <p>bundle - for bundle protocol, using eclipse FileLocator (should be provided in classpath)
	 * <p>jarInputStream - creates a {@link JarInputDirectory} over jar files, using Java's JarInputStream
	 */
	public enum DefaultUrlTypes implements UrlType{
		JAR_FILE{
			public boolean matches(final URL url){
				return (url.getProtocol().equals("file") && hasJarFileInPath(url));
			}

			public Directory createDir(final URL url) throws Exception{
				return new ZipDirectory(new JarFile(resourceToFile(url)));
			}
		},

		JAR_URL{
			private final Set<String> protocols = new HashSet<>(Arrays.asList("jar", "zip", "wsjar"));

			public boolean matches(final URL url){
				return protocols.contains(url.getProtocol());
			}

			public Directory createDir(final URL url) throws Exception{
				try{
					final URLConnection urlConnection = url.openConnection();
					if(urlConnection instanceof JarURLConnection){
						urlConnection.setUseCaches(false);
						return new ZipDirectory(((JarURLConnection) urlConnection).getJarFile());
					}
				}
				catch(final Throwable ignored){}

				final java.io.File file = resourceToFile(url);
				return (file != null? new ZipDirectory(new JarFile(file)): null);
			}
		},

		DIRECTORY{
			public boolean matches(final URL url){
				if(url.getProtocol().equals("file") && !hasJarFileInPath(url)){
					final java.io.File file = resourceToFile(url);
					return (file != null && file.isDirectory());
				}
				return false;
			}

			public Directory createDir(final URL url){
				return new SystemDirectory(resourceToFile(url));
			}
		},

		JBOSS_VFS{
			public boolean matches(final URL url){
				return url.getProtocol().equals("vfs");
			}

			public Directory createDir(final URL url) throws Exception{
				final Object content = url.openConnection().getContent();
				final Class<?> virtualFile = ClasspathHelper.contextClassLoader().loadClass("org.jboss.vfs.VirtualFile");
				final java.io.File physicalFile = (java.io.File)virtualFile.getMethod("getPhysicalFile").invoke(content);
				final String name = (String)virtualFile.getMethod("getName").invoke(content);
				java.io.File file = new java.io.File(physicalFile.getParentFile(), name);
				if(!file.exists() || !file.canRead())
					file = physicalFile;
				return (file.isDirectory()? new SystemDirectory(file): new ZipDirectory(new JarFile(file)));
			}
		},

		JBOSS_VFSFILE{
			private final Set<String> protocols = new HashSet<>(Arrays.asList("vfszip", "vfsfile"));

			public boolean matches(final URL url){
				return protocols.contains(url.getProtocol());
			}

			public Directory createDir(final URL url){
				return new UrlTypeVFS().createDir(url);
			}
		},

		BUNDLE{
			public boolean matches(final URL url){
				return url.getProtocol().startsWith("bundle");
			}

			public Directory createDir(final URL url) throws Exception{
				final Class<?> fileLocatorClass = ClasspathHelper.contextClassLoader().
					loadClass("org.eclipse.core.runtime.FileLocator");
				return fromURL((URL)fileLocatorClass.getMethod("resolve", URL.class).invoke(null, url));
			}
		},

		JAR_INPUT_STREAM{
			public boolean matches(final URL url){
				return url.toExternalForm().contains(".jar");
			}

			public Directory createDir(final URL url){
				return new JarInputDirectory(url);
			}
		};


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
						LOGGER.warn("could not create VirtualFileSystem.Directory using " + type + " from URL " + url.toExternalForm() + ": skipping", e);
				}
			}

			throw new ReflectionsException("could not create VirtualFileSystem.Directory from URL, no matching UrlType was found [" + url.toExternalForm() + "]\n" + "either use fromURL(final URL url, final List<UrlType> urlTypes) or " + "use the static setDefaultURLTypes(final List<UrlType> urlTypes) or addDefaultURLTypes(UrlType urlType) " + "with your specialized UrlType.");
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

	}

}
