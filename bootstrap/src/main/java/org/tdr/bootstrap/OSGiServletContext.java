package org.tdr.bootstrap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.EventListener;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.Filter;
import javax.servlet.FilterConfig;
import javax.servlet.FilterRegistration;
import javax.servlet.Servlet;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextAttributeListener;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletException;
import javax.servlet.ServletRegistration;
import javax.servlet.ServletRequestAttributeListener;
import javax.servlet.ServletRequestListener;
import javax.servlet.http.HttpSessionAttributeListener;
import javax.servlet.http.HttpSessionListener;

import org.apache.catalina.LifecycleState;
import org.apache.catalina.Wrapper;
import org.apache.catalina.core.ApplicationContext;
import org.apache.catalina.core.ApplicationContextFacade;
import org.apache.catalina.core.ApplicationFilterConfig;
import org.apache.catalina.core.ApplicationFilterRegistration;
import org.apache.catalina.core.CatalinaManager;
import org.apache.catalina.deploy.FilterDef;
import org.apache.catalina.deploy.FilterMap;
import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;
import org.apache.tomcat.util.ExceptionUtils;

/**
 * OSGi Servlet Context.
 * 
 * @author nic
 */
public class OSGiServletContext extends ApplicationContext {

	static private Log log = LogFactory.getLog(OSGiServletContext.class);
	
	private OSGiContext osgiContext;
	
    /**
     * The merged context initialization parameters for this Context.
     */
    private Map<String,String> parameters = new ConcurrentHashMap<String,String>();

	/**
     * The facade around this object.
     */
    private ServletContext facade = new ApplicationContextFacade(this);
    
    private Map<String, ApplicationFilterConfig> filterConfigs = new HashMap<String, ApplicationFilterConfig>();
    
    // TODO: Keep the dynamic filter defs together with the static ones???
    
	public OSGiServletContext(OSGiContext context) {
		super(context);
		this.osgiContext = context;
	}
	
	 /**
     * Return the facade associated with this ApplicationContext.
     */
    ServletContext getFacadeObject() {

        return (this.facade);
    }
    
    FilterConfig getFilterConfig(String filterName) {
    	return this.filterConfigs.get(filterName);
    }
    
    /***************************************************************************************************************
     *         		INIT PARAMETER MANAGEMENT
     ***************************************************************************************************************/
    
    /**
     * Return the value of the specified initialization parameter, or
     * <code>null</code> if this parameter does not exist.
     *
     * @param name Name of the initialization parameter to retrieve
     */
    @Override
    public String getInitParameter(final String name) {
        return parameters.get(name);
    }

    /**
     * Return the names of the context's initialization parameters, or an
     * empty enumeration if the context has no initialization parameters.
     */
    @Override
    public Enumeration<String> getInitParameterNames() {
        return Collections.enumeration(parameters.keySet());
    }
    
    @Override
    public boolean setInitParameter(String name, String value) {
        if (parameters.containsKey(name)) {
            return false;
        }
        
        parameters.put(name, value);
        return true;
    }
    
    public void clearInitParameter(String name) {
    	parameters.remove(name);
    }
    
    /***************************************************************************************************************
     *         		FILTER MANAGEMENT
     ***************************************************************************************************************/
	
	public void startFilter(FilterRegistration.Dynamic filter) {
		FilterDef filterDef = this.osgiContext.findFilterDef(filter.getName());
		try {
			ApplicationFilterConfig filterConfig = CatalinaManager.createFilterConfig(this.osgiContext, filterDef);
			this.filterConfigs.put(filter.getName(), filterConfig);
		} catch (Throwable t) {
	        t = ExceptionUtils.unwrapInvocationTargetException(t);
	        ExceptionUtils.handleThrowable(t);
	        log.error("Could not start filter: " + filter.getName());
	        // TODO: Rethrow this exception or not???
	    }
	}
	
	public void removeFilter(String filterName) {
		ApplicationFilterConfig filterConfig = filterConfigs.get(filterName);
		if ( filterConfig != null ) {
			CatalinaManager.releaseFilterConfig(filterConfig);
		}
		FilterDef filterDef = this.osgiContext.findFilterDef(filterName);
		this.osgiContext.removeFilterDef(filterDef);
		for ( FilterMap filterMap : this.osgiContext.findFilterMaps() ) {
			if ( filterMap.getFilterName().equals(filterName) ) {
				this.osgiContext.removeFilterMap(filterMap);
			}
		}
	}	
	
