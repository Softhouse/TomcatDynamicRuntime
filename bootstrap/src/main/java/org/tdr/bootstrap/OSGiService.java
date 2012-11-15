package org.tdr.bootstrap;

import java.io.File;

import org.apache.catalina.LifecycleException;
import org.apache.catalina.core.StandardEngine;
import org.apache.catalina.core.StandardService;
import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;
import org.apache.karaf.main.Main;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.FrameworkListener;
import org.tdr.runtime.TDRBundleRepository;

public class OSGiService extends StandardService implements FrameworkListener {
	
	private static final Log log = LogFactory.getLog(OSGiService.class);
	
	private Main main;
	private String karafPath;
	private long startupTimeout = 60000; // Default 60 sec timeout
	private TDRBundleRepository bundleRepository;
	
	public void setKarafPath(String karafPath) {
		this.karafPath = karafPath;
	}
	
	public void setStartupTimeout(long timeout) {
		this.startupTimeout = timeout;
	}
	
	
	@Override
	protected void startInternal() throws LifecycleException {
				
		log.info("Starting up OSGi kernel. All further logging will be available via OSGi log service.");
		
		// TODO: Use ${tomcat.home}/osgi where karaf dist is placed
		
		String root = new File(karafPath).getAbsolutePath();
		log.info("Root: " + root);
		System.setProperty("karaf.home", root);
        System.setProperty("karaf.base", root);
        System.setProperty("karaf.data", root + "/data");
        System.setProperty("karaf.history", root + "/data/history.txt");
        System.setProperty("karaf.instances", root + "/instances");
		System.setProperty("karaf.startLocalConsole", "false"); // TODO: Use true as development option??
		System.setProperty("karaf.startRemoteShell", "true");
        System.setProperty("karaf.lock", "false");
        
		main = new Main(new String[0]);
		try {
			main.launch();
		}
		catch (Exception e ) {
			log.error("Could not startup Karaf OSGi kernel.", e);
			throw new LifecycleException(e);
		}
        final BundleContext bundleContext = main.getFramework().getBundleContext();
        GlobalReference.set("osgi/bundleContext", bundleContext);
        bundleContext.addFrameworkListener(this);
        this.waitForStartupComplete();
        
        this.bundleRepository = new TDRBundleRepository(bundleContext);
        GlobalReference.set("tdr/bundleRepository", this.bundleRepository);
        
        log.info("OSGi kernel started");
        bundleContext.removeFrameworkListener(this);
        super.startInternal();
        
        this.setContainer(new StandardEngine()); // Just to get rid of exceptions from ThreadLocalLeakPreventionListener
	}

	@Override
	protected void stopInternal() throws LifecycleException {
		log.info("Stopping OSGi kernel...");
		
		try {
			main.destroy();
		}
		catch ( Exception e ) {
			log.error("Error while stopping OSGi kernel:" + e);
		}
		super.stopInternal();
	}
	
	synchronized void waitForStartupComplete() {
		try {
			this.wait(this.startupTimeout);
		}
		catch ( InterruptedException e ) {}
	}
	
	synchronized void signalStartupComplete() {
		this.notify();
	}

	@Override
	public void frameworkEvent(FrameworkEvent frameworkEvent) {
		if ( frameworkEvent.getType() == FrameworkEvent.STARTLEVEL_CHANGED ) {
			this.signalStartupComplete();
		}
	}

}
