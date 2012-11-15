package org.tdr.spring.runtime;

import javax.servlet.ServletContextEvent;

import org.springframework.web.context.ContextLoaderListener;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.XmlWebApplicationContext;

public class DynamicContextLoaderListener extends ContextLoaderListener {

	private SpringDeployer springDeployer;
	
	@Override
	public void contextInitialized(ServletContextEvent event) {
		super.contextInitialized(event);
		
		// Fetch created base context and wrap that into a "virtual" context that
		// delegates all getBean() and similar requests to the different deployed context
		//

		XmlWebApplicationContext appContext = 
				(XmlWebApplicationContext) event.getServletContext().getAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE);
		
		VirtualWebApplicationContext virtualAppContext = new VirtualWebApplicationContext(appContext);
		this.springDeployer = new SpringDeployer(virtualAppContext);
		
		event.getServletContext().setAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE, virtualAppContext);
		
	}

}
