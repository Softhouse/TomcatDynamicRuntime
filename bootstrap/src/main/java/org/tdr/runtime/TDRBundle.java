package org.tdr.runtime;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.List;
import java.util.StringTokenizer;

import javax.naming.directory.DirContext;

import org.apache.catalina.LifecycleException;
import org.apache.catalina.deploy.WebXml;
import org.apache.catalina.loader.LoaderManager;
import org.apache.catalina.loader.WebappClassLoader;
import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;
import org.osgi.framework.Bundle;
import org.tdr.bootstrap.OSGiWebAppLoader;
import org.tdr.bootstrap.TLSReference;
import org.tdr.bootstrap.WebXmlParser;
import org.tdr.bootstrap.classloader.OSGiJarClassLoader;

/**
 * TDR Bundle. A wrapper around OSGi bundles that are targeting TDR.
 * 
 * @author nic
 */
public class TDRBundle {

	private static final Log log = LogFactory.getLog(TDRBundle.class);
	
	public static final String TDR_RUNTIME_NAME_HEADER		= "TDR-RUNTIME-NAME";
	public static final String EXPORTED_PATHS_HEADER 		= "TDR-EXPORTED-PATHS";
	public static final String HOST_HEADER					= "TDR-HOST";
	public static final String HOST_PRIO_HEADER				= "TDR-HOST-PRIO";
	public static final String DEPENDS_OF_HEADER			= "TDR-DEPENDS-OF";	
	public static final String RESOURCE_DIRECTORY_HEADER	= "TDR-RESOURCE-DIRECTORY";
	public static final String WEB_XML_FRAGMENT_NAME		= "META-INF/web-fragment.xml";
	public static final String RESOURCE_LIST_FILENAME		= "resource_list.txt";
	
	private Bundle bundle;
	private String basePath;
	private String resourcePath;
	private WebXml webXml;
	private List<File> jarFiles = null;
	
	public static boolean isTDRBundle(Bundle bundle) {
		Dictionary<String,String> headers = bundle.getHeaders();
		return headers.get(EXPORTED_PATHS_HEADER) != null ||
			   headers.get(HOST_HEADER) != null;
	}
	
	public static boolean isTDRBundleFragment(Bundle bundle) {
		Dictionary<String,String> headers = bundle.getHeaders();
		return headers.get(HOST_HEADER) != null;
	}
	
	protected String getHeaderValue(String headerName) {
		Dictionary<String,String> headers = this.bundle.getHeaders();
		return headers.get(headerName);
	}
	
	public TDRBundle(Bundle bundle, String basePath, WebXmlParser webXmlParser) {
		
		// TODO: Add bundle parent
		
		this.bundle = bundle;
		this.basePath = basePath;
		String resourcePathHeader = this.getHeaderValue(RESOURCE_DIRECTORY_HEADER);
		if ( resourcePathHeader != null ) {
			this.resourcePath = System.getProperty("catalina.home") + "/" + resourcePathHeader;
		}
		this.buildWebXml(webXmlParser);	
	}
	
	public void start() {
		// TODO: NEEDED?
	}
	
	public void stop() {
		// TODO: NEEDED?
	}
	
	public boolean isFragmentToRuntime(String runtime) {
				
		Dictionary<String,String> headers = bundle.getHeaders();
		String hostHeader = headers.get(HOST_HEADER);
		if ( hostHeader != null ) {
			StringTokenizer tokenizer = new StringTokenizer(hostHeader, ",");
			while ( tokenizer.hasMoreTokens() ) {
				if ( tokenizer.nextToken().equals(runtime) ) {
					return true;
				}
			}
		}
		return false;
	}
	
	public int getFragmentPrio() {
		Dictionary<String,String> headers = bundle.getHeaders();
		String prioString = headers.get(HOST_PRIO_HEADER);
		if ( prioString != null ) {
			return Integer.parseInt(prioString);
		}
		return 0;
	}
	
	public void addJarsToClassLoader(WebappClassLoader classLoader) {
		this.buildJarList();
		for ( File jar : this.jarFiles ) {
			try {
				log.info("Adding JAR to classloader: " + jar);
				((OSGiWebAppLoader) classLoader).addJar(jar);
				LoaderManager.addJarToWebappClassLoader(classLoader, jar);
			}
			catch ( IOException e ) {}
		}		
	}

