package org.tdr.spring.runtime;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.support.GenericXmlApplicationContext;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.tdr.runtime.RuntimeDeployer;
import org.tdr.runtime.RuntimeDeployerRegistry;
import org.tdr.runtime.TDRBundle;

/**
 * Spring Deployer
 * 
 * @author nic
 */
public class SpringDeployer implements RuntimeDeployer {

	private static final Log log = LogFactory.getLog(SpringDeployer.class);
		
	private VirtualWebApplicationContext virtualAppContext;
	private Map<TDRBundle, GenericXmlApplicationContext> bundleApplicationContexts = new HashMap<TDRBundle, GenericXmlApplicationContext>();
	
	public SpringDeployer(VirtualWebApplicationContext virtualAppContext) {
		log.info("Starting Spring Deployer...");
		this.virtualAppContext = virtualAppContext;
		RuntimeDeployerRegistry.instance().addDeployer(this);
	}
	
	/* (non-Javadoc)
	 * @see org.tdr.runtime.RuntimeDeployer#deploy(org.tdr.runtime.TDRBundle)
	 */
	@Override
	public boolean deploy(TDRBundle bundle) {
		URL url = bundle.getBundle().getEntry("META-INF/spring-context.xml");
		if ( url != null ) {
			log.info("Deploying spring-context.xml...");
			
			try {
				XmlApplicationContextFragment bundleContext = new XmlApplicationContextFragment(this.virtualAppContext.getServletContext());
				bundleContext.setValidating(false); // TODO: Do I really need to do this?
				Resource contextResource = new InputStreamResource(url.openStream());
				bundleContext.load(contextResource);
				bundleContext.refresh();
				bundleContext.start();
				this.virtualAppContext.addApplicationContext(bundleContext);
				this.bundleApplicationContexts.put(bundle, bundleContext);
				log.info("Spring context deployed.");
				return true;
			}
			catch ( IOException e ) {
				log.error("Could not deploy spring context.", e);
			}
		}
		return false;
	}

	/* (non-Javadoc)
	 * @see org.tdr.runtime.RuntimeDeployer#undeploy(org.tdr.runtime.TDRBundle)
	 */
	@Override
	public void undeploy(TDRBundle bundle) {
		GenericXmlApplicationContext bundleContext = this.bundleApplicationContexts.get(bundle);
		if ( bundleContext != null ) {
			log.info("Undeploying beans from bundle context...");
			this.virtualAppContext.removeApplicationContext(bundleContext);
			bundleContext.stop();
			bundleContext.close();
			this.bundleApplicationContexts.remove(bundle);
		}
	}

	/* (non-Javadoc)
	 * @see org.tdr.runtime.RuntimeDeployer#getDeployedBundles()
	 */
	@Override
	public List<TDRBundle> getDeployedBundles() {
		ArrayList<TDRBundle> list = new ArrayList<TDRBundle>();
		list.addAll(this.bundleApplicationContexts.keySet());
		return list;
	}

	@Override
	public void shutdown() {		
	}

}
