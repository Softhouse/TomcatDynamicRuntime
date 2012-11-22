package org.tdr.web;

import java.beans.FeatureDescriptor;
import java.util.Iterator;

import javax.el.ELContext;
import javax.el.ELResolver;

import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.tdr.bootstrap.GlobalReference;

public class OSGiELResolver extends ELResolver {

	static private Log log = LogFactory.getLog(OSGiELResolver.class);
	
	private BundleContext bundleContext;
	
	public OSGiELResolver() {
		this.bundleContext = (BundleContext) GlobalReference.get("osgi/bundleContext");
	}
	
	@Override
	public Object getValue(ELContext context, Object base, Object property) {
		if (base == null) {
			String serviceName = property.toString();
			//log.info("Resolving OSGi service: " + serviceName);
			Object service = this.getServiceByName(serviceName);
			if ( service != null ) {
				context.setPropertyResolved(true);
			}
			return service;
		}
		return null;
	}
	
	/**
	 * Search OSGi Bundle Context after services.
	 * The following naming scheme is used org.xxx.Name = name
	 * 
	 * @param name
	 * @return
	 */
	protected Object getServiceByName(String name) {
		
		// TODO: How to unget services??
		// TODO: If service name is not present -> use class name instead
		
		try {
			ServiceReference[] serviceReferences = this.bundleContext.getAllServiceReferences(null, null);
			for ( ServiceReference serviceRef : serviceReferences ) {
				String serviceName = (String) serviceRef.getProperty("serviceName");
				//log.info("Service Ref: " + serviceRef + ", service name: " + serviceName);
				if ( name.equals(serviceName) ) {
					return this.bundleContext.getService(serviceRef);
				}
			}
		}
		catch ( Exception e ) {}
		return null;
	}

	@Override
	public Class<?> getType(ELContext context, Object base, Object property) {
		Object service = this.getValue(context, base, property);
		return service.getClass().getInterfaces()[0]; // TEMP TEST. This might be a dangerous call
	}

	@Override
	public void setValue(ELContext context, Object base, Object property,
			Object value) {		
	}

	@Override
	public boolean isReadOnly(ELContext context, Object base, Object property) {
		return true;
	}

	@Override
	public Iterator<FeatureDescriptor> getFeatureDescriptors(ELContext context,
			Object base) {
		return null;
	}

	@Override
	public Class<?> getCommonPropertyType(ELContext context, Object base) {
		return Object.class;
	}

}
