package org.tdr.runtime;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.jar.Manifest;

import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;
import org.tdr.bootstrap.OSGiContext;
import org.tdr.bootstrap.OSGiServletContext;
import org.tdr.bootstrap.OSGiWebAppLoader;
import org.tdr.bootstrap.TLSReference;
import org.tdr.bootstrap.classloader.OSGiJarClassLoader;

/**
 * Runtime Application.
 * 
 * @author nic
 */
public class RuntimeApplication {

	private static final Log log = LogFactory.getLog(RuntimeApplication.class);
	
	public static final String RUNTIMES_HEADER = "TDR-Runtimes";
	
	public static final String CURRENT_BUNDLE_CLASS_LOADER_TLS = "TDRCurrentBundleClassLoader";
	
	// TODO: Use BundleMetadata for this?? Version=null for any version?
	// Version Range class? version="[1.2.0,1.2.9]"
	
	// Bygga lager av properties så att man kan göra override? Då måste vi göra en extension på servlet context oxå...
	
	// TODO: Ta hänsyn till absolute ordering???	

	
	/**
	 * Införa ett TDR-Host header som talar om för bundles att kunna bli deployade i en runtime?
	 * Då kommer alla GUI komponenter, taglibs etc bli automatisk deployade.
	 * 
	 * -> Liknande standard OSGi fragment
	 * 
	 * TDR-Host: JSF Runtime
	 * 
	 * Blir JSF Runtime avdeployad så åker TDR Bundle fragments bort oxå...
	 */
	
	// TODO: Lägg till stöd för TDR-NoBundleCache
	
	private static class Runtime {
		String name;
		String version;
		
		Runtime(String name, String version) { this.name = name; this.version = version; }
	}
	
	private ArrayList<Runtime> wantedRuntimes = new ArrayList<Runtime>();
	private ArrayList<TDRBundle> currentRuntimes = null;
	private Map<TDRBundle, List<TDRBundle>> fragments = null;
	private TDRBundleRepository bundleRepository;
	private boolean hasBeenRefreshed = false;
	private OSGiContext osgiContext;
	private OSGiServletContext servletContext;
	private RuntimeDeployerRegistry runtimeDeployerRegistry;
	private ArrayList<TDRBundle> pendingUndeployedBundles = new ArrayList<TDRBundle>();
	private Map<TDRBundle, List<TDRBundle>> bundleDependencies = new HashMap<TDRBundle, List<TDRBundle>>();
	
	/*
	 * Future extension:
	 * 
	 * TDR-Dependent-Of: Bundle1
	 * 
	 * TDR-Bundle
	 * 
	 * 
	 */
	
	public static boolean isRuntimeManifest(Manifest manifest) {
		return manifest.getMainAttributes().getValue(RUNTIMES_HEADER) != null;
	}
	
	/**
	 * Constructor
	 * @param applicationBundle
	 * @param bundleRepository
	 */
	public RuntimeApplication(Manifest manifest, TDRBundleRepository bundleRepository) {
		this.bundleRepository = bundleRepository;

		String runtimesStr = manifest.getMainAttributes().getValue(RUNTIMES_HEADER);
		
		// TODO: Add support for version tag
		// TODO: Should not the search order be reverse to make it possible to override files etc?
		
		StringTokenizer tokenizer = new StringTokenizer(runtimesStr, ",");
		while ( tokenizer.hasMoreTokens() ) {
			this.wantedRuntimes.add(new Runtime(tokenizer.nextToken(), "0.0"));
		}
		
		// TEMP FIX
		this.runtimeDeployerRegistry = new RuntimeDeployerRegistry(null);
		this.runtimeDeployerRegistry.setInstance();
	}
	
	/**
	 * Start runtime application and make its resources available to current webapp.
	 */
	public void start() {
		this.refreshRuntimes();
		this.bundleRepository.registerRuntimeApplication(this);
	}
	
	/**
	 * Stop runtime application
	 */
	public void stop() {
		this.bundleRepository.unregisterRuntimeApplication(this);
		// TODO: Undeploy runtime fragements here...???
	}
	
	public ClassLoader getClassLoader() {
		
		return null;
		
		/*
		ClassLoader bundleClassLoader = (ClassLoader) TLSReference.get(CURRENT_BUNDLE_CLASS_LOADER_TLS);
		if ( bundleClassLoader == null ) {
			bundleClassLoader = this.startClassLoader;
		}
		return bundleClassLoader;
		*/
	}
	