   @Override
    public FilterRegistration.Dynamic addFilter(String filterName,
            String filterClass) throws IllegalStateException {     
        return addFilter(filterName, filterClass, null);
    }

    @Override
    public FilterRegistration.Dynamic addFilter(String filterName,
            Filter filter) throws IllegalStateException {
        return addFilter(filterName, null, filter);
    }

    @Override
    public FilterRegistration.Dynamic addFilter(String filterName,
            Class<? extends Filter> filterClass) throws IllegalStateException {
        return addFilter(filterName, filterClass.getName(), null);
    }
	
	protected FilterRegistration.Dynamic addFilter(String filterName, String filterClass, Filter filter) throws IllegalStateException {
       
        FilterDef filterDef = this.osgiContext.findFilterDef(filterName);
        
        // Assume a 'complete' FilterRegistration is one that has a class and
        // a name
        if (filterDef == null) {
            filterDef = new FilterDef();
            filterDef.setFilterName(filterName);
            this.osgiContext.addFilterDef(filterDef);
        } 
        else {
            if (filterDef.getFilterName() != null && filterDef.getFilterClass() != null) {
                return null;
            }
        }

        if (filter == null) {
            filterDef.setFilterClass(filterClass);
           
//           try {
//        	   filter = this.createFilter((Class<Filter>) Thread.currentThread().getContextClassLoader().loadClass(filterClass));
//        	   filterDef.setFilter(filter);
//           }
//           catch ( Exception e ) {
//        	   log.error("Could not create filter. ", e);
//           }
            
        }
        else {
            filterDef.setFilterClass(filter.getClass().getName());
            filterDef.setFilter(filter);
        }
        
        return new ApplicationFilterRegistration(filterDef, this.osgiContext);
    } 

    /***************************************************************************************************************
     *         		SERVLET MANAGEMENT
     ***************************************************************************************************************/

    @Override
    public ServletRegistration.Dynamic addServlet(String servletName,
            String servletClass) throws IllegalStateException {
        
        return addServlet(servletName, servletClass, null);
    }

    @Override
    public ServletRegistration.Dynamic addServlet(String servletName,
            Servlet servlet) throws IllegalStateException {

        return addServlet(servletName, null, servlet);
    }

    @Override
    public ServletRegistration.Dynamic addServlet(String servletName,
            Class<? extends Servlet> servletClass)
    throws IllegalStateException {

        return addServlet(servletName, servletClass.getName(), null);
    }

    protected ServletRegistration.Dynamic addServlet(String servletName,
            String servletClass, Servlet servlet) throws IllegalStateException {
        
        Wrapper wrapper = (Wrapper) this.osgiContext.findChild(servletName);
        
        // Assume a 'complete' ServletRegistration is one that has a class and
        // a name
        if (wrapper == null) {
            wrapper = this.osgiContext.createWrapper();
            wrapper.setName(servletName);
            this.osgiContext.addChild(wrapper);
        } else {
            if (wrapper.getName() != null &&
                    wrapper.getServletClass() != null) {
                if (wrapper.isOverridable()) {
                    wrapper.setOverridable(false);
                } else {
                    return null;
                }
            }
        }

        if (servlet == null) {
            wrapper.setServletClass(servletClass);
        } else {
            wrapper.setServletClass(servlet.getClass().getName());
            wrapper.setServlet(servlet);
        }

        return this.osgiContext.dynamicServletAdded(wrapper);
    }
    
    public void startServlet(String servletName) {
    	Wrapper servlet = (Wrapper) this.osgiContext.findChild(servletName);
    	if ( servlet != null ) {
    		ClassLoader currentContextCL = Thread.currentThread().getContextClassLoader();
    		log.info("Using following CL to load servlet:\n" + currentContextCL);
			//Thread.currentThread().setContextClassLoader(this.getClassLoader()); // TODO: WHAT TO DO HERE???
	    	try {
	            servlet.load();
	        } 
	    	catch (ServletException e) {
	    		log.error("Could not load servlet: " + servletName, e);
	    		// TODO: Rethrow exception here??
	        }
	    	finally {
	    		//Thread.currentThread().setContextClassLoader(currentContextCL);
	    	}
    	}
    }
    
