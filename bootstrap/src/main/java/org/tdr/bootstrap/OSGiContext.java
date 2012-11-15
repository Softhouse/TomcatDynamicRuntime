package org.tdr.bootstrap;

import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;

import org.apache.catalina.Globals;
import org.apache.catalina.core.ApplicationContext;
import org.apache.catalina.core.ApplicationContextFacade;
import org.apache.catalina.core.StandardContext;
import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;

/**
 * OSGi Context, which is an extended Tomcat Standard Context.
 * 
 * @author nic
 *
 */
public class OSGiContext extends StandardContext {

	private static final Log log = LogFactory.getLog(OSGiContext.class);
	
	public OSGiContext() {
		log.info("Constructing an OSGi context...");
	}
	
	/* (non-Javadoc)
	 * @see org.apache.catalina.core.StandardContext#getServletContext()
	 */
	@Override
	public ServletContext getServletContext() {
		return this.getOSGiServletContext().getFacadeObject();
	}
	
	/**
	 * Get a reference to an extended OSGi servlet context with extended support for web application management.
	 * 
	 * @return osgi servlet context
	 */
	public OSGiServletContext getOSGiServletContext() {
		if (context == null) {
			context = new OSGiServletContext(this);
        }
        return ((OSGiServletContext) context);
	}
	
	@Override
    public FilterConfig findFilterConfig(String name) {

    	FilterConfig filterConfig = this.getOSGiServletContext().getFilterConfig(name);
    	if ( filterConfig == null ) {
    		filterConfig = super.findFilterConfig(name);
    	}
    	return filterConfig;
    }
	
}
