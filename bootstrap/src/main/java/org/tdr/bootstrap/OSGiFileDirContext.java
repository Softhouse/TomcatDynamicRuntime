package org.tdr.bootstrap;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

import javax.naming.NamingException;
import javax.servlet.ServletContext;

import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;
import org.apache.naming.NamingEntry;
import org.apache.naming.resources.Resource;
import org.apache.naming.resources.VirtualDirContext;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.tdr.osgi.OSGiUtils;
import org.tdr.runtime.RuntimeApplication;
import org.tdr.runtime.TDRBundleRepository;

public class OSGiFileDirContext extends VirtualDirContext {

	static private Log log = LogFactory.getLog(OSGiFileDirContext.class);
	
	private BundleContext bundleContext;
	private TDRBundleRepository bundleRepository;
	private RuntimeApplication runtimeApplication;
	private String baseResourcePaths = "";
	private String tomcatHome = System.getProperty("catalina.home");
	
	/**
	 * Constructor.
	 */
	public OSGiFileDirContext() {
		super();
		
		log.info("Creating OSGi directory context...");
		this.bundleContext = this.getBundleContext();
		this.bundleRepository = this.getBundleRepository();
		TLSReference.set("osgi/dirContext", this);
		
		
		// TODO: Fetch the runtimes that are associated with the webapp
		// TODO: Make a TDR-Runtime setting to define if an external content path(s) should be used
		// -> Or just use vanilla virtual dir context config for that???
		
		// Mergning av web.xml osv??? Eller ska vi lyfta fram servlets & filter a'la whiteboard pattern???
		
		
	}
	
	public void setBaseResourcePaths(String path)
    {
        this.baseResourcePaths = path;
    }
	
	// TODO: Consider to use allocate & release instead??
	public void allocate() {
		Manifest manifest = this.getManifest();
		if ( manifest != null && RuntimeApplication.isRuntimeManifest(manifest) ) {
			log.info("Starting runtime application...");
			this.runtimeApplication = new RuntimeApplication(manifest, this.bundleRepository);
			this.runtimeApplication.start();
			//this.reallocateResourcePaths();
			this.setExtraResourcePaths(this.baseResourcePaths + this.runtimeApplication.getResourcePaths());
		}
		super.allocate();
	}
	protected void reallocateResourcePaths() {
		super.release();
		this.setExtraResourcePaths(this.baseResourcePaths + this.runtimeApplication.getResourcePaths());
		super.allocate();
	}
	
	public void release() {
		super.release();
		if ( this.runtimeApplication != null ) {
			this.runtimeApplication.stop();
		}
	}
	
	public RuntimeApplication getRuntimeApplication() {
		return this.runtimeApplication;
	}
	
	// TODO: Let this class generate the bundle info?
	// TODO: How to handle undeploy of a webapp then?
	
	// TODO: Have absolute paths to the JAR resources??? Or is it just JAR path?
	// Or just call Bundle getResource?
	// When using Bundle loadClass it will use Bundle infrastructure which might conflict with Tomcat env...
	// -> We probably need to have own JAR classloaders here...
	
	private BundleContext getBundleContext() {
		return (BundleContext) GlobalReference.get("osgi/bundleContext");
	}
	
	private TDRBundleRepository getBundleRepository() {
		return (TDRBundleRepository) GlobalReference.get("tdr/bundleRepository");
	}
	
	@Override
	protected String doGetRealPath(String path) {
	
		if ( path.startsWith(this.tomcatHome) ) {
			return path;
		}
		String realPath = super.doGetRealPath(path);
		if ( realPath == null || ! new File(realPath).exists() ) {
			// If the file does not exists in the webapp -> check in the different runtimes
			//
			File runtimeFile = this.runtimeApplication.getResource(path);
			if ( runtimeFile != null ) {
				realPath = runtimeFile.getAbsolutePath();
			}
		}
		log.info("Real path: " + realPath);
		return realPath;
	}
	