	public OSGiJarClassLoader addToClassLoader(OSGiWebAppLoader classLoader) {
		
		this.buildJarList();
		if ( this.hasClasses() || this.jarFiles.size() > 0 ) {
		
			OSGiJarClassLoader bundleCL = classLoader.getJarClassLoader().newChild(this.bundle.getSymbolicName());
			
			if ( this.hasClasses() ) {
				log.info("Adding classes to classloader repository");
				try {
					bundleCL.addClassPath(this.getClassPath()); 
					LoaderManager.addClassPathToWebappClassLoader(classLoader, "/bundle$" + this.bundle.getBundleId() + "/classes/", 
																  new File(this.getClassPath()));
				}
				catch ( IOException e ) {}
			}
			
			for ( File jar : this.jarFiles ) {
				try {
					log.info("Adding JAR to classloader: " + jar);
					bundleCL.addJar(jar);
					LoaderManager.addJarToWebappClassLoader(classLoader, jar);
				}
				catch ( IOException e ) {}
			}
			
			return bundleCL;
		}
		return null;
	}

	public void removeFromClassLoader(OSGiWebAppLoader classLoader) {
		classLoader.getJarClassLoader().removeChild(this.bundle.getSymbolicName());
		
		// TODO: REMOVE RESOURCES FROM MAIN CL HERE ALSO!!!
	}
	
	private boolean hasClasses() {
		return new File(this.getClassPath()).exists();
	}
	
	private void buildJarList() {
		if ( this.jarFiles == null )
		{
			this.jarFiles = new ArrayList<File>();
			File jarPath = new File(this.basePath + "/lib");
			if ( jarPath.exists() ) {
				for ( File jar : jarPath.listFiles() ) {
					this.jarFiles.add(jar);
				}
			}
		}
	}
	
	public long getId() { return this.bundle.getBundleId(); }
	
	public Bundle getBundle() { return this.bundle; }
		
	public void install() throws IOException {
				
		File bundleBaseDir = new File(this.basePath);
		if ( bundleBaseDir.exists() ) {
			deleteDirectory(bundleBaseDir);
		}
		bundleBaseDir.mkdirs();
		
		if ( this.resourcePath != null ) {
			File resourcePathDir = new File(this.resourcePath);
			if ( ! resourcePathDir.exists() ) {
				resourcePathDir.mkdirs();
			}
			this.copyResourcesTo(this.resourcePath);
		}
		else {
			this.copyResourcesTo(this.basePath);
		}
		this.copyLibsTo(this.basePath);
		this.copyClassesTo(this.getClassPath());
		
	}
	
	public void uninstall() throws IOException {
		
		if ( this.resourcePath != null ) {
			File resourceListFile = new File(this.basePath + "/" + RESOURCE_LIST_FILENAME);
			if ( resourceListFile.exists() ) {
				FileInputStream is = new FileInputStream(resourceListFile);
				BufferedReader reader = new BufferedReader(new InputStreamReader(is, Charset.forName("UTF-8")));
				String resource;
				while ((resource = reader.readLine()) != null) {
				    File resourceFile = new File(resource);
				    if ( resourceFile.isDirectory() && resourceFile.listFiles().length == 0 || 
				    	 resourceFile.isFile() ) {
				    	log.info("Removing file: " + resourceFile);
				    	resourceFile.delete();
				    }
				}
				reader.close();
				is.close();	
			}
		}
		
		File bundleBaseDir = new File(this.basePath);
		log.info("Uninstalling bundle " + bundle.getBundleId() + " from: " + bundleBaseDir);
		if ( bundleBaseDir.exists() ) {
			deleteDirectory(bundleBaseDir);
		}
	}
	
	static void deleteDirectory(File directory)
    {           
        File[] files = directory.listFiles();   
        for ( int i=0; i < files.length; i++ ) {
            if ( files[i].isDirectory() ) {
                    deleteDirectory(files[i]);
            }
            else {
            	files[i].delete();
            }
        }
        directory.delete();
    }
	
