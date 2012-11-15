package org.tdr.bootstrap.classloader;

import java.io.InputStream;
import java.util.List;

import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;
import org.xeustechnologies.jcl.JarClassLoader;
import org.xeustechnologies.jcl.ProxyClassLoader;

public class ChildJarClassLoader extends ProxyClassLoader {

	private static final Log log = LogFactory.getLog(ChildJarClassLoader.class);

	private List<OSGiJarClassLoader> jarClassLoaders;
	
	public ChildJarClassLoader(List<OSGiJarClassLoader> jarClassLoaders) {
		this.jarClassLoaders = jarClassLoaders;
		this.order = 4;
	}
	
	@Override
	public Class loadClass(String className, boolean resolveIt) {
		for ( OSGiJarClassLoader jarLoader : this.jarClassLoaders ) {
			if ( className.equals("javax.faces.application.ApplicationFactory") ) {
				log.debug("Trying class loader: " + jarLoader + " for class: " + className);
			}
			try {
				//jarLoader.getParentClassLoader().setEnabled(false);
				Class clazz = jarLoader.loadClass(className, resolveIt);
				log.debug("Loaded class: " + className + " via JAR loader: " + jarLoader.toString());
				return clazz;
			}
			catch ( ClassNotFoundException e ) { /* Ignore - Continue with next loader */ }
			finally {
				//jarLoader.getParentClassLoader().setEnabled(true);
			}
		}
		return null;
	}

	@Override
	public InputStream loadResource(String name) {
		for ( OSGiJarClassLoader jarLoader : this.jarClassLoaders ) {
			jarLoader.getParentClassLoader().setEnabled(false);
			InputStream is = jarLoader.getResourceAsStream(name);
			jarLoader.getParentClassLoader().setEnabled(true);
			if ( is != null ) return is;
		}
		return null;
	}

	

}