	/**
	 * Initialize context
	 * @param osgiContext
	 */
	public void initializeContext(OSGiContext osgiContext) {
			
		this.osgiContext = osgiContext; // TODO: Behövs OSGi Context här???
		this.servletContext = this.osgiContext.getOSGiServletContext();
		this.runtimeDeployerRegistry = new RuntimeDeployerRegistry(this.servletContext);
		this.servletContext.setAttribute(RuntimeDeployerRegistry.CONTEXT_NAME, this.runtimeDeployerRegistry);
		this.runtimeDeployerRegistry.addDeployer(new WebFragmentDeployer(servletContext));

		
		// Build "classloader snake", i.e. first
		// iterate through all runtimes and build a class loader chain
		// and iterate through all fragments per runtime and hook those in
		// the same class loader chain. The last deployed fragment is the first
		// class loader that will be used in the chain.
		//
		TDRBundle lastBundle = null;
		TDRBundle previousBundle = null;
		for ( TDRBundle bundle : this.currentRuntimes ) {
			this.doDeployBundle(bundle, previousBundle, null);
			previousBundle = bundle;
			lastBundle = bundle;
		}
		for ( TDRBundle bundle : this.currentRuntimes ) {
			for ( TDRBundle fragment : this.fragments.get(bundle) ) {
				log.info("Deploying fragment: " + fragment);
				this.doDeployBundle(fragment, previousBundle, null);
				previousBundle = fragment;
				lastBundle = fragment;
			}			
		}
		
		// TODO: How to inject new stuff in this chain????
		// TODO: We need to have a wrapper CL to be able inject new parent or??
		
		// TODO: How to handle dependencies?? TO BE IMPL!!
		
		
		// TODO: The start class loader must be maintained. It should be the
//		if ( lastBundle != null ) {
//			this.startClassLoader = this.classLoaders.get(lastBundle);
//			log.info("Start class loader: " + this.startClassLoader);
//		}
		
	
	}
	
	/**
	 * Destroy context
	 */
	public void destroyContext() {
		
		// Undeploy the "classloader snake" in reverse order
		//
		for ( int i=this.currentRuntimes.size()-1; i >= 0; i-- ) {
			TDRBundle bundle = this.currentRuntimes.get(i);
			List<TDRBundle> fragments = this.fragments.get(bundle);
			for ( int j=fragments.size()-1; j >= 0; j-- ) {
				TDRBundle fragment = fragments.get(j);
				this.undeployBundle(fragment);
			}
			this.undeployBundle(bundle);
		}
		this.servletContext = null;
		this.osgiContext = null;
	}
	
	/**
	 * Refresh runtimes
	 */
	protected void refreshRuntimes() {
			
		// TODO: Add support for requires here also??
		
		// TODO: Here should the refresh towards the virtual dir context also be done
		
		// TODO: All CL needs to ordered in a chain, not a tree
		
		/*
		 * Runtime ->
		 * 
		 * TDR-Runtime-Name: JSF <- För att signalera att det är en runtime
		 * TDR-Require-Runtime: Spring
		 * 
		 * För Spring som sig själv ska inte behöva JSF, så de borde vara oberoende
		 * Det är mer för t.ex. Webflow RT som kräver Spring för att bli deployad...
		 * Det är frågan om de olika runtime deployers håller reda på detta själv?
		 * Genom att man har 2 nya metoder på RuntimeDeployer:
		 * - Collection<Bundle> getDeployedBundles()
		 * - redeployBundle(Collection<Bundle> bundles)
		 * 
		 * Men det löser inte class loader problematiken...
		 * 
		 * -> 
		 * 
		 * Fragment ->
		 * TDR-Host: JSF1.2,Spring <- Denna kombination krävs för att kunna köra fragmentet
		 * 
		 * 
		 * Sedan kommer alla fragment få en klassladdare som har alla runtimes i sig
		 * 
		 * 
		 */		
		
		ArrayList<TDRBundle> runtimeList = new ArrayList<TDRBundle>();
		HashMap<TDRBundle,List<TDRBundle>> fragmentsMap = new HashMap<TDRBundle, List<TDRBundle>>();
		for ( Runtime runtime : this.wantedRuntimes ) {
			log.info("Checking runtime: " + runtime.name);
			TDRBundle bundle = bundleRepository.getBundle(runtime.name);
			if ( bundle != null ) {
				runtimeList.add(bundle);	
				fragmentsMap.put(bundle, this.bundleRepository.getBundleFragments(runtime.name));
				
				/**
				 * Här kommer samma fragment förekomma flera gånger. Är ju helt ok. 
				 * Man får ha någon kod som gör undeploy av fragments ifall alla dess runtimes är borta
				 * Men ifall de kräver att 2 runtimes behövs för att de ska starta då??
				 */
			}
		}
		this.currentRuntimes = runtimeList;
		this.fragments = fragmentsMap;
		this.hasBeenRefreshed = true;
	}
	
