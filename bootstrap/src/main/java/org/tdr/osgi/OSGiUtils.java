package org.tdr.osgi;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

public abstract class OSGiUtils {

	static public Bundle getBundle(String name, BundleContext bundleContext) {
		Bundle[] bundles = bundleContext.getBundles();
		for ( Bundle bundle : bundles) {
			String bundleName = (String) bundle.getHeaders().get("Bundle-Name");
			if ( bundleName != null && bundleName.equals(name) ) {
				return bundle;
			}
		}
		return null;
	}

}
