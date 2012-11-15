package org.tdr.bootstrap.classloader;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.apache.catalina.loader.ResourceEntry;
import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;
import org.apache.tomcat.util.ExceptionUtils;
import org.tdr.util.BytesURLStreamHandler;
import org.xeustechnologies.jcl.JarClassLoader;
import org.xeustechnologies.jcl.exception.JclException;

public class OSGiJarClassLoader extends JarClassLoader {

	static private Log log = LogFactory.getLog(OSGiJarClassLoader.class);
	
	private ParentClassLoader parent;
	private String label;
	
	private List<OSGiJarClassLoader> jarClassLoaders = new ArrayList<OSGiJarClassLoader>();
	
	private static ThreadLocal<Map<String,Stack<String>>> callStackTLS = new ThreadLocal<Map<String, Stack<String>>>();
	
	static public void clearCallStack() {
		callStackTLS.remove();
	}
	
	public OSGiJarClassLoader(String label, ClassLoader parent) {
		this.label = label;
		this.getOsgiBootLoader().setEnabled(false);
		this.getParentLoader().setEnabled(false);
		this.getSystemLoader().setEnabled(false);
		if ( parent != null ) {
			this.parent = new ParentClassLoader(parent);
			super.addLoader(this.parent);
		}
		super.addLoader(new ChildJarClassLoader(this.jarClassLoaders));
	}
	
	public ParentClassLoader getParentClassLoader() {
		return this.parent;
	}
	
	public String getLabel() {
		return this.label;
	}
	
	public boolean isEnabled() {
		return this.getLocalLoader().isEnabled();
	}
	
	public OSGiJarClassLoader newChild(String label) {
		log.info("Creating a new child JAR class loader with label: " + label);
		OSGiJarClassLoader jarClassLoader = new OSGiJarClassLoader(label, this);
		this.jarClassLoaders.add(jarClassLoader);
		return jarClassLoader;
	}
	
	public void removeChild(String label) {
		this.removeJarLoader(label);
	}
	
	public void unloadClasses() {
		this.clearReferencesStaticFinal();
		for ( String className : this.getLoadedClasses().keySet().toArray(new String[0]) ) {
			this.unloadClass(className);
		}
	}
	
	public void addJar(File jarFile) throws IOException {
		this.add(jarFile.toURL());
//		OSGiJarClassLoader jarClassLoader = new OSGiJarClassLoader(jarFile.getName(), this);
//		jarClassLoader.add(jarFile.toURL());
//		this.jarClassLoaders.add(jarClassLoader);
	}
	
	public void removeJar(File jarFile) throws IOException {
		this.removeJarLoader(jarFile.getName());
//		if ( classLoader != null ) {
//			unloadJarResources(classLoader, jarFile);
//		}
		
	}
	
	// TODO: Add some merge function here to merge classpaths with JARs so they "publically" belongs to the same CL...
	// OR: Have a CL per module...?
	
	public void addClassPath(String classPath) throws IOException {
		this.add(new File(classPath).toURL());
//		OSGiJarClassLoader classLoader = new OSGiJarClassLoader(classPath, this);
//		classLoader.add(new File(classPath).toURL());
//		this.jarClassLoaders.add(classLoader);
	}
	
	public void removeClassPath(String classPath) {
		this.removeJarLoader(classPath);
	}
	
	public OSGiJarClassLoader getJarLoader(String name) {
		for ( OSGiJarClassLoader classLoader : this.jarClassLoaders ) {
			if ( classLoader.getLabel().equals(name) ) {
				return classLoader;
			}
		}
		return null;
	}
	
	public void removeJarLoader(String name) {
		OSGiJarClassLoader classLoader = this.getJarLoader(name);
		if ( classLoader != null ) {
			classLoader.unloadClasses();
			classLoader.getLocalLoader().setEnabled(false);
			this.jarClassLoaders.remove(classLoader);
		}
	}
	
	static void unloadJarResources(OSGiJarClassLoader jarClassLoader, File jarFile) throws IOException {
		
		JarFile file = new JarFile(jarFile);
		Enumeration<JarEntry> entries = file.entries();
		while ( entries.hasMoreElements() ) {
			JarEntry entry = entries.nextElement();
			String name = entry.getName();
			if ( ! entry.isDirectory() && name.endsWith(".class" ) ) {
				String className = name.replace("/", ".").replace(".class", "");
				//log.info("Unloading class:" + className);
				try {
					jarClassLoader.unloadClass(className);
				}
				catch ( JclException e ) {
					log.error("Could not unload class: " + className, e);
				}
			}
		}		
	}
	
