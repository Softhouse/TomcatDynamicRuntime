package org.tdr.bootstrap;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.AccessController;
import java.security.CodeSource;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import javax.naming.directory.DirContext;

import org.apache.catalina.LifecycleException;
import org.apache.catalina.loader.ResourceEntry;
import org.apache.catalina.loader.WebappClassLoader;
import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.tdr.bootstrap.classloader.OSGiJarClassLoader;
import org.tdr.bootstrap.classloader.ParentClassLoader;
import org.tdr.osgi.OSGiUtils;
import org.tdr.runtime.RuntimeApplication;
import org.xeustechnologies.jcl.JarClassLoader;
import org.xeustechnologies.jcl.exception.JclException;

// TODO: Class.forname cachar antagligen Class & CL

public class OSGiWebAppLoader extends WebappClassLoader {
	
	static private Log log = LogFactory.getLog(OSGiWebAppLoader.class);
	
	private BundleContext bundleContext;
	private OSGiFileDirContext dirContext;
	private RuntimeApplication runtimeApplication;
	
	private OSGiJarClassLoader jarClassLoader;
		
	
	public OSGiWebAppLoader() {	
		this(null);
	}

	public OSGiWebAppLoader(ClassLoader parent) {
		super(parent);	
		this.createJarClassLoader();
		
		// TODO: Can we have problems with dir context references when doing redeploy...??
		this.dirContext = (OSGiFileDirContext) TLSReference.get("osgi/dirContext");
		
		log.info("Creating OSGI web app loader with dir context=" + dirContext);
	}
	
	protected void createJarClassLoader() {
		this.jarClassLoader = new OSGiJarClassLoader("BASE", this);
	}
	
	public OSGiJarClassLoader getJarClassLoader() {
		return this.jarClassLoader;
	}
	
	public void addJar(File jarFile) throws IOException {
		this.jarClassLoader.addJar(jarFile);
	}
	
	public void addClassPath(String classPath) throws IOException {	
		this.jarClassLoader.addClassPath(classPath);
	}
	
	public void removeClassPath(String classPath) {
		this.jarClassLoader.removeClassPath(classPath);
	}
	
	@Override
	public void start() throws LifecycleException {
		super.start();
		//this.dirContext.start();
		
		log.info("Starting OSGi webapp loader for context: " + this.getContextName());
		this.bundleContext = this.getBundleContext();
		if ( this.dirContext != null ) {
			this.runtimeApplication = this.dirContext.getRuntimeApplication();
		}
	}
	
	@Override
	public void stop() throws LifecycleException {
		//this.dirContext.stop();
		super.stop();
	}
	
	@Override
	public Class<?> loadClass(String name) throws ClassNotFoundException {
		OSGiJarClassLoader contextCL = (OSGiJarClassLoader) TLSReference.get("TDRCurrentBundleClassLoader");
		if ( contextCL != null ) {
			return contextCL.loadClass(name);
		}
		return this.jarClassLoader.loadClass(name);
	}
	
	/*
	@Override
	public Class<?> loadClass(String name) throws ClassNotFoundException {
	
		// Beroenode på vem som anropas -> child eller extern så borde man göra olika saker...
		// -> Gör en wrapper till denna klass som kan avgöra detta...
	
		//log.info("Loading class: " + name);
		if ( ! name.startsWith(":") && this.runtimeApplication != null ) {
			ClassLoader runtimeClassLoader = this.runtimeApplication.getClassLoader();
			if ( runtimeClassLoader != null && runtimeClassLoader != this ) {
				return runtimeClassLoader.loadClass(name);
			}
			else if ( Thread.currentThread().getContextClassLoader() != this ) {
				return Thread.currentThread().getContextClassLoader().loadClass(name);
			}
		}
		if ( name.startsWith(":") ) {
			name = name.substring(1);
		}
		return super.loadClass(name);	
	}
	*/

