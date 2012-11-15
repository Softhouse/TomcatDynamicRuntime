package org.tdr.bootstrap;

import org.apache.catalina.startup.HostConfig;
import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;

public class OSGiHostConfig extends HostConfig {

	private static final Log log = LogFactory.getLog(OSGiHostConfig.class);
			
	public OSGiHostConfig() {
		log.info("Starting up OSGi host config...");
		super.setContextClass("org.tdr.bootstrap.OSGiContext");
	}
}
