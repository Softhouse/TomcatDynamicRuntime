package org.tdr.runtime;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleListener;
import org.tdr.bootstrap.WebXmlParser;
import org.tdr.osgi.BundleMetadata;
import org.tdr.osgi.BundleVersion;
import org.tdr.osgi.OSGiUtils;

/**
 * TDR Bundle Repository
 * 
 * Global repository which all webapps are using to fetch bundle resources.
 * 
 * @author nic
 *
 */
public final class TDRBundleRepository implements BundleListener {
	
	private static final Log log = LogFactory.getLog(TDRBundleRepository.class);
	
	private String basePath = System.getProperty("catalina.home") + "/tdr_bundle_repository";
	
	private Map<Long, TDRBundle> bundles = new HashMap<Long, TDRBundle>();
	private Map<BundleMetadata, TDRBundle> bundleMetadata = new HashMap<BundleMetadata, TDRBundle>();
	
	private List<RuntimeApplication> runtimeApplications = new ArrayList<RuntimeApplication>();
	private WebXmlParser webXmlParser = new WebXmlParser();
	
	// TODO: This is temporary fix. We need to have dependency chains instead. Because taglib should not be started until common is available
	static class FragmentBundleComparator implements Comparator<TDRBundle> {

		@Override
		public int compare(TDRBundle o1, TDRBundle o2) {
			return o1.getFragmentPrio()-o2.getFragmentPrio();
		}
		
	}
	
	public TDRBundleRepository(BundleContext bundleContext) {
		bundleContext.addBundleListener(this);
		
		for ( Bundle bundle : bundleContext.getBundles() ) {
			if ( bundle.getState() == Bundle.ACTIVE && 
				 ( TDRBundle.isTDRBundle(bundle) ) ) {
//				TDRBundle tdrBundle = this.addBundle(bundle);
//				tdrBundle.start();
				this.installBundle(bundle, true);
			}
		}
	}
	
	public void shutdown() {
		for ( TDRBundle bundle : bundles.values() ) {
			bundle.stop();
		}
	}
	
	public synchronized void registerRuntimeApplication(RuntimeApplication application) {
		this.runtimeApplications.add(application);
	}
	
	public synchronized void unregisterRuntimeApplication(RuntimeApplication application) {
		this.runtimeApplications.remove(application);
	}
	
	private TDRBundle addBundle(Bundle bundle) {

		File bundleBaseDir = new File(this.getBundleBaseDir(bundle));
		log.info("Creating a new TDR bundle of bundle: " + bundle.getBundleId());
		TDRBundle tdrBundle = new TDRBundle(bundle, bundleBaseDir.getPath(), this.webXmlParser);
		this.bundles.put(bundle.getBundleId(), tdrBundle);
		this.bundleMetadata.put(new BundleMetadata(bundle), tdrBundle);	
		return tdrBundle;
	}
	
	
	private TDRBundle installBundle(Bundle bundle) {
		return this.installBundle(bundle, false);
	}
	
	private TDRBundle installBundle(Bundle bundle, boolean atServerStartup) {
		log.info("Installing bundle: " + bundle.getBundleId());
		TDRBundle tdrBundle = this.addBundle(bundle);
		try {
			if ( !atServerStartup || 
				 ( atServerStartup && tdrBundle.isModified() ) ) {
				tdrBundle.install();
			}
			tdrBundle.start();
			return tdrBundle;
		}
		catch ( IOException e ) {
			log.error("Could not install TDR bundle.", e);
			this.uninstallBundle(bundle);
			return null;
		}
	}
	
	private void uninstallBundle(Bundle bundle) {
		TDRBundle tdrBundle = this.bundles.get(bundle.getBundleId());
		if ( tdrBundle != null ) {
			try {
				tdrBundle.stop();
				tdrBundle.uninstall();
			}
			catch ( IOException e ) {
				log.error("Could no uninstall TDR bundle.", e);
			}
		}
		this.bundles.remove(bundle.getBundleId());
		this.bundleMetadata.remove(new BundleMetadata(bundle));
	}
	
	

	
	private String getBundleBaseDir(Bundle bundle) {
		return basePath + "/" + bundle.getBundleId();
	}
	
	public TDRBundle getBundle(long bundleId) {
		return this.bundles.get(bundleId);
	}
	
	public TDRBundle getBundle(Bundle bundle) {
		return this.bundles.get(bundle.getBundleId());
	}
	
	public TDRBundle getBundle(String name) {
		
		BundleVersion highestVersionFound = null;
		TDRBundle bundle = null;
		for ( BundleMetadata bundleMetadata : this.bundleMetadata.keySet() ) {
			if ( bundleMetadata.getName().equals(name) ) {
				if ( ( bundle != null && bundleMetadata.getVersion().compareTo(highestVersionFound) > 0 ) || 
					 bundle == null ) { 
					bundle = this.bundleMetadata.get(bundleMetadata);
					highestVersionFound = bundleMetadata.getVersion();
				}
			}
		}
		return bundle;
	}
	
	public TDRBundle getBundle(String name, String version) {
		// TODO: IMPLEMENT!!!
		return null;
	}
	
	public List<TDRBundle> getBundleFragments(String runtimeName) {
		ArrayList<TDRBundle> fragments = new ArrayList<TDRBundle>();
		for ( TDRBundle bundle : this.bundles.values() ) {
			if ( bundle.isFragmentToRuntime(runtimeName) ) {
				fragments.add(bundle);
			}
		}
		Collections.sort(fragments, new FragmentBundleComparator());
		log.info("Fragments for runtime '" + runtimeName + "' : " + fragments);
		return fragments;
	}

	@Override
	public void bundleChanged(BundleEvent bundleEvent) {
		if ( TDRBundle.isTDRBundle(bundleEvent.getBundle() ) ) {
			if ( bundleEvent.getType() == BundleEvent.STARTED ) {
				TDRBundle tdrBundle = this.installBundle(bundleEvent.getBundle());
				this.refreshRuntimeApplications();
				this.deployBundleInRuntimes(tdrBundle);
			}
			else if ( bundleEvent.getType() == BundleEvent.STOPPED ) {
				TDRBundle tdrBundle = this.getBundle(bundleEvent.getBundle());
				this.undeployBundleInRuntimes(tdrBundle);
				this.uninstallBundle(bundleEvent.getBundle());
				this.refreshRuntimeApplications(); // TODO: This could cause 404 if requests arrive while uninstalling bundle
			}	
			// TODO: Possibility to detect redeploy? För då slipper vi riva hela kedjor av strukturer
			// Delayed stop to wait for a start to show up?
		}
	}
	
	private void refreshRuntimeApplications() {
		for ( RuntimeApplication application : this.runtimeApplications ) {
			application.refreshRuntimes();
		}
	}
	
	private void deployBundleInRuntimes(TDRBundle bundle) {
		for ( RuntimeApplication application : this.runtimeApplications ) {
			 application.deployBundle(bundle);
		}
	}
	
	private void undeployBundleInRuntimes(TDRBundle bundle) {
		for ( RuntimeApplication application : this.runtimeApplications ) {
			application.undeployBundle(bundle);
		}
	}
}