	/*
	@Override
	public Enumeration<URL> findResources(String name) throws IOException {
		
		if ( ! name.startsWith(":") && this.runtimeApplication != null ) {
			WebappClassLoader runtimeClassLoader = this.runtimeApplication.getClassLoader();
			if ( runtimeClassLoader != null && runtimeClassLoader != this ) {
				return runtimeClassLoader.findResources(name);
			}
			else if ( Thread.currentThread().getContextClassLoader() != this  &&
					  Thread.currentThread().getContextClassLoader() instanceof WebappClassLoader ) {
				WebappClassLoader webAppClassLoader = (WebappClassLoader) Thread.currentThread().getContextClassLoader();
				return webAppClassLoader.findResources(name);
			}
		}
		if ( name.startsWith(":") ) {
			name = name.substring(1);
		}
		return super.findResources(name);
	}
	*/
	
	
	/*
	@Override
	public URL getResource(String name) {
		
		log.info("Getting resource: " + name);
		if ( ! name.startsWith(":") && this.runtimeApplication != null ) {
			ClassLoader runtimeClassLoader = this.runtimeApplication.getClassLoader();
			if ( runtimeClassLoader != null && runtimeClassLoader != this ) {
				return runtimeClassLoader.getResource(name);
			}
//			else if ( Thread.currentThread().getContextClassLoader() != this ) {
//				return Thread.currentThread().getContextClassLoader().getResource(name);
//			}
		}
		if ( name.startsWith(":") ) {
			name = name.substring(1);
		}
		return super.getResource(name);
	}
	*/
	
	
	@Override
	public Enumeration<URL> getResources(String name) throws IOException {
		
		/*
		if ( ! name.startsWith(":") && this.runtimeApplication != null ) {
			ClassLoader runtimeClassLoader = this.runtimeApplication.getClassLoader();
			if ( runtimeClassLoader != null && runtimeClassLoader != this ) {
				return runtimeClassLoader.getResources(name);
			}
			else if ( Thread.currentThread().getContextClassLoader() != this ) {
				return Thread.currentThread().getContextClassLoader().getResources(name);
			}
		}
		if ( name.startsWith(":") ) {
			name = name.substring(1);
		}
		*/
		return super.getResources(name);
	}

	
	@Override
	public InputStream getResourceAsStream(String name) {
		
		/*
		if ( ! name.startsWith(":") && this.runtimeApplication != null ) {
			ClassLoader runtimeClassLoader = this.runtimeApplication.getClassLoader();
			if ( runtimeClassLoader != null && runtimeClassLoader != this ) {
				return runtimeClassLoader.getResourceAsStream(name);
			}
			else if ( Thread.currentThread().getContextClassLoader() != this ) {
				return Thread.currentThread().getContextClassLoader().getResourceAsStream(name);
			}
		}
		if ( name.startsWith(":") ) {
			name = name.substring(1);
		}
		*/

		return super.getResourceAsStream(name);
	}
	
	
	/*
	@Override
	public Class<?> findClass(String name) throws ClassNotFoundException {
		if ( ! name.startsWith(":") && this.runtimeApplication != null ) {
			WebappClassLoader runtimeClassLoader = this.runtimeApplication.getClassLoader();
			if ( runtimeClassLoader != null && runtimeClassLoader != this ) {
				return runtimeClassLoader.findClass(name);
			}
			else if ( Thread.currentThread().getContextClassLoader() != this &&
					  Thread.currentThread().getContextClassLoader() instanceof WebappClassLoader ) {
				WebappClassLoader webAppClassLoader = (WebappClassLoader) Thread.currentThread().getContextClassLoader();
				return webAppClassLoader.findClass(name);
			}
		}
		if ( name.startsWith(":") ) {
			name = name.substring(1);
		}
		return super.findClass(name);
	}

	@Override
	public URL findResource(String name) {
		if ( ! name.startsWith(":") && this.runtimeApplication != null ) {
			WebappClassLoader runtimeClassLoader = this.runtimeApplication.getClassLoader();
			if ( runtimeClassLoader != null && runtimeClassLoader != this ) {
				return runtimeClassLoader.findResource(name);
			}
			else if ( Thread.currentThread().getContextClassLoader() != this &&
					  Thread.currentThread().getContextClassLoader() instanceof WebappClassLoader ) {
				WebappClassLoader webAppClassLoader = (WebappClassLoader) Thread.currentThread().getContextClassLoader();
				return webAppClassLoader.findResource(name);
			}
		}
		if ( name.startsWith(":") ) {
			name = name.substring(1);
		}
		return super.findResource(name);
	}
	*/

	public synchronized void removeJar(File jarFile) {
		
		String path = jarFile.getPath();
		if ( isJarFileDefined(jarFile) ) {
			//this.clearClassCache(jarFile);
		
			File[] newFileList = new File[this.jarRealFiles.length-1];
			int jarIndex=0;
			int j=0;
			for ( int i=0; i < this.jarRealFiles.length; i++ ) {
				File file = this.jarRealFiles[i];
				if ( ! file.equals(jarFile) ) {
					newFileList[j++] = file;
				}
				else {
					jarIndex = i;
				}
			}
			this.jarRealFiles = newFileList;
			
			JarFile[] newJarList = new JarFile[this.jarFiles.length-1];
			j=0;
			for ( int i=0; i < this.jarFiles.length; i++ ) {
				JarFile file = this.jarFiles[i];
				if ( i == jarIndex ) {
					if ( file != null ) {
						try {
							//unloadJarResources(file);
							file.close();
						}
						catch ( IOException e ) {}
					}
				}
				else {
					newJarList[j++] = file;
				}
			}
			this.jarFiles = newJarList;
			
		}
		
		// Remove loaded JAR resources from the JAR class loader
		//
		
		
		// TODO: Clear up the class cache
		//this.resourceEntries
		
	}
	
