package org.tdr.runtime;

import java.util.ArrayList;
import java.util.EventListener;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.Filter;
import javax.servlet.FilterRegistration;
import javax.servlet.ServletContext;
import javax.servlet.ServletRegistration;

import org.apache.catalina.core.ApplicationFilterConfig;
import org.apache.catalina.deploy.FilterDef;
import org.apache.catalina.deploy.FilterMap;
import org.apache.catalina.deploy.ServletDef;
import org.apache.catalina.deploy.WebXml;
import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;
import org.tdr.bootstrap.OSGiServletContext;

/**
 * Deployer for web fragments, i.e. JEE servlets, filters and listeners
 * 
 * @author nic
 *
 */
public class WebFragmentDeployer implements RuntimeDeployer {
	
	private static final Log log = LogFactory.getLog(WebFragmentDeployer.class);
	
	private OSGiServletContext servletContext;
	private Map<TDRBundle, List<EventListener>> deployedListeners = new HashMap<TDRBundle, List<EventListener>>();
	private ArrayList<TDRBundle> deployedBundles = new ArrayList<TDRBundle>();
	
	public WebFragmentDeployer(OSGiServletContext servletContext) {
		this.servletContext = servletContext;
	}
	
	/* (non-Javadoc)
	 * @see org.tdr.runtime.RuntimeDeployer#deploy(org.tdr.runtime.TDRBundle)
	 */
	@Override
	public boolean deploy(TDRBundle bundle) {		
		WebXml webFragment = bundle.getWebXml(); // TODO: Move the web XML code into the deployer insteaad!!
		if ( webFragment != null ) {
			log.info("Deploying from web-fragment.xml...");
			//log.info(webFragment.toXml());
			this.deployContextParams(bundle);
			this.deployListeners(bundle);
			this.deployServlets(bundle);
			this.deployFilters(bundle);
			this.deployedBundles.add(bundle);
			return true;
		}		
		return false;
	}

	/* (non-Javadoc)
	 * @see org.tdr.runtime.RuntimeDeployer#undeploy(org.tdr.runtime.TDRBundle)
	 */
	@Override
	public void undeploy(TDRBundle bundle) {		
		WebXml webFragment = bundle.getWebXml();
		if ( webFragment != null ) {
			log.info("Undeploying from web-fragment.xml...");
			this.undeployFilters(bundle);
			this.undeployServlets(bundle);
			this.undeployListeners(bundle);
			this.undeployContextParams(bundle);
			this.deployedBundles.remove(bundle);
		}		
	}
	
	@Override
	public List<TDRBundle> getDeployedBundles() {
		return this.deployedBundles;
	}
	
	protected void deployContextParams(TDRBundle bundle) {
		WebXml webFragment = bundle.getWebXml();
		Map<String, String> contextParams = webFragment.getContextParams();
		for ( String paramName : contextParams.keySet() ) {
			log.info("Defining context param: " + paramName);
			this.servletContext.setInitParameter(paramName, contextParams.get(paramName));
		}
	}
	
	protected void undeployContextParams(TDRBundle bundle) {
		WebXml webFragment = bundle.getWebXml();
		Map<String, String> contextParams = webFragment.getContextParams();
		for ( String paramName : contextParams.keySet() ) {
			this.servletContext.clearInitParameter(paramName);
		}
	}
		
	protected void deployListeners(TDRBundle bundle) {
		
		WebXml webFragment = bundle.getWebXml();
		ArrayList<EventListener> listenerList = new ArrayList<EventListener>();
		this.deployedListeners.put(bundle, listenerList);
		for ( String listenerClassName : webFragment.getListeners() ) {
			log.info("Creating listener of class: " + listenerClassName);
			try {
				log.info("Class loader for loading listener:\n" + this.servletContext.getClassLoader());
				Class<EventListener> listenerClass =  (Class<EventListener>) this.servletContext.getClassLoader().loadClass(listenerClassName);
				log.info("Class hash: " + listenerClass.hashCode());
				EventListener listener = this.servletContext.createListener(listenerClass);
				this.servletContext.addListener(listener);
				listenerList.add(listener);
			}
			catch ( Exception e ) {
				log.error("Exception while deploying listener.", e);
			}
		}	
	}
	