	private Stack<String> getCallStack(String className) {
		Map<String, Stack<String>> callStackMap = callStackTLS.get();
		if ( callStackMap == null ) {
			callStackMap = new HashMap<String, Stack<String>>();
			callStackTLS.set(callStackMap);
		}
		Stack<String> callStack = callStackMap.get(className);
		if ( callStack == null ) {
			callStack = new Stack<String>();
			callStackMap.put(className, callStack);
		}
		return callStack;
	}
	
	@Override
    public Class loadClass(String className, boolean resolveIt) throws ClassNotFoundException {
		
		log.debug("Loading class:" + className + " via CL:" + this.label);
		
		if ( className.equals("org.springframework.webflow.mvc.servlet.FlowHandlerAdapter") ) {
			log.info("Loading FlowHandlerAdapter via CL: " + this.label + ", hash: " + this.hashCode());
		}
		if ( ! this.getLocalLoader().isEnabled() ) {
			//throw new ClassNotFoundException("Class not found: " + className + ", label: " + this.label);
			log.info("Loading '" + className + " via disabled class loader: " + this.label);
			return super.loadClass(className, resolveIt);
		}
		
		Stack<String> callStack = this.getCallStack(className);
		if ( callStack.contains(this.label) ) { 
			log.debug("[" + this.label + "] Calling stack joined. Returning...");
			throw new ClassNotFoundException("Class '" + className + "' not found");
		}
		callStack.push(this.label);
		log.debug("[" + this.label + "] Current call stack: " + callStack);
		try {
			return super.loadClass(className, resolveIt);
		}
		finally {
			callStack.pop();
		}
	}
	
//	@Override
//	public Class loadClass(String className) throws ClassNotFoundException {
//		
//		// TODO: This should be a proxy loader for this instead
//		for ( OSGiJarClassLoader jarLoader : this.jarClassLoaders ) {
//			try {
//				jarLoader.getParentClassLoader().setEnabled(false);
//				Class clazz = jarLoader.loadClass(className, false);
//				log.info("Loaded class: " + className + " via JAR loader: " + jarLoader.toString());
//				return clazz;
//			}
//			catch ( ClassNotFoundException e ) { /* Ignore - Continue with next loader */ }
//			finally {
//				jarLoader.getParentClassLoader().setEnabled(true);
//			}
//		}
//		
//		return super.loadClass(className);
//		
//	}
	
	@Override
	public InputStream getResourceAsStream(String name) {
		
		return this.parent.getParent().getResourceAsStream(name);
		//return this.parent.loadResource(name);
		
		/*
		if ( ! this.getLocalLoader().isEnabled() ) {
			return super.getResourceAsStream(name);
		}
		
		Stack<String> callStack = this.getCallStack(name);
		if ( callStack.contains(this.label) ) { 
			log.debug("[" + this.label + "] Calling stack joined. Returning...");
			return null;
		}
		callStack.push(this.label);
		log.debug("[" + this.label + "] Current call stack: " + callStack);
		try {
			return super.getResourceAsStream(name);
		}
		finally {
			callStack.pop();
		}
		*/
	}

	@Override
	public URL getResource(String name) {
		return this.parent.getResource(name);	
		
		/*
		if ( ! this.getLocalLoader().isEnabled() ) {
			return super.getResource(name);
		}
		
		Stack<String> callStack = this.getCallStack(name);
		if ( callStack.contains(this.label) ) { 
			log.debug("[" + this.label + "] Calling stack joined. Returning...");
			return null;
		}
		callStack.push(this.label);
		log.debug("[" + this.label + "] Current call stack: " + callStack);
		try {
			return super.getResource(name);
		}
		finally {
			callStack.pop();
		}
		*/
	}
	
	protected URL getLocalResource(String name) {
		byte[] resource = this.getLoadedResources().get(name);
		if ( resource != null ) {
			try { 
				return new URL("http", "bytes@memory/", -1, name, new BytesURLStreamHandler(resource));
			}
			catch ( MalformedURLException e ) {}
		}
		return null;
	}

	/*
	@Override
	public Enumeration<URL> getResources(String name) throws IOException {
		
		if ( ! this.getLocalLoader().isEnabled() ) {
			return super.getResources(name);
		}
		
		Stack<String> callStack = this.getCallStack(name);
		if ( callStack.contains(this.label) ) { 
			log.debug("[" + this.label + "] Calling stack joined. Returning...");
			return null;
		}
		callStack.push(this.label);
		log.debug("[" + this.label + "] Current call stack: " + callStack);
		try {
			return super.getResources(name);
		}
		finally {
			callStack.pop();
		}
	}
	*/
	
	
	@Override
	public Enumeration<URL> getResources(String name) throws IOException {
		
		log.info("[" + this.label + "] Getting resources: " + name);
//		URL url = this.getLocalResource(name);
//		if ( url != null ) {
//			list.add(url);
//		}
		
		ArrayList<URL> list = new ArrayList<URL>();
		if ( this.parent != null ) {
			list.addAll(this.parent.getResources(name));
		}
		
		final Iterator<URL> iterator = list.iterator();

        return new Enumeration<URL>() {
            @Override
            public boolean hasMoreElements() {
                return iterator.hasNext();
            }

            @Override
            public URL nextElement() {
                return iterator.next();
            }
        };
	}
	
