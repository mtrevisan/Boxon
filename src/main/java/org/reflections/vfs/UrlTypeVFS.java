package org.reflections.vfs;

import org.reflections.Reflections;
import org.reflections.ReflectionsException;
import org.reflections.vfs.Vfs.Dir;
import org.reflections.vfs.Vfs.UrlType;

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
 * <p>to use it, register it in Vfs via {@link Vfs#addDefaultURLTypes(UrlType)} or {@link Vfs#setDefaultURLTypes(java.util.List)}.
 *
 * @author Sergio Pola
 */
public class UrlTypeVFS implements UrlType{
	public final static String[] REPLACE_EXTENSION = new String[]{".ear/", ".jar/", ".war/", ".sar/", ".har/", ".par/"};

	final String VFSZIP = "vfszip";
	final String VFSFILE = "vfsfile";


	public boolean matches(final URL url){
		return VFSZIP.equals(url.getProtocol()) || VFSFILE.equals(url.getProtocol());
	}

	public Dir createDir(final URL url){
		try{
			final URL adaptedUrl = adaptURL(url);
			return new ZipDir(new JarFile(adaptedUrl.getFile()));
		}
		catch(final Exception e){
			try{
				return new ZipDir(new JarFile(url.getFile()));
			}
			catch(final IOException e1){
				if(Reflections.LOGGER != null){
					Reflections.LOGGER.warn("Could not get URL", e);
					Reflections.LOGGER.warn("Could not get URL", e1);
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
		for(int i = 0; i < numSubs; i ++)
			prefix.append("zip:");

		if(zipPath.trim().length() == 0)
			return new URL(prefix + "/" + zipFile);
		else
			return new URL(prefix + "/" + zipFile + "!" + zipPath);
	}

}
