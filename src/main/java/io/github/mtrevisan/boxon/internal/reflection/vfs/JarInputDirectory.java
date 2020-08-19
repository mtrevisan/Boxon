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

import io.github.mtrevisan.boxon.internal.reflection.ReflectionsException;

import java.io.IOException;
import java.net.URL;
import java.util.Iterator;
import java.util.jar.JarInputStream;
import java.util.zip.ZipEntry;


public class JarInputDirectory implements Directory{

	private final URL url;
	JarInputStream jarInputStream;
	long cursor = 0;
	private long nextCursor = 0;


	JarInputDirectory(final URL url){
		this.url = url;
	}

	@Override
	public String getPath(){
		return url.getPath();
	}

	@Override
	public Iterable<File> getFiles(){
		return () -> new Iterator<>(){
			{
				try{
					jarInputStream = new JarInputStream(url.openConnection().getInputStream());
				}
				catch(final Exception e){
					throw new ReflectionsException("Could not open url connection", e);
				}
			}

			File entry = null;

			@Override
			public boolean hasNext(){
				return (entry != null || (entry = computeNext()) != null);
			}

			@Override
			public File next(){
				final File next = entry;
				entry = null;
				return next;
			}

			private File computeNext(){
				while(true){
					try{
						final ZipEntry entry = jarInputStream.getNextJarEntry();
						if(entry == null)
							return null;

						long size = entry.getSize();
						if(size < 0)
							size = 0xffffffffl + size; //JDK-6916399
						nextCursor += size;
						if(!entry.isDirectory())
							return new JarInputFile(entry, JarInputDirectory.this, cursor, nextCursor);
					}
					catch(final IOException e){
						throw new ReflectionsException("could not get next zip entry", e);
					}
				}
			}
		};
	}

}