	/**
	 * @return if runtime application has been refreshed (i.e. bundles has been redeployed, started or stopped).
	 */
	public boolean hasBeenRefreshed() {
		// TODO: Make a better solution for this
		if ( this.hasBeenRefreshed ) {
			this.hasBeenRefreshed = false;
			return true;
		}
		return false;
	}
	
	/**
	 * Check if a specific TDR bundle is active in current runtime application
	 * @param bundle
	 * @return active state
	 */
	public boolean isActiveInRuntime(TDRBundle bundle) {
		if ( this.currentRuntimes.contains(bundle) ) {
			return true;
		}
		for ( List<TDRBundle> bundleList : this.fragments.values() ) {
			if ( bundleList.contains(bundle) ) {
				return true;
			}
		}
		return false;
	}
		
	protected TDRBundle getParentBundle(TDRBundle bundle) {
		
		TDRBundle parent = null;
		for ( TDRBundle runtimeBundle : this.currentRuntimes ) {
			if ( runtimeBundle == bundle ) return parent;
			parent = runtimeBundle;
		}
		for ( TDRBundle runtimeBundle : this.currentRuntimes ) {
			for ( TDRBundle fragment : this.fragments.get(runtimeBundle) ) {
				if ( fragment == bundle ) return parent;
				parent = fragment;
			}
		}
		return null;
	}
	
	protected TDRBundle getChildBundle(TDRBundle bundle) {
		boolean foundBundle = false;
		for ( TDRBundle runtimeBundle : this.currentRuntimes ) {
			if ( foundBundle) return runtimeBundle;
			if ( runtimeBundle == bundle ) {
				foundBundle = true;
				continue;
			}
		}
		for ( TDRBundle runtimeBundle : this.currentRuntimes ) {
			for ( TDRBundle fragment : this.fragments.get(runtimeBundle) ) {
				if ( foundBundle) return fragment;
				if ( fragment == bundle ) {
					foundBundle = true;
					continue;
				}
			}
		}
		return null;
	}
	
	protected synchronized void addDependendency(TDRBundle from, TDRBundle to) {
		List<TDRBundle> list = this.bundleDependencies.get(from);
		if ( list == null ) {
			list = new ArrayList<TDRBundle>();
			this.bundleDependencies.put(from, list);
		}
		list.add(to);
	}
	
	protected synchronized void removeDependency(TDRBundle from, TDRBundle to) {
		List<TDRBundle> list = this.bundleDependencies.get(from);
		if ( list != null ) {
			list.remove(to);
		}
	}
	
	protected void deployDependencies(TDRBundle bundle) {
		
		log.info("Deploying dependencies for bundle: " + bundle.getBundle().getSymbolicName());
		
		boolean foundBundle = false;
		for ( TDRBundle runtimeBundle : this.currentRuntimes ) {
			if ( runtimeBundle == bundle ) {
				foundBundle = true;
				continue;
			}
			else if ( foundBundle ) {
				log.info("Deploying dependency: " + runtimeBundle.getBundle().getSymbolicName());
				this.doDeployBundle(runtimeBundle); // TODO: OPTIMIZE!!!
			}
		}
		for ( TDRBundle runtimeBundle : this.currentRuntimes ) {
			for ( TDRBundle fragment : this.fragments.get(runtimeBundle) ) {
				if ( fragment == bundle ) {
					foundBundle = true;
					continue;
				}
				else if ( foundBundle ) {
					log.info("Deploying dependency: " + fragment.getBundle().getSymbolicName());
					this.doDeployBundle(fragment);
				}
			}
		}
	}
	