	private void unloadJarResources(JarFile file) {
		
		Enumeration<JarEntry> entries = file.entries();
		while ( entries.hasMoreElements() ) {
			JarEntry entry = entries.nextElement();
			String name = entry.getName();
			if ( ! entry.isDirectory() && name.endsWith(".class" ) ) {
				String className = name.replace("/", ".").replace(".class", "");
				//log.info("Unloading class:" + className);
				try {
					this.resourceEntries.remove(name);
					//this.jarClassLoader.unloadClass(className);
				}
				catch ( JclException e ) {
					log.error("Could not unload class: " + className, e);
				}
			}
		}		
	}

	protected void clearClassCache(File sourceJarFile) {
	
		
		this.clearReferences();
		this.resourceEntries.clear();
//		try {
//			JarFile jar = new JarFile(sourceJarFile);
//		} 
//		catch (IOException e) {
//			log.error("Could not clear cache.");
//		}
		
		
	}
	
	public boolean isJarFileDefined(File jarFile) {
		boolean isDefined = false;
		for ( File file : this.jarRealFiles ) {
			if ( file.equals(jarFile) ) {
				isDefined = true;
				break;
			}
		}
		return isDefined;
	}
	
	// TODO: Add methods for removal of JAR files and clear class cache ???
	
	protected BundleContext getBundleContext() {
		return (BundleContext) GlobalReference.get("osgi/bundleContext");
	}

	protected Bundle getBundle(String name) {
		return OSGiUtils.getBundle(name, this.bundleContext);
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(super.toString());
		if ( this.runtimeApplication != null ) {
			ClassLoader runtimeCl = this.runtimeApplication.getClassLoader();
			if ( runtimeCl != null && runtimeCl != this ) {
				sb.append("\nRuntime class loader:\n");
				sb.append(runtimeCl);
			}
		}
//		for ( OSGiJarClassLoader classLoader : this.jarClassLoaders ) {
//			sb.append(classLoader);
//		}
		sb.append(this.jarClassLoader);
		return sb.toString();
	}

	
	// TEST

	/*
    @Override
    public synchronized Class<?> loadClass(String name, boolean resolve)
        throws ClassNotFoundException {

        if (log.isDebugEnabled())
            log.debug("loadClass(" + name + ", " + resolve + ")");
        Class<?> clazz = null;

        if ( name.equals("com.sun.faces.config.ConfigureListener") ) {
        	log.info("Loading faces configure listener...");
        }
        
        // Log access to stopped classloader
        if (!started) {
            try {
                throw new IllegalStateException();
            } catch (IllegalStateException e) {
                log.info(sm.getString("webappClassLoader.stopped", name), e);
            }
        }

        // (0) Check our previously loaded local class cache
//        clazz = findLoadedClass0(name);
//        if (clazz != null) {
//            if (log.isDebugEnabled())
//                log.debug("  Returning class from cache");
//            if (resolve)
//                resolveClass(clazz);
//            return (clazz);
//        }

        // (0.1) Check our previously loaded class cache
//        clazz = findLoadedClass(name);
//        if (clazz != null) {
//            if (log.isDebugEnabled())
//                log.debug("  Returning class from cache");
//            if (resolve)
//                resolveClass(clazz);
//            return (clazz);
//        }

        // (0.2) Try loading the class with the system class loader, to prevent
        //       the webapp from overriding J2SE classes
        try {
            clazz = system.loadClass(name);
            if (clazz != null) {
                if (resolve)
                    resolveClass(clazz);
                return (clazz);
            }
        } catch (ClassNotFoundException e) {
            // Ignore
        }

        // (0.5) Permission to access this class when using a SecurityManager
        if (securityManager != null) {
            int i = name.lastIndexOf('.');
            if (i >= 0) {
                try {
                    securityManager.checkPackageAccess(name.substring(0,i));
                } catch (SecurityException se) {
                    String error = "Security Violation, attempt to use " +
                        "Restricted Class: " + name;
                    log.info(error, se);
                    throw new ClassNotFoundException(error, se);
                }
            }
        }

        boolean delegateLoad = delegate || filter(name);

        // (1) Delegate to our parent if requested
        if (delegateLoad) {
            if (log.isDebugEnabled())
                log.debug("  Delegating to parent classloader1 " + parent);
            ClassLoader loader = parent;
            if (loader == null)
                loader = system;
            try {
                clazz = Class.forName(name, false, loader);
                if (clazz != null) {
                    if (log.isDebugEnabled())
                        log.debug("  Loading class from parent");
                    if (resolve)
                        resolveClass(clazz);
                    return (clazz);
                }
            } catch (ClassNotFoundException e) {
                // Ignore
            }
        }

        // (2) Search local repositories
        if (log.isDebugEnabled())
            log.debug("  Searching local repositories");
        try {
            clazz = findClass(name);
            if ( name.equals("com.sun.faces.config.ConfigureListener") ) {
            	log.info("Searched after faces class: " + clazz);
            }
            if (clazz != null) {
                if (log.isDebugEnabled())
                    log.debug("  Loading class from local repository");
                if (resolve)
                    resolveClass(clazz);
                return (clazz);
            }
        } catch (ClassNotFoundException e) {
            // Ignore
        }

        // (3) Delegate to parent unconditionally
        if (!delegateLoad) {
            if (log.isDebugEnabled())
                log.debug("  Delegating to parent classloader at end: " + parent);
            ClassLoader loader = parent;
            if (loader == null)
                loader = system;
            try {
                clazz = Class.forName(name, false, loader);
                if (clazz != null) {
                    if (log.isDebugEnabled())
                        log.debug("  Loading class from parent");
                    if (resolve)
                        resolveClass(clazz);
                    return (clazz);
                }
            } catch (ClassNotFoundException e) {
                // Ignore
            }
        }

        throw new ClassNotFoundException(name);

    }
    */
    
