import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.apache.catalina.loader.LoaderManager;
import org.apache.naming.resources.FileDirContext;
import org.tdr.bootstrap.classloader.OSGiJarClassLoader;
import org.tdr.bootstrap.classloader.ParentClassLoader;
import org.xeustechnologies.jcl.JarClassLoader;
import org.xeustechnologies.jcl.exception.JclException;


public class TestClassLoader2 {

	// TODO: Clean up hardcoded URLs & refactor into a number of JUnit tests
	
	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		
		// Skippa hierarkier och istället använd en class loader per JAR som ligger på samma nivå...
		
		// JSF RT
		File jsfApi = new File("/Users/nic/Develop/projects/osgi-poc/TomcatDynamicRuntime/jsf-runtime/src/main/resources/lib/jsf-api-1.2_12.jar");
		File jsfLib = new File("/Users/nic/Develop/projects/osgi-poc/TomcatDynamicRuntime/jsf-runtime/src/main/resources/lib/jsf-impl-1.2_12.jar");
		String jsfClassPath = "/Users/nic/Develop/projects/osgi-poc/TomcatDynamicRuntime/jsf-runtime/bin";
		
		// Spring RT
		File sfCoreLib = new File("/Users/nic/Develop/projects/osgi-poc/TomcatDynamicRuntime/spring-runtime/src/main/resources/lib/spring-core-3.0.7.RELEASE.jar");
		File sfBeansLib = new File("/Users/nic/Develop/projects/osgi-poc/TomcatDynamicRuntime/spring-runtime/src/main/resources/lib/spring-beans-3.0.7.RELEASE.jar");
		File sfContextLib = new File("/Users/nic/Develop/projects/osgi-poc/TomcatDynamicRuntime/spring-runtime/src/main/resources/lib/spring-context-3.0.7.RELEASE.jar");
		File sfWebLib = new File("/Users/nic/Develop/projects/osgi-poc/TomcatDynamicRuntime/spring-runtime/src/main/resources/lib/spring-web-3.0.7.RELEASE.jar");
		
		// WebFlow RT
		//
		File wfFaces = new File("/Users/nic/Develop/projects/osgi-poc/TPP2/webflow-runtime/src/main/resources/lib/spring-faces-2.1.1.RELEASE.jar");
		File wfWebflow = new File("/Users/nic/Develop/projects/osgi-poc/TPP2/webflow-runtime/src/main/resources/lib/spring-webflow-2.1.1.RELEASE.jar");
		File commonsLogging = new File("/Users/nic/Develop/projects/osgi-poc/DynamicRuntime/apache-tomcat-7.0.29/lib/commons-logging-1.1.1.jar");
		String wfClassPath = "/Users/nic/Develop/projects/osgi-poc/TPP2/webflow-runtime/bin";
		
		String className = "com.sun.faces.application.ApplicationFactoryImpl";
		String className2 = "javax.faces.application.ApplicationFactory";
		
		
		OSGiJarClassLoader jarCL = new OSGiJarClassLoader("TEST", null);
		OSGiJarClassLoader moduleCL = jarCL.newChild("JSF Runtime1");

		moduleCL.addJar(jsfApi);
		
		OSGiJarClassLoader moduleCL2 = jarCL.newChild("JSF Runtime2");
		moduleCL2.addJar(jsfLib);
		
		
//		Class class2 = jarCL.getJarLoader(jsfLib.getName()).loadClass(className);
//		System.out.println("Direct loaded class: " + class2);
//	
		
		Class clazz = jarCL.loadClass(className);
		
		ClassLoader classLoader = clazz.getClassLoader();
		
		classLoader.loadClass(className);
		
		System.out.println("Loaded class: " + clazz + ", hash: " + clazz.hashCode());
				
		// TODO: Test to keep reference to old class loader and try to load a class via that one... 
		// -> It should be able to relocate the new CL hierarchy instead of falling back on parent CL...

		jarCL.removeChild("JSF Runtime2");
		moduleCL2 = jarCL.newChild("JSF Runtime2");
//		jarCL.removeJar(jsfLib);
//		jarCL.removeJar(jsfApi);

		moduleCL.addClassPath(jsfClassPath);
		moduleCL2.addJar(jsfLib);
		//moduleCL.addJar(jsfApi);

		System.out.println("Class loader: \n" + jarCL);
		
		clazz = jarCL.loadClass(className);
		System.out.println("Loaded class: " + clazz + ", hash: " + clazz.hashCode());
		
		jarCL.loadClass(className2);
		
		System.out.println("Trying using an old class loader reference: \n" + classLoader);
		Class clazz2 = classLoader.loadClass(className2);
		
		System.out.println("Loaded class: " + clazz2 + ", hash: " + clazz2.hashCode());
		
		Class clazz3 = jarCL.loadClass(className2);
		clazz3 = jarCL.loadClass(className2);
		
		System.out.println("Loaded class: " + clazz3 + ", hash: " + clazz3.hashCode());
		
		
		OSGiJarClassLoader springCL = jarCL.newChild("Spring FW");
		springCL.addJar(sfCoreLib);
		springCL.addJar(sfBeansLib);
		springCL.addJar(sfContextLib);
		springCL.addJar(sfWebLib);
		
		OSGiJarClassLoader webflowCL = jarCL.newChild("WebFlow");
		webflowCL.addClassPath(wfClassPath);
		webflowCL.addJar(wfFaces);
		webflowCL.addJar(wfWebflow);
		webflowCL.addJar(commonsLogging);
		
		
		
		Class reqCtxClass = jarCL.loadClass("org.springframework.webflow.execution.RequestContext");
		Class reqCtxHolderClass = jarCL.loadClass("org.springframework.webflow.execution.RequestContextHolder");
		
		
		Class mockReqCtxClass = jarCL.loadClass("org.springframework.webflow.test.MockRequestContext");
		Object reqCtx = mockReqCtxClass.newInstance();

		Method setReqCtxMethod = reqCtxHolderClass.getMethod("setRequestContext", reqCtxClass);
		setReqCtxMethod.invoke(null, reqCtx);
		Method getReqCtxMethod = reqCtxHolderClass.getMethod("getRequestContext");

		System.out.println("Request Context: " + getReqCtxMethod.invoke(null));
		
		
		jarCL.removeChild("WebFlow");
		webflowCL = jarCL.newChild("WebFlow");
		webflowCL.addClassPath(wfClassPath);
		webflowCL.addJar(wfFaces);
		webflowCL.addJar(wfWebflow);
		webflowCL.addJar(commonsLogging);
		
		reqCtxHolderClass = jarCL.loadClass("org.springframework.webflow.execution.RequestContextHolder");
		getReqCtxMethod = reqCtxHolderClass.getMethod("getRequestContext");
		
		System.out.println("Request Context: " + getReqCtxMethod.invoke(null));
		
		// TODO: Cachad JSF träd som spökar ????
		
	}
	

}
