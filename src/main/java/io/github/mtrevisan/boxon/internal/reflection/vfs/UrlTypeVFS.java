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
 * UrlType to be used by Reflections library.
 * This class handles the vfszip and vfsfile protocol of JBOSS files.
 * <p>to use it, register it in Vfs via {@link VirtualFileSystem#addDefaultURLTypes(UrlType)} or {@link VirtualFileSystem#setDefaultURLTypes(java.util.List)}.
 *
 * @author Sergio Pola
 */
public class UrlTypeVFS implements UrlType{

	public static final Logger LOGGER = JavaHelper.getLoggerFor(UrlTypeVFS.class);

	public final static String[] REPLACE_EXTENSION = new String[]{".ear/", ".jar/", ".war/", ".sar/", ".har/", ".par/"};

	final String VFSZIP = "vfszip";
	final String VFSFILE = "vfsfile";


	public boolean matches(final URL url){
		return VFSZIP.equals(url.getProtocol()) || VFSFILE.equals(url.getProtocol());
	}

	public Directory createDir(final URL url){
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

	public URL adaptURL(final URL url) throws MalformedURLException{
		if(VFSZIP.equals(url.getProtocol()))
			return replaceZipSeparators(url.getPath(), realFile);
		else if(VFSFILE.equals(url.getProtocol()))
			return new URL(url.toString().replace(VFSFILE, "file"));
		else
			return url;
	}

	URL replaceZipSeparators(final String path, final Predicate<File> acceptFile) throws MalformedURLException{
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

	int findFirstMatchOfDeployableExtension(final String path, final int pos){
		final Pattern p = Pattern.compile("\\.[ejprw]ar/");
		final Matcher m = p.matcher(path);
		if(m.find(pos))
			return m.end();
		else
			return -1;
	}

	final Predicate<File> realFile = file -> file.exists() && file.isFile();

	URL replaceZipSeparatorStartingFrom(final String path, final int pos) throws MalformedURLException{
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