	protected void undeployListeners(TDRBundle bundle) {
		
		List<EventListener> listenerList = this.deployedListeners.get(bundle);
		if ( listenerList != null ) {
			for ( EventListener listener : listenerList ) {
				this.servletContext.removeListener(listener);
			}
			this.deployedListeners.remove(bundle);
		}
	}
	
	protected void deployServlets(TDRBundle bundle) {
		
		WebXml webFragment = bundle.getWebXml();
		Map<String, ServletDef> servlets = webFragment.getServlets();
		for ( String servletName : servlets.keySet() ) {
			log.info("Deploying servlet: " + servletName);
			ServletDef servletDef = servlets.get(servletName);
			log.info("Servlet class: " + servletDef.getServletClass() + ", hash: " + servletDef.getServletClass().hashCode());
			try {
				ServletRegistration.Dynamic servlet = this.servletContext.addServlet(servletName, servletDef.getServletClass());
				for ( String urlPattern : webFragment.getServletMappings().keySet() ) {
					String mappedServletName = webFragment.getServletMappings().get(urlPattern);
					if ( mappedServletName.equals(servletName) ) {
						log.info("Servlet URL mapping: " + urlPattern);
						servlet.addMapping(urlPattern);
					}
				}
				if ( servletDef.getLoadOnStartup() != null ) {
					servlet.setLoadOnStartup(servletDef.getLoadOnStartup());				
					if ( servletDef.getLoadOnStartup() == 1 ) {
						log.info("Starting servlet: " + servletName);
						this.servletContext.startServlet(servletName);
					}
				}
			}
			catch ( Exception e ) {
				log.error("Exception while deploying servlet.", e);
			}
		}
	}
	
	protected void undeployServlets(TDRBundle bundle) {
		
		WebXml webFragment = bundle.getWebXml();
		for ( String servletName : webFragment.getServlets().keySet() ) {
			log.info("Undeploying servlet: " + servletName);
			this.servletContext.removeServlet(servletName);
		}
	}
	
	protected void deployFilters(TDRBundle bundle) {
		
		WebXml webFragment = bundle.getWebXml();
		Map<String, FilterDef> filterDefinitions = webFragment.getFilters();
		Set<FilterMap> filterMappings = webFragment.getFilterMappings();
		
		for ( String filterName : filterDefinitions.keySet() ) {
			try { 
				log.info("Deploying filter: " + filterName);
				FilterDef filterDef = filterDefinitions.get(filterName);
				FilterRegistration.Dynamic filterReg = this.servletContext.addFilter(filterName, filterDef.getFilterClass());
				for ( FilterMap filterMapping : filterMappings) {
					if ( filterMapping.getFilterName().equals(filterName) ) {
						// TODO: Implement support for dispatcher mappings from filterMapping.getDispatcherMapping()
						filterReg.addMappingForUrlPatterns(null, false, filterMapping.getURLPatterns());
					}
				}
				log.info("Starting filter...");
				this.servletContext.startFilter(filterReg);
			}
			catch ( Exception e ) {
				log.error("Error while deploying filter: " + filterName, e);
			}
		}
	}
	
	protected void undeployFilters(TDRBundle bundle) {
		WebXml webFragment = bundle.getWebXml();
		for ( String filterName : webFragment.getFilters().keySet() ) {
			this.servletContext.removeFilter(filterName);
		}
		
		// TODO: Call destroy here!!
	}

	/* (non-Javadoc)
	 * @see org.tdr.runtime.RuntimeDeployer#shutdown()
	 */
	@Override
	public void shutdown() {		
	}
	
}