	protected void undeployDependencies(TDRBundle bundle) {
		
		log.info("Undeploying dependencies for bundle: " + bundle.getBundle().getSymbolicName());
		
		boolean foundBundle = false;
		ArrayList<TDRBundle> bundleList = new ArrayList<TDRBundle>();
		for ( TDRBundle runtimeBundle : this.currentRuntimes ) {
			if ( runtimeBundle == bundle ) {
				foundBundle = true;
				continue;
			}
			else if ( foundBundle ) {
				bundleList.add(runtimeBundle);
			}
		}
		for ( TDRBundle runtimeBundle : this.currentRuntimes ) {
			for ( TDRBundle fragment : this.fragments.get(runtimeBundle) ) {
				if ( fragment == bundle ) {
					foundBundle = true;
					continue;
				}
				else if ( foundBundle ) {
					bundleList.add(fragment);
				}
			}
		}
		
		for ( int i=bundleList.size()-1; i >= 0; i-- ) {
			log.info("Undeploying bundle #" + i);
			this.doUndeployBundle(bundleList.get(i));
		}
	}
	
	public void deployBundle(TDRBundle bundle) {
		this.doDeployBundle(bundle);
		this.deployDependencies(bundle);
	}
	
	public void doDeployBundle(TDRBundle bundle) {
		
		this.doDeployBundle(bundle, this.getParentBundle(bundle), null); // this.getChildBundle(bundle));
	}
	
	/***
	 * Deploy bundle with a specific parent.
	 * @param bundle
	 * @param parent
	 */
	public void doDeployBundle(TDRBundle bundle, TDRBundle parent, TDRBundle child) {
						
		if ( this.isActiveInRuntime(bundle) ) {		
				
			log.info("Deploying TDR bundle: " + bundle.getBundle().getSymbolicName());
					
			OSGiJarClassLoader bundleCL = bundle.addToClassLoader((OSGiWebAppLoader) this.servletContext.getClassLoader());
			
			this.runtimeDeployerRegistry.setCurrentBundle(bundle);
			this.runtimeDeployerRegistry.setInstance();
			ClassLoader currentContextCL = Thread.currentThread().getContextClassLoader();
			//Thread.currentThread().setContextClassLoader(this.servletContext.getClassLoader());	
			Thread.currentThread().setContextClassLoader(bundleCL);	
			TLSReference.set("TDRServletContextCL", this.servletContext.getClassLoader());			
			TLSReference.set(CURRENT_BUNDLE_CLASS_LOADER_TLS, bundleCL);
			
			try {
				int deployersSize = this.runtimeDeployerRegistry.getDeployers().size();
				for ( RuntimeDeployer deployer : this.runtimeDeployerRegistry.getDeployers() ) {
					log.info("Invoking deployer: " + deployer.getClass());
					boolean deployed = deployer.deploy(bundle);
					if ( deployed ) {
						this.addDependendency(this.runtimeDeployerRegistry.getDeployerOwner(deployer), bundle);
					}
				}
				int newDeployersSize = this.runtimeDeployerRegistry.getDeployers().size();
				if ( newDeployersSize > deployersSize) {
					// New deployer(s) added -> Invoke it
					// TODO: Is this not recursive? 
					for ( int i=newDeployersSize-(newDeployersSize-deployersSize); i < newDeployersSize; i++ ) {
						RuntimeDeployer deployer = this.runtimeDeployerRegistry.getDeployers().get(i);
						log.info("Invoking deployer: " + deployer.getClass());
						deployer.deploy(bundle);
						log.info("Invoking deployer on child bundles...");
						TLSReference.clear(CURRENT_BUNDLE_CLASS_LOADER_TLS);
						//this.invokeDeployerOnChildBundles(bundle, deployer);
					}
				}
				
			}
			finally {
				Thread.currentThread().setContextClassLoader(currentContextCL);
				this.runtimeDeployerRegistry.clearCurrentBundle();
				this.runtimeDeployerRegistry.clearInstance();
				TLSReference.clear(CURRENT_BUNDLE_CLASS_LOADER_TLS);
				TLSReference.clear("TDRServletContextCL");
			}
			
		}
	}
	
	public void undeployBundle(TDRBundle bundle) {
		this.undeployDependencies(bundle);
		this.doUndeployBundle(bundle);
	}
	
