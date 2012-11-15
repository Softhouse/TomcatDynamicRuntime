package org.tdr.bootstrap.classloader;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import org.apache.catalina.loader.WebappClassLoader;
import org.xeustechnologies.jcl.ProxyClassLoader;


// TODO: Move to a new package with the other class loader classes!!!

public class ParentClassLoader extends ProxyClassLoader {

	private ClassLoader parent;
	
	public ParentClassLoader(ClassLoader parent) {
		this.parent = parent;
		this.order = 5;
	}
	
	public void updateParent(ClassLoader newParent) {
		this.parent = newParent;
	}
	
	public ClassLoader getParent() {
		return this.parent;
	}
	
	@Override
	public Class loadClass(String className, boolean resolveIt) {
		
		// TODO: REFACTOR HERE AND CLEAN UP!!!
		
		try {
			if ( parent instanceof WebappClassLoader ) {
				WebappClassLoader webAppClassLoader = (WebappClassLoader) parent;
				return webAppClassLoader.loadClass(className, resolveIt);
			}
			else if ( parent instanceof OSGiJarClassLoader ) {
				OSGiJarClassLoader jarClassLoader = (OSGiJarClassLoader) parent;
				return jarClassLoader.loadClass(className, resolveIt);
			}
			else {
				return null;
			}
		}
		catch ( ClassNotFoundException e ) {
			return null;
		}
	}

	@Override
	public InputStream loadResource(String name) {
		return this.parent.getResourceAsStream(name);
	}
	
	public URL getResource(String name) {
		return this.parent.getResource(name);
	}
	
	public List<URL> getResources(String name) {
		
		ArrayList<URL> list = new ArrayList<URL>();
		try {
			Enumeration<URL> urls = this.parent.getResources(name);
			while ( urls.hasMoreElements() ) {
				list.add(urls.nextElement());
			}
		}
		catch ( IOException e ) {}
		return list;
	}

	@Override
	public String toString() {
		return this.parent.toString();
	}

}