	@Override
	protected File file(String name) {
		
		// TEMP DEBUGGING...
		if ( name.endsWith(".xhtml") ) {
			log.info("Accessing file: " + name);
		}
		
		if ( this.runtimeApplication != null && this.runtimeApplication.hasBeenRefreshed() ) {
			this.reallocateResourcePaths();
		}
		if ( name.startsWith("/bundle$") && this.runtimeApplication != null ) {
			return this.runtimeApplication.getBundleResource(name);
		}
		return super.file(name);
	}
	
	protected File getBundleResourceAsFile(String resourceName, Bundle bundle) {
		
		URL url = bundle.getEntry(resourceName);
		
		if ( url == null ) {
			return null;
		}
		try {
			String bundleFile = System.getProperty("catalina.home") + "/work/bundle_files/" + bundle.getBundleId() + "_" + resourceName.hashCode();
			FileOutputStream fos = new FileOutputStream(bundleFile);
			InputStream urlStream = url.openStream();
			byte buf[] = new byte[1024];
			
			while ( true ) { // TODO: There must exist some standard util for this kind of code??
				int len = urlStream.read(buf);
				if ( len > 0 ) {
					fos.write(buf, 0, len);
				}
				if ( len == -1 ) break;
			}
				
			return new File(bundleFile);
		}
		catch ( IOException e ) {
			log.error("Could not read bundle file '" + resourceName + "' from bundle with ID=" + bundle.getBundleId(), e);
			return null;
		}
		
	}
	
	// TODO: Divide into several functions!!
	// TODO: Move to RuntimeApplication class???
	
	private Manifest getManifest() {
		try {
			Resource resource = (Resource) super.lookup("META-INF/MANIFEST.MF");
			Manifest manifest = new Manifest(resource.streamContent());
			log.info("Manifest attributes: " + manifest.getMainAttributes().keySet());
			log.info("TDR Runtimes: " + manifest.getMainAttributes().getValue("TDR-Runtimes"));
			return manifest;
		}
		catch ( NamingException | IOException e ) {
			log.error("No manifest found. Ignoring webapp.", e);
			return null;
		}
	}
	
	/**
	 * Create and deploy (install/start) the application bundle. It is a wrapper for the current webapp to access
	 * bundle provided classes and services.
	 * @return application bundle
	 * @throws IOException
	 * @throws BundleException
	 */
	public Bundle createAndDeployApplicationBundle() throws IOException, BundleException {

		Bundle bundle = null;
		
		// Create a wrapper bundle JAR and copy the webapp MANIFEST into it
		//
		Resource resource;
		try {
			resource = (Resource) super.lookup("META-INF/MANIFEST.MF"); // TODO: Is this too early to lookup resources here?? 
		}
		catch ( NamingException e ) {
			log.error("No manifest found. Ignoring webapp.", e);
			return null;
		}
		Manifest bundleManifest = new Manifest(resource.streamContent());
		Attributes attributes = bundleManifest.getMainAttributes();
		String bundleName = attributes.getValue("Bundle-Name"); // TODO: Use constant here!!!
		
		log.info("Bundle name: " + bundleName);
		
		// TODO: REFACTOR!!!
		if ( bundleName != null ) {
		
			Bundle currentBundle = OSGiUtils.getBundle(bundleName, this.bundleContext);
			if ( currentBundle != null ) { 
				log.info("Uninstalling already defined bundle: " + currentBundle);
				currentBundle.uninstall();
				// TODO: Improve the go through all headers. Are they identical, so skip do uninstall and just keep the bundle
				// Otherwise will the bundle identities grow...
			}
			File bundleFile = File.createTempFile("tdr_" + bundleName.replaceAll(" ", ""), ".jar");
			FileOutputStream fos = new FileOutputStream(bundleFile);
			JarOutputStream out = new JarOutputStream(fos, bundleManifest);
			out.close();
			fos.close();
			
			log.info("Created JAR file: " + bundleFile);
			
			bundle = this.getBundleContext().installBundle(bundleFile.toURI().toString());
			bundle.start();
			
			log.info("Installed bundle in OSGi runtime: " + bundle);
			
		}
		return bundle;
	}
}
