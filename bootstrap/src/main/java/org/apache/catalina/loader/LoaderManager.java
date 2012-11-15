package org.apache.catalina.loader;

import java.io.File;
import java.io.IOException;
import java.util.jar.JarFile;

public class LoaderManager {

	public static void addJarToWebappClassLoader(WebappClassLoader classLoader, File jarFile) throws IOException {
		classLoader.addJar(jarFile.getAbsolutePath(), new JarFile(jarFile), jarFile);
		classLoader.addRepository("file://" + jarFile.getAbsolutePath());
	}
	
	public static void addClassPathToWebappClassLoader(WebappClassLoader classLoader, String repository, File classPath) {
		classLoader.addRepository(repository, classPath);
	}
}