    public void removeServlet(String servletName) {
    	Wrapper servlet = (Wrapper) this.osgiContext.findChild(servletName);
    	if ( servlet != null ) {
    		log.info("Removing servlet: " + servlet);
    		
    		String[] urlPatterns = this.osgiContext.findServletMappings();
            for (String urlPattern : urlPatterns) {
                String name = osgiContext.findServletMapping(urlPattern);
                if (name.equals(servletName)) {
                    this.osgiContext.removeServletMapping(urlPattern);
                }
            }
    		
    		ClassLoader currentContextCL = Thread.currentThread().getContextClassLoader();
			//Thread.currentThread().setContextClassLoader(this.getClassLoader());
			try {
				this.osgiContext.removeChild(servlet);
			}
			finally {
				//Thread.currentThread().setContextClassLoader(currentContextCL);
			}
    	}
    	// TODO: Throw exception if not found?
    }
     
    @Override
    public <T extends EventListener> void addListener(T t) {
             
    	// TODO: Stoppa in en proxy lyssnare istället som delegerar ut event???
    	
        boolean match = false;
        if (t instanceof ServletContextAttributeListener ||
                t instanceof ServletRequestListener ||
                t instanceof ServletRequestAttributeListener ||
                t instanceof HttpSessionAttributeListener) {
            this.osgiContext.addApplicationEventListener(t);
            match = true;
        }
        
        if (t instanceof HttpSessionListener
                || (t instanceof ServletContextListener )) {
            this.osgiContext.addApplicationLifecycleListener(t);
            match = true;
        }
        
        if ( ! match) {
        	throw new IllegalArgumentException("Invalid listener type: " + t.getClass().getName());
        }
              
        if ( t instanceof ServletContextListener ) {
        	ServletContextEvent event = new ServletContextEvent(this.getFacade());
        	ClassLoader currentContextCL = Thread.currentThread().getContextClassLoader();
			//Thread.currentThread().setContextClassLoader(this.getClassLoader());	// TODO: WHAT TO DO HERE???
        	try {
        		this.osgiContext.fireContainerEvent("beforeContextInitialized", t);
        		((ServletContextListener) t).contextInitialized(event);
        		this.osgiContext.fireContainerEvent("afterContextInitialized", t);
        	}
        	catch ( Throwable e ) {
        		log.error("Error while adding listener.", e);
        		// TODO: Rethrow this exception?
        	}
        	finally {
        		//Thread.currentThread().setContextClassLoader(currentContextCL);
        	}
        }
    }
    
    public <T extends EventListener> void removeListener(T t) {
        	
    	if ( t instanceof ServletContextListener ) {
    		ServletContextEvent event = new ServletContextEvent(this.getFacade());
    		ClassLoader currentContextCL = Thread.currentThread().getContextClassLoader();
			//Thread.currentThread().setContextClassLoader(this.getClassLoader());	
			try {
	    		this.osgiContext.fireContainerEvent("beforeContextDestroyed", t);
	            ((ServletContextListener) t).contextDestroyed(event);
	            this.osgiContext.fireContainerEvent("afterContextDestroyed", t);
			}
			catch ( Throwable e ) {
				log.error("Error while removing listener.", e);
			}
			finally {
        		//Thread.currentThread().setContextClassLoader(currentContextCL);
        	}
    	}
    	
    	if (t instanceof ServletContextAttributeListener ||
                t instanceof ServletRequestListener ||
                t instanceof ServletRequestAttributeListener ||
                t instanceof HttpSessionAttributeListener) {
    		
    		Object[] currentList = this.osgiContext.getApplicationEventListeners();
    		ArrayList<Object> newList = new ArrayList<Object>();
    		for ( Object listener : currentList ) {
    			if ( listener != t ) {
    				newList.add(listener);
    			}
    		}
    		this.osgiContext.setApplicationEventListeners(newList.toArray());
        }
    	else if (t instanceof HttpSessionListener || t instanceof ServletContextListener ) {
    		Object[] currentList = this.osgiContext.getApplicationLifecycleListeners();
    		ArrayList<Object> newList = new ArrayList<Object>();
    		for ( Object listener : currentList ) {
    			if ( listener != t ) {
    				newList.add(listener);
    			}
    		}
    		this.osgiContext.setApplicationLifecycleListeners(newList.toArray());
        }
    	  	
    	try {
            this.osgiContext.getInstanceManager().destroyInstance(t); // TODO: Do the same for servlets and filters?
        } 
    	catch (Throwable e) {
            log.error("Could not destroy listener: " + t, e);
        }
    	
    }
 
}