    /**
     * Find specified class in local repositories.
     *
     * @return the loaded class, or null if the class isn't found
     */
	/*
    protected Class<?> findClassInternal(String name)
        throws ClassNotFoundException {

        if (!validate(name))
            throw new ClassNotFoundException(name);

        String tempPath = name.replace('.', '/');
        String classPath = tempPath + ".class";

        ResourceEntry entry = null;

        if (securityManager != null) {
//            PrivilegedAction<ResourceEntry> dp =
//                new PrivilegedFindResourceByName(name, classPath);
//            entry = AccessController.doPrivileged(dp);
        } else {
            entry = findResourceInternal(name, classPath);
        }

        if (entry == null)
            throw new ClassNotFoundException(name);

        Class<?> clazz = entry.loadedClass;
        if (clazz != null)
            return clazz;

        synchronized (this) {
            clazz = entry.loadedClass;
            if (clazz != null)
                return clazz;

            if (entry.binaryContent == null)
                throw new ClassNotFoundException(name);

            // Looking up the package
            String packageName = null;
            int pos = name.lastIndexOf('.');
            if (pos != -1)
                packageName = name.substring(0, pos);

            Package pkg = null;

            if (packageName != null) {
                pkg = getPackage(packageName);
                // Define the package (if null)
                if (pkg == null) {
                    try {
                        if (entry.manifest == null) {
                            definePackage(packageName, null, null, null, null,
                                    null, null, null);
                        } else {
                            definePackage(packageName, entry.manifest,
                                    entry.codeBase);
                        }
                    } catch (IllegalArgumentException e) {
                        // Ignore: normal error due to dual definition of package
                    }
                    pkg = getPackage(packageName);
                }
            }

            if (securityManager != null) {

                // Checking sealing
                if (pkg != null) {
                    boolean sealCheck = true;
                    if (pkg.isSealed()) {
                        sealCheck = pkg.isSealed(entry.codeBase);
                    } else {
                        sealCheck = (entry.manifest == null)
                            || !isPackageSealed(packageName, entry.manifest);
                    }
                    if (!sealCheck)
                        throw new SecurityException
                            ("Sealing violation loading " + name + " : Package "
                             + packageName + " is sealed.");
                }

            }

            try {
            	if ( name.equals("com.sun.faces.config.ConfigureListener") ) {
            		log.info("Defining faces class...");
            	}
                clazz = defineClass(name, entry.binaryContent, 0,
                        entry.binaryContent.length,
                        new CodeSource(entry.codeBase, entry.certificates));
            } catch (UnsupportedClassVersionError ucve) {
                throw new UnsupportedClassVersionError(
                        ucve.getLocalizedMessage() + " " +
                        sm.getString("webappClassLoader.wrongVersion",
                                name));
            }
            entry.loadedClass = clazz;
            entry.binaryContent = null;
            entry.source = null;
            entry.codeBase = null;
            entry.manifest = null;
            entry.certificates = null;
        }

        return clazz;

    }
    */
	
}
