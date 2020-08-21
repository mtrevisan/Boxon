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
import org.slf4j.Logger;

import java.io.File;
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
 * <p>Use the {@link VirtualFileSystem#fromURL(URL)} to get a {@link VFSDirectory},
 * then use {@link VFSDirectory#getFiles()} to iterate over the {@link VFSFile}s.
 * <p>for example:
 * <pre><code>
 *      VFSDirectory dir = VirtualFileSystem.fromURL(url);
 *      for(VFSFile file : dir.getFiles())
 *          InputStream is = file.openInputStream();
 * </code></pre>
 * <p>{@link VirtualFileSystem#fromURL(URL)} uses static {@link DefaultUrlTypes} to resolve URLs.
 * It contains types for handling for common resources such as local jar file, local directory, jar url, jar input stream and more.
 */
public final class VirtualFileSystem{

	private static final Logger LOGGER = JavaHelper.getLoggerFor(VirtualFileSystem.class);

	private static final List<UrlType> DEFAULT_URL_TYPES = new ArrayList<>(Arrays.asList(DefaultUrlTypes.values()));


	private VirtualFileSystem(){}

	/**
	 * Tries to create a Dir from the given url, using the defaultUrlTypes
	 *
	 * @param url	The URL.
	 * @return	The Dir from the given {@code url}.
	 */
	public static VFSDirectory fromURL(final URL url){
		for(final UrlType type : DEFAULT_URL_TYPES){
			try{
				if(type.matches(url)){
					final VFSDirectory directory = type.createDirectory(url);
					if(directory != null)
						return directory;
				}
			}
			catch(final Throwable e){
				if(LOGGER != null)
					LOGGER.warn("Could not create VFSDirectory using {} from URL {}: skipping", type, url.toExternalForm(), e);
			}
		}

		throw new VFSException("Could not create VFSDirectory from URL, no matching UrlType was found [{}]\n"
			+ "either use fromURL(final URL url) with your specialized UrlType.", url.toExternalForm());
	}

	/**
	 * Default url types used by {@link VirtualFileSystem#fromURL(URL)}.
	 * <p>JAR_FILE - creates a {@link ZipDirectory} over JAR file.
	 * <p>JAR_URL - creates a {@link ZipDirectory} over a JAR URL (contains {@code ".jar!/"} in it's name), using Java's {@link JarURLConnection}.
	 * <p>DIRECTORY - creates a {@link SystemDirectory} over a file system directory.
	 * <p>BUNDLE - for bundle protocol, using eclipse FileLocator (should be provided in classpath).
	 * <p>JAR_INPUT_STREAM - creates a {@link JarInputDirectory} over JAR files, using Java's {@link java.util.jar.JarInputStream JarInputStream}.
	 */
	enum DefaultUrlTypes implements UrlType{
		JAR_FILE{
			@Override
			public boolean matches(final URL url){
				return (url.getProtocol().equals("file") && hasJarFileInPath(url));
			}

			@Override
			public VFSDirectory createDirectory(final URL url) throws Exception{
				return new ZipDirectory(new JarFile(resourceToFile(url)));
			}
		},

		JAR_URL{
			private final Set<String> protocols = new HashSet<>(Arrays.asList("jar", "zip", "wsjar"));

			@Override
			public boolean matches(final URL url){
				return protocols.contains(url.getProtocol());
			}

			@Override
			public VFSDirectory createDirectory(final URL url) throws Exception{
				try{
					final URLConnection urlConnection = url.openConnection();
					if(urlConnection instanceof JarURLConnection){
						urlConnection.setUseCaches(false);
						return new ZipDirectory(((JarURLConnection)urlConnection).getJarFile());
					}
				}
				catch(final Throwable ignored){}

				final File file = resourceToFile(url);
				return (file != null? new ZipDirectory(new JarFile(file)): null);
			}
		},

		DIRECTORY{
			@Override
			public boolean matches(final URL url){
				if(url.getProtocol().equals("file") && !hasJarFileInPath(url)){
					final File file = resourceToFile(url);
					return (file != null && file.isDirectory());
				}
				return false;
			}

			@Override
			public VFSDirectory createDirectory(final URL url){
				return new SystemDirectory(resourceToFile(url));
			}
		},

		BUNDLE{
			@Override
			public boolean matches(final URL url){
				return url.getProtocol().startsWith("bundle");
			}

			@Override
			public VFSDirectory createDirectory(final URL url) throws Exception{
				final Class<?> fileLocatorClass = Thread.currentThread().getContextClassLoader()
					.loadClass("org.eclipse.core.runtime.FileLocator");
				return fromURL((URL)fileLocatorClass.getMethod("resolve", URL.class).invoke(null, url));
			}
		},

		JAR_INPUT_STREAM{
			@Override
			public boolean matches(final URL url){
				return url.toExternalForm().contains(".jar");
			}

			@Override
			public VFSDirectory createDirectory(final URL url){
				return new JarInputDirectory(url);
			}
		};


		/**
		 * Get {@link File} from URL.
		 *
		 * @param url	The URL to get the file from.
		 * @return	{@link File} from URL.
		 */
		private static File resourceToFile(final URL url){
			File file = resourceToFileFromSchemePart(url);
			if(file == null)
				file = resourceToFileFromDecodedPath(url);
			if(file == null)
				file = resourceToFileFromExternalPath(url);
			return file;
		}

		private static File resourceToFileFromSchemePart(final URL url){
			File file = null;
			try{
				final String path = url.toURI().getSchemeSpecificPart();
				file = getFileIfExists(path);
			}
			catch(final URISyntaxException | FileNotFoundException ignored){}
			return file;
		}

		private static File resourceToFileFromDecodedPath(final URL url){
			File file = null;
			try{
				final String path = extractDecodedPath(url);
				file = getFileIfExists(path);
			}
			catch(final FileNotFoundException ignored){}
			return file;
		}

		private static File resourceToFileFromExternalPath(final URL url){
			File file = null;
			try{
				String path = extractExternalFormPath(url);
				file = new File(path);
				if(!file.exists()){
					path = path.replace("%20", " ");
					file = getFileIfExists(path);
				}
			}
			catch(final Exception ignored){}
			return file;
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

		private static File getFileIfExists(final String path) throws FileNotFoundException{
			final File file = new File(path);
			if(!file.exists())
				throw new FileNotFoundException();

			return file;
		}

		private static boolean hasJarFileInPath(final URL url){
			return url.toExternalForm().matches(".*\\.jar(!.*|$)");
		}

	}

}
