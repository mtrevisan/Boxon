package io.github.mtrevisan.boxon.internal.reflection;

import io.github.mtrevisan.boxon.internal.reflection.adapters.JavassistAdapter;
import io.github.mtrevisan.boxon.internal.reflection.utils.ClasspathHelper;
import io.github.mtrevisan.boxon.internal.reflection.vfs.Directory;
import io.github.mtrevisan.boxon.internal.reflection.vfs.File;
import io.github.mtrevisan.boxon.internal.reflection.vfs.SystemDirectory;
import io.github.mtrevisan.boxon.internal.reflection.vfs.VirtualFileSystem;
import javassist.bytecode.ClassFile;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;

import java.net.MalformedURLException;
import java.net.URL;
import java.text.MessageFormat;
import java.util.Collection;


class VirtualFileSystemTest{

	@Test
	void testJarFile() throws Exception{
		URL url = new URL(ClasspathHelper.forClass(Logger.class).toExternalForm().replace("jar:", ""));
		Assertions.assertTrue(url.toString().startsWith("file:"));
		Assertions.assertTrue(url.toString().contains(".jar"));

		Assertions.assertTrue(VirtualFileSystem.DefaultUrlTypes.JAR_FILE.matches(url));
		Assertions.assertFalse(VirtualFileSystem.DefaultUrlTypes.JAR_URL.matches(url));
		Assertions.assertFalse(VirtualFileSystem.DefaultUrlTypes.DIRECTORY.matches(url));

		Directory dir = VirtualFileSystem.DefaultUrlTypes.JAR_FILE.createDir(url);
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

		Directory dir = VirtualFileSystem.DefaultUrlTypes.JAR_URL.createDir(url);
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

		Directory dir = VirtualFileSystem.DefaultUrlTypes.DIRECTORY.createDir(url);
		testVirtualFileSystemDir(dir);
	}

	@Test
	void testJarInputStream() throws Exception{
		URL url = ClasspathHelper.forClass(Logger.class);
		Assertions.assertTrue(VirtualFileSystem.DefaultUrlTypes.JAR_INPUT_STREAM.matches(url));
		try{
			testVirtualFileSystemDir(VirtualFileSystem.DefaultUrlTypes.JAR_INPUT_STREAM.createDir(url));
			Assertions.fail();
		}
		catch(ReflectionsException e){
			// expected
		}

		url = new URL(ClasspathHelper.forClass(Logger.class).toExternalForm().replace("jar:", "").replace(".jar!", ".jar"));
		Assertions.assertTrue(VirtualFileSystem.DefaultUrlTypes.JAR_INPUT_STREAM.matches(url));
		testVirtualFileSystemDir(VirtualFileSystem.DefaultUrlTypes.JAR_INPUT_STREAM.createDir(url));

		url = ClasspathHelper.forClass(getClass());
		Assertions.assertFalse(VirtualFileSystem.DefaultUrlTypes.JAR_INPUT_STREAM.matches(url));
		try{
			testVirtualFileSystemDir(VirtualFileSystem.DefaultUrlTypes.JAR_INPUT_STREAM.createDir(url));
			Assertions.fail();
		}
		catch(NullPointerException e){
			// expected
		}
	}

	@Test
	void dirWithSpaces(){
		Collection<URL> urls = ClasspathHelper.forPackage("dir+with spaces");
		Assertions.assertFalse(urls.isEmpty());
		for(URL url : urls){
			Directory dir = VirtualFileSystem.fromURL(url);
			Assertions.assertNotNull(dir);
			Assertions.assertNotNull(dir.getFiles().iterator().next());
		}
	}

	@Test
	void virtualFileSystemFromDirWithJarInName() throws MalformedURLException{
		String tmpFolder = System.getProperty("java.io.tmpdir");
		tmpFolder = tmpFolder.endsWith(java.io.File.separator)? tmpFolder: tmpFolder + java.io.File.separator;
		String dirWithJarInName = tmpFolder + "tony.jarvis";
		java.io.File newDir = new java.io.File(dirWithJarInName);
		newDir.mkdir();

		try{
			Directory dir = VirtualFileSystem.fromURL(new URL(MessageFormat.format("file:{0}", dirWithJarInName)));

			Assertions.assertEquals(dirWithJarInName, dir.getPath());
			Assertions.assertEquals(SystemDirectory.class, dir.getClass());
		}
		finally{
			newDir.delete();
		}
	}

	private void testVirtualFileSystemDir(Directory dir){
		JavassistAdapter mdAdapter = new JavassistAdapter();
		File file = null;
		for(File f : dir.getFiles()){
			if(f.getRelativePath().endsWith(".class")){
				file = f;
				break;
			}
		}

		ClassFile stringCF = mdAdapter.getOrCreateClassObject(file);
		String className = mdAdapter.getClassName(stringCF);
		Assertions.assertFalse(className.isEmpty());
	}


}
