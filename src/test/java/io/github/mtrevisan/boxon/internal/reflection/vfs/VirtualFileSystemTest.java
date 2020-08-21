package io.github.mtrevisan.boxon.internal.reflection.vfs;

import io.github.mtrevisan.boxon.internal.reflection.adapters.MetadataAdapterBuilder;
import io.github.mtrevisan.boxon.internal.reflection.adapters.MetadataAdapterInterface;
import io.github.mtrevisan.boxon.internal.reflection.helpers.ClasspathHelper;
import javassist.bytecode.ClassFile;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.Iterator;


class VirtualFileSystemTest{

	@Test
	void testJarFile() throws Exception{
		URL url = new URL(ClasspathHelper.forClass(Logger.class).toExternalForm().replace("jar:", ""));
		Assertions.assertTrue(url.toString().startsWith("file:"));
		Assertions.assertTrue(url.toString().contains(".jar"));

		Assertions.assertTrue(VirtualFileSystem.DefaultUrlTypes.JAR_FILE.matches(url));
		Assertions.assertFalse(VirtualFileSystem.DefaultUrlTypes.JAR_URL.matches(url));
		Assertions.assertFalse(VirtualFileSystem.DefaultUrlTypes.DIRECTORY.matches(url));

		VFSDirectory dir = VirtualFileSystem.DefaultUrlTypes.JAR_FILE.createDirectory(url);
		testVirtualFileSystemDir(dir);
	}

	@Test
	void testJarUrl() throws Exception{
		URL url = ClasspathHelper.forClass(Logger.class);
		Assertions.assertTrue(url.toString().startsWith("jar:file:"));
		Assertions.assertTrue(url.toString().contains(".jar!"));

		Assertions.assertFalse(VirtualFileSystem.DefaultUrlTypes.JAR_FILE.matches(url));
		Assertions.assertTrue(VirtualFileSystem.DefaultUrlTypes.JAR_URL.matches(url));
		Assertions.assertFalse(VirtualFileSystem.DefaultUrlTypes.DIRECTORY.matches(url));

		VFSDirectory dir = VirtualFileSystem.DefaultUrlTypes.JAR_URL.createDirectory(url);
		testVirtualFileSystemDir(dir);
	}

	@Test
	void testDirectory() throws Exception{
		URL url = ClasspathHelper.forClass(getClass());
		Assertions.assertTrue(url.toString().startsWith("file:"));
		Assertions.assertFalse(url.toString().contains(".jar"));

		Assertions.assertFalse(VirtualFileSystem.DefaultUrlTypes.JAR_FILE.matches(url));
		Assertions.assertFalse(VirtualFileSystem.DefaultUrlTypes.JAR_URL.matches(url));
		Assertions.assertTrue(VirtualFileSystem.DefaultUrlTypes.DIRECTORY.matches(url));

		VFSDirectory dir = VirtualFileSystem.DefaultUrlTypes.DIRECTORY.createDirectory(url);
		testVirtualFileSystemDir(dir);
	}

	@Test
	void testJarInputStream() throws Exception{
		URL url1 = ClasspathHelper.forClass(Logger.class);
		Assertions.assertTrue(VirtualFileSystem.DefaultUrlTypes.JAR_INPUT_STREAM.matches(url1));
		Assertions.assertThrows(VFSException.class, () -> testVirtualFileSystemDir(VirtualFileSystem.DefaultUrlTypes.JAR_INPUT_STREAM.createDirectory(url1)));

		URL url2 = new URL(ClasspathHelper.forClass(Logger.class).toExternalForm().replace("jar:", "").replace(".jar!", ".jar"));
		Assertions.assertTrue(VirtualFileSystem.DefaultUrlTypes.JAR_INPUT_STREAM.matches(url2));
		testVirtualFileSystemDir(VirtualFileSystem.DefaultUrlTypes.JAR_INPUT_STREAM.createDirectory(url2));

		URL url3 = ClasspathHelper.forClass(getClass());
		Assertions.assertFalse(VirtualFileSystem.DefaultUrlTypes.JAR_INPUT_STREAM.matches(url3));
		Assertions.assertThrows(NullPointerException.class, () -> testVirtualFileSystemDir(VirtualFileSystem.DefaultUrlTypes.JAR_INPUT_STREAM.createDirectory(url3)));
	}

	@Test
	void dirWithSpaces(){
		Collection<URL> urls = ClasspathHelper.forPackage("dir+with spaces");
		Assertions.assertFalse(urls.isEmpty());
		for(URL url : urls){
			VFSDirectory dir = VirtualFileSystem.fromURL(url);
			Assertions.assertNotNull(dir);
			Assertions.assertNotNull(dir.getFiles().iterator().next());
		}
	}

	@Test
	void virtualFileSystemFromDirWithJarInName() throws MalformedURLException{
		String tmpFolder = System.getProperty("java.io.tmpdir");
		tmpFolder = (tmpFolder.endsWith(File.separator)? tmpFolder: tmpFolder + File.separator);
		String dirWithJarInName = tmpFolder + "tony.jarvis";
		File newDir = new File(dirWithJarInName);
		newDir.mkdir();

		try{
			VFSDirectory dir = VirtualFileSystem.fromURL(new URL(MessageFormat.format("file:{0}", dirWithJarInName)));

			Assertions.assertEquals(dirWithJarInName.replace('\\', '/'), dir.getPath());
			Assertions.assertEquals(SystemDirectory.class, dir.getClass());
		}
		finally{
			newDir.delete();
		}
	}

	private void testVirtualFileSystemDir(final VFSDirectory dir) throws Exception{
		//this should be a JavassistAdapter instance
		MetadataAdapterInterface<ClassFile> mdAdapter = (MetadataAdapterInterface<ClassFile>)MetadataAdapterBuilder.getMetadataAdapter();
		VFSFile file = null;
		Iterator<VFSFile> itr = dir.getFiles().iterator();
		while(itr.hasNext()){
			VFSFile f = itr.next();
			if(f.getRelativePath().endsWith(".class")){
				file = f;
				break;
			}
		}

		ClassFile stringCF = mdAdapter.createClassObject(dir, file);
		String className = mdAdapter.getClassName(stringCF);
		Assertions.assertFalse(className.isEmpty());

		dir.close();
	}


}
