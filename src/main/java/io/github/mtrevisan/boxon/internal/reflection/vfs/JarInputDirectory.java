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

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Iterator;
import java.util.jar.JarInputStream;
import java.util.zip.ZipEntry;


/**
 * An implementation of {@link VFSDirectory} for a {@link JarInputStream} in a JAR.
 */
class JarInputDirectory implements VFSDirectory{

	private static final Logger LOGGER = JavaHelper.getLoggerFor(JarInputDirectory.class);

	private final URL url;
	private JarInputStream jarInputStream;
	private long cursor;
	private long nextCursor;


	JarInputDirectory(final URL url){
		this.url = url;
	}

	@Override
	public String getPath(){
		return url.getPath();
	}

	@Override
	public Iterable<VFSFile> getFiles(){
		return () -> new Iterator<>(){
			{
				try{
					jarInputStream = new JarInputStream(url.openConnection().getInputStream());
				}
				catch(final Exception e){
					throw new VFSException("Could not open URL connection")
						.withCause(e);
				}
			}

			private VFSFile entry;


			@Override
			public boolean hasNext(){
				return (entry != null || (entry = computeNext()) != null);
			}

			@Override
			public VFSFile next(){
				final VFSFile next = entry;
				entry = null;
				return next;
			}

			private VFSFile computeNext(){
				while(true){
					try{
						final ZipEntry entry = jarInputStream.getNextJarEntry();
						if(entry == null)
							return null;

						long size = entry.getSize();
						if(size < 0)
							//JDK-6916399 (no longer reproducible in 6u18+)
							size = 0xFFFF_FFFFl + size;
						nextCursor += size;
						if(!entry.isDirectory())
							return new JarInputFile(entry, cursor, nextCursor);
					}
					catch(final IOException e){
						throw new VFSException("Could not get next zip entry")
							.withCause(e);
					}
				}
			}
		};
	}

	@Override
	public void close(){
		try{
			if(jarInputStream != null)
				jarInputStream.close();
		}
		catch(final IOException e){
			if(LOGGER != null)
				LOGGER.warn("Could not close InputStream", e);
		}
	}

	@Override
	public InputStream openInputStream(final VFSFile file){
		return new InputStream(){
			@Override
			public int read() throws IOException{
				int read = -1;
				if(cursor >= ((JarInputFile)file).fromIndex && cursor <= ((JarInputFile)file).endIndex){
					read = jarInputStream.read();
					cursor ++;
				}
				return read;
			}
		};
	}

}
