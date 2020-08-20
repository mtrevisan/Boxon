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
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.function.Predicate;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * {@link UrlType} to be used by {@link io.github.mtrevisan.boxon.internal.reflection.Reflections Reflections}.
 * <p>This class handles the vfszip and vfsfile protocol of JBOSS files.</p>
 *
 * @author	Sergio Pola
 */
class UrlTypeVFS implements UrlType{

	private static final Logger LOGGER = JavaHelper.getLoggerFor(UrlTypeVFS.class);

	private static final String[] REPLACE_EXTENSION = new String[]{".ear/", ".jar/", ".war/", ".sar/", ".har/", ".par/"};

	private static final String VFSZIP = "vfszip";
	private static final String VFSFILE = "vfsfile";

	private static final Matcher EXTENSIONS = Pattern.compile("\\.[ejprw]ar/").matcher("");


	@Override
	public boolean matches(final URL url){
		return VFSZIP.equals(url.getProtocol()) || VFSFILE.equals(url.getProtocol());
	}

	@Override
	public VFSDirectory createDir(final URL url){
		try{
			final URL adaptedUrl = adaptURL(url);
			return new ZipDirectory(new JarFile(adaptedUrl.getFile()));
		}
		catch(final Exception e){
			try{
				return new ZipDirectory(new JarFile(url.getFile()));
			}
			catch(final IOException e1){
				if(LOGGER != null){
					LOGGER.warn("Could not get URL", e);
					LOGGER.warn("Could not get URL", e1);
				}
			}
		}
		return null;
	}

	private URL adaptURL(final URL url) throws MalformedURLException{
		if(VFSZIP.equals(url.getProtocol()))
			return replaceZipSeparators(url.getPath(), realFile);
		else if(VFSFILE.equals(url.getProtocol()))
			return new URL(url.toString().replace(VFSFILE, "file"));
		else
			return url;
	}

	private URL replaceZipSeparators(final String path, final Predicate<File> acceptFile) throws MalformedURLException{
		int pos = 0;
		while(pos != -1){
			pos = findFirstMatchOfDeployableExtension(path, pos);

			if(pos > 0){
				final File file = new File(path.substring(0, pos - 1));
				if(acceptFile.test(file))
					return replaceZipSeparatorStartingFrom(path, pos);
			}
		}

		throw new ReflectionsException("Unable to identify the real zip file in path '" + path + "'.");
	}

	private int findFirstMatchOfDeployableExtension(final String path, final int pos){
		final Matcher m = EXTENSIONS.reset(path);
		return (m.find(pos)? m.end(): -1);
	}

	private final Predicate<File> realFile = file -> file.exists() && file.isFile();

	private URL replaceZipSeparatorStartingFrom(final String path, final int pos) throws MalformedURLException{
		final String zipFile = path.substring(0, pos - 1);
		String zipPath = path.substring(pos);

		int numSubs = 1;
		for(final String ext : REPLACE_EXTENSION)
			while(zipPath.contains(ext)){
				zipPath = zipPath.replace(ext, ext.substring(0, 4) + "!");
				numSubs ++;
			}

		final StringBuilder prefix = new StringBuilder();
		prefix.append("zip:".repeat(Math.max(0, numSubs)))
			.append('/')
			.append(zipFile);
		if(zipPath.trim().length() > 0)
			prefix.append('!')
				.append(zipPath);
		return new URL(prefix.toString());
	}

}