	private final void clearReferencesStaticFinal() {
        
        for ( Class<?> clazz : this.getLoadedClasses().values().toArray(new Class<?>[0]) ) {
        	
            try {
                Field[] fields = clazz.getDeclaredFields();
                for (int i = 0; i < fields.length; i++) {
                    if(Modifier.isStatic(fields[i].getModifiers())) {
                        fields[i].get(null);
                        break;
                    }
                }
            } catch(Throwable t) {
                // Ignore
            }
        }
        for ( Class<?> clazz : this.getLoadedClasses().values().toArray(new Class<?>[0]) ) {
        	
            try {
                Field[] fields = clazz.getDeclaredFields();
                for (int i = 0; i < fields.length; i++) {
                    Field field = fields[i];
                    int mods = field.getModifiers();
                    if (field.getType().isPrimitive()
                            || (field.getName().indexOf("$") != -1)) {
                        continue;
                    }
                    if (Modifier.isStatic(mods)) {
                        try {
                            field.setAccessible(true);
                            if (Modifier.isFinal(mods)) {
                                if (!((field.getType().getName().startsWith("java."))
                                        || (field.getType().getName().startsWith("javax.")))) {
                                    nullInstance(field.get(null));
                                }
                            } else {
                                field.set(null, null);
                                if (log.isDebugEnabled()) {
                                    log.debug("Set field " + field.getName()
                                            + " to null in class " + clazz.getName());
                                }
                            }
                        } catch (Throwable t) {
                            ExceptionUtils.handleThrowable(t);
                            if (log.isDebugEnabled()) {
                                log.debug("Could not set field " + field.getName()
                                        + " to null in class " + clazz.getName(), t);
                            }
                        }
                    }
                }
            } 
            catch (Throwable t) {
                ExceptionUtils.handleThrowable(t);
                if (log.isDebugEnabled()) {
                    log.debug("Could not clean fields for class " + clazz.getName(), t);
                }
            }
       
        }

    }

    private void nullInstance(Object instance) {
        if (instance == null) {
            return;
        }
        Field[] fields = instance.getClass().getDeclaredFields();
        for (int i = 0; i < fields.length; i++) {
            Field field = fields[i];
            int mods = field.getModifiers();
            if (field.getType().isPrimitive()
                    || (field.getName().indexOf("$") != -1)) {
                continue;
            }
            try {
                field.setAccessible(true);
                if (Modifier.isStatic(mods) && Modifier.isFinal(mods)) {
                    // Doing something recursively is too risky
                    continue;
                }
                Object value = field.get(instance);
                if (null != value) {
                    Class<? extends Object> valueClass = value.getClass();
                    if ( ! this.getLoadedClasses().containsValue(valueClass) ) {
                        if (log.isDebugEnabled()) {
                            log.debug("Not setting field " + field.getName() +
                                    " to null in object of class " +
                                    instance.getClass().getName() +
                                    " because the referenced object was of type " +
                                    valueClass.getName() +
                                    " which was not loaded by this WebappClassLoader.");
                        }
                    } else {
                        field.set(instance, null);
                        if (log.isDebugEnabled()) {
                            log.debug("Set field " + field.getName()
                                    + " to null in class " + instance.getClass().getName());
                        }
                    }
                }
            } catch (Throwable t) {
                ExceptionUtils.handleThrowable(t);
                if (log.isDebugEnabled()) {
                    log.debug("Could not set field " + field.getName()
                            + " to null in object instance of class "
                            + instance.getClass().getName(), t);
                }
            }
        }
    }
	
	
	@Override
	public String toString() {
		
		String parentStr = null;
		if ( this.parent != null) {
			ClassLoader parent = this.parent.getParent();
			
			if ( parent instanceof OSGiJarClassLoader ) {
				parentStr = ((OSGiJarClassLoader) parent).getLabel();
			}
			else {
				parentStr = parent.getClass().getName();
			}
		}
		
		StringBuilder sb = new StringBuilder();
		if ( ! this.getLocalLoader().isEnabled() ) {
			sb.append("DISABLED ");
		}
		sb.append("OSGiJarClassLoader label: " + this.label + " parent: " + parentStr + " [" + this.hashCode() + "]\n");
		if ( this.jarClassLoaders.size() > 0 ) {
			sb.append("Child class loaders:\n");
			for ( OSGiJarClassLoader childCL : this.jarClassLoaders ) {
				sb.append(childCL);
			}
		}
		return sb.toString();
	}

}