	private String getClassPath() {
		return this.basePath + "/classes";
	}
	
	public WebXml getWebXml() {
		return this.webXml;
	}
	
	// TODO: Make functions to retrieve resources from bundle repo here...

	public void copyResources(String resourceDirectoryName) throws IOException {
		ResourceDirectory directory = ResourceDirectory.buildDirectories(resourceDirectoryName, this);
		directory.copyTo(this.basePath);
	}
	
	private void copyResourcesTo(String path) throws IOException {
		File resourceListFile = new File(this.basePath + "/" + RESOURCE_LIST_FILENAME);
		resourceListFile.createNewFile();
		FileOutputStream outputStream = new FileOutputStream(resourceListFile);
		BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(outputStream, Charset.forName("UTF-8")));
		try {
			for ( String exportedPath : this.getExportedPaths() ) {
				ResourceDirectory resourceDirectory = ResourceDirectory.buildDirectories(exportedPath, this);
				resourceDirectory.copyTo(path);
				resourceDirectory.writeInfo(path, writer);
			}
		}
		finally {
			writer.close();
			outputStream.close();
		}
	}
	
	private void copyLibsTo(String path) throws IOException {
		ResourceDirectory libDirectory = ResourceDirectory.buildDirectories("lib", this);
		libDirectory.copyTo(path);
	}
	
	private void copyClassesTo(String path) throws IOException {
		Enumeration<URL> classes = (Enumeration<URL>) this.bundle.findEntries("", "*.class", true);
		if ( classes != null ) {
			while ( classes.hasMoreElements() ) {
				URL classUrl = classes.nextElement();
				//log.info("Class URL to copy: " + classUrl.getPath());
				this.readUrlIntoFile(path, classUrl);
			}
		}
	}
	
	// TODO: Refactor and merge with equalent method in ResourceFile
	private void readUrlIntoFile(String path, URL url) throws IOException {
		
		File file = new File(path + url.getPath());
		
		// Verify that full path exists -> if not create it
		//
		File copyToPath = file.getParentFile();
		if ( ! copyToPath.exists() ) {
			copyToPath.mkdirs();
		}
		
		log.info("Copy resource to: " + file);
		
		FileOutputStream fos = new FileOutputStream(file);
		InputStream urlStream = url.openStream();
		byte buf[] = new byte[1028];
		
		while ( true ) { // TODO: There must exist some standard util for this kind of code??
			int len = urlStream.read(buf);
			if ( len > 0 ) {
				fos.write(buf, 0, len);
			}
			if ( len == -1 ) break;
		}
		fos.close();
	}
	
	// TODO: Move this into the webfragment deployer instead!!!
	private void buildWebXml(WebXmlParser webXmlParser) {
		URL resourceUrl = this.bundle.getEntry(WEB_XML_FRAGMENT_NAME);
		if ( resourceUrl != null ) {
			//log.info("Building web.xml...");
			this.webXml = webXmlParser.parseWebXmlFragment(resourceUrl);
		}
	}
	
	public String getResourcePath() {
		return this.resourcePath != null ? this.resourcePath : this.basePath;
	}
	
	public File getResource(String path) {	
		if ( ! path.startsWith("/lib") ) {
			File resource = new File(this.getResourcePath() + path);
			if ( resource.exists() ) {
				//log.info("Returning bundle resource: " + resource);
				return resource;
			}
		}
		return null;
	}
	
	public List<String> getExportedPaths() {
		return this.getHeaderValueList(EXPORTED_PATHS_HEADER);
	}
	
	private List<String> getHeaderValueList(String headerName) {
		// TODO: Improve the parsing to allow whitespaces
		List<String> valueList = new ArrayList<String>();
		String valuesStr = (String) this.bundle.getHeaders().get(headerName);
		if ( valuesStr != null ) {
			StringTokenizer tokenizer = new StringTokenizer(valuesStr, ",");
			while ( tokenizer.hasMoreTokens() ) {
				valueList.add(tokenizer.nextToken());
			}
		}
		return valueList;
	}
	
	@Override
	public String toString() {
		return "TDRBundle [name=" + bundle.getSymbolicName() + "]";
	}
	
}