	/**
	 * Undeploy web fragment for a specific bundle
	 * @param bundle
	 */
	public void doUndeployBundle(TDRBundle bundle) {
					
		if ( this.isActiveInRuntime(bundle) ) {		
			
			log.info("Undeploying bundle: " + bundle.getBundle().getSymbolicName());
						
			// Trigger undeploy on all dependencies
			//
			//log.info("Undeploying dependencies...");
			/*
			List<TDRBundle> dependencies = this.bundleDependencies.get(bundle);
			if ( dependencies != null ) {
				for ( TDRBundle dependency : dependencies ) {
					log.info("Undeploying dependency: " + dependency.getBundle().getSymbolicName());
					this.undeployBundle(dependency);
				}
			}
			this.bundleDependencies.remove(bundle);
			*/
			
			/*
			// Go through all deployers owned by this bundle
			//
			for ( RuntimeDeployer deployer : this.runtimeDeployerRegistry.getDeployersOwnedBy(bundle).toArray(new RuntimeDeployer[] {}) ) {
				
				if ( deployer != null && deployer.getDeployedBundles() != null ) {
					for ( TDRBundle deployedBundle : deployer.getDeployedBundles().toArray(new TDRBundle[] {})) {
						if ( ! this.pendingUndeployedBundles.contains(deployedBundle) ) {
							log.info("Undeploying bundle from deployer: " + deployedBundle.getBundle().getSymbolicName());
							deployer.undeploy(deployedBundle);
							//this.undeployBundle(deployedBundle);
						}
					}
				}
				if ( deployer != null ) {
					deployer.shutdown();
				}
			}
			*/
			
			for ( RuntimeDeployer deployer : this.runtimeDeployerRegistry.getDeployersInReverseOrder() ) {
				log.info("Undeploying bundle using deployer: " + deployer.getClass());
				deployer.undeploy(bundle);
			}	
			this.pendingUndeployedBundles.remove(bundle); // TODO: In a finally statement??	
			this.runtimeDeployerRegistry.removeDeployersOwnedBy(bundle);
						
			bundle.removeFromClassLoader((OSGiWebAppLoader) this.servletContext.getClassLoader());
		
		}
	
	}
	
	public String getResourcePaths() {
		
		// TODO: Change order of the paths
		
		ArrayList<String> resourcePathList = new ArrayList<String>();
		this.hasBeenRefreshed = false; // TODO: Make this fully access safe
		StringBuilder resourcePaths = new StringBuilder();
		for ( TDRBundle runtimeBundle : this.currentRuntimes ) {
			resourcePaths.append(",/=");
			String resourcePath = runtimeBundle.getResourcePath();
			if ( ! resourcePathList.contains(resourcePath) ) {
				resourcePaths.append(runtimeBundle.getResourcePath());
				for ( TDRBundle fragment : this.fragments.get(runtimeBundle) ) {
					resourcePaths.append(",/=");
					resourcePaths.append(fragment.getResourcePath());
				}
				resourcePathList.add(resourcePath);
			}
		}
		log.info("Resource paths: " + resourcePaths);
		return resourcePaths.toString();
	}
	
	/**
	 * Get a resource from a runtime bundle. The order specified in the manifest is used.
	 * @param path
	 * @return resource if found, otherwise null
	 */
	public File getResource(String path) {
				
		// TODO: Map WEB-INF/lib request to read from [bundle]/lib ???
		
		for ( TDRBundle runtimeBundle : this.currentRuntimes ) {
			File resource = runtimeBundle.getResource(path);
			if ( resource != null ) return resource;
			for ( TDRBundle fragment : this.fragments.get(runtimeBundle) ) {
				resource = fragment.getResource(path);
				if ( resource != null ) return resource;
			}
		}
		return null;
	}
	
	public File getBundleResource(String bundleDirectPath) {
				
		if ( ! bundleDirectPath.startsWith("/bundle$") ) {
			return null;
		}
		StringTokenizer tokenizer = new StringTokenizer(bundleDirectPath, "/");
		String bundlePrefix = tokenizer.nextToken();
		int bundleId = Integer.parseInt(bundlePrefix.substring("bundle$".length()));
		
		for ( TDRBundle runtimeBundle : this.currentRuntimes ) {
			if ( runtimeBundle.getId() == bundleId ) {
				return runtimeBundle.getResource(bundleDirectPath.replace("/" + bundlePrefix, ""));
			}
			for ( TDRBundle fragment : this.fragments.get(runtimeBundle) ) {
				if ( fragment.getId() == bundleId ) {
					return fragment.getResource(bundleDirectPath.replace("/" + bundlePrefix, ""));
				}
			}
		}
		return null;
	}

}
