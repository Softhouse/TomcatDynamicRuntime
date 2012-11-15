package org.tdr.jsf.runtime;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.faces.FacesException;
import javax.faces.FactoryFinder;
import javax.faces.application.ViewHandler;
import javax.faces.component.UIViewRoot;
import javax.faces.context.FacesContext;
import javax.servlet.ServletContext;

import org.tdr.runtime.RuntimeDeployer;
import org.tdr.runtime.RuntimeDeployerRegistry;
import org.tdr.runtime.TDRBundle;
import org.tdr.util.xml.XMLUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import com.sun.facelets.compiler.Compiler;
import com.sun.facelets.compiler.TagLibraryConfig;
import com.sun.facelets.tag.TagLibrary;

/**
 * Facelet View Handler extended with functionality for deploying/undeploying runtime bundles.
 * The facelet deployer looks for META-INF/facelet-fragment.xml
 * 
 * XML Format.
 * 
 * <facelet-fragment>
 *   <library-class>[class name]</library-class>
 *    ...
 * </facelet-fragment>
 * 
 * @author nic
 */

// TODO: How to handle scan of additional JSF configs hidden in deployed JAR files?

public class FaceletViewHandler extends com.sun.facelets.FaceletViewHandler implements RuntimeDeployer {
	
	private Compiler compiler = null;
	private ServletContext servletContext;
	private FacesConfigProcessor facesConfigProcessor;
	private Map<TagLibrary, TDRBundle> notInitializedTagLibraries = new HashMap<TagLibrary, TDRBundle>();
	private Map<TagLibrary, TDRBundle> initializedTagLibraries = new HashMap<TagLibrary, TDRBundle>();
	private ArrayList<TDRBundle> deployedBundles = new ArrayList<TDRBundle>();
	
	public FaceletViewHandler(ViewHandler parent) {
		super(parent);
		log.info("Adding runtime deployer...");
		RuntimeDeployerRegistry.instance().addDeployer(this);
		this.facesConfigProcessor = new FacesConfigProcessor(RuntimeDeployerRegistry.instance().getServletContext());
	}
	
	@Override
	protected void initialize(FacesContext context) {
		super.initialize(context);
		
		// TODO: Is needed??
		this.servletContext = (ServletContext) context.getExternalContext().getContext();
		
	}
		
	@Override
	protected synchronized void initializeCompiler(Compiler compiler) {
		this.compiler = compiler;
		for ( TagLibrary  taglib: this.notInitializedTagLibraries.keySet() ) {
			this.addTagLibrary(taglib, this.notInitializedTagLibraries.get(taglib));
		}
		this.notInitializedTagLibraries.clear();
		super.initializeCompiler(compiler);
	}

	/* (non-Javadoc)
	 * @see org.tdr.runtime.RuntimeDeployer#deploy(org.tdr.runtime.TDRBundle)
	 */
	@Override
	public synchronized boolean deploy(TDRBundle bundle) {
		
		boolean deploymentTriggered = this.deployTaglibs(bundle);
		deploymentTriggered |= this.deployFaceletConfig(bundle);
		if ( deploymentTriggered ) {
			this.deployedBundles.add(bundle);
		}
		return deploymentTriggered;
	}
	
	private boolean deployTaglibs(TDRBundle bundle) {
		boolean deployedTaglibs = false;
		Enumeration<String> metaInfEntries = bundle.getBundle().getEntryPaths("META-INF");
		while ( metaInfEntries.hasMoreElements() ) {
			String metaInfEntry = metaInfEntries.nextElement();
			if ( metaInfEntry.endsWith("taglib.xml") ) {
				URL url = bundle.getBundle().getEntry(metaInfEntry);
				if ( url != null ) {
					deployedTaglibs = true;
					log.info("Deploying taglib: " + metaInfEntry); 
					log.info("Current CL:\n" + Thread.currentThread().getContextClassLoader());
		
					try {
						TagLibrary taglib = TagLibraryConfig.create(url);
						if ( this.compiler == null ) {
							this.notInitializedTagLibraries.put(taglib, bundle);
						}
						else {
							this.addTagLibrary(taglib, bundle);	
						}
					} 
					catch (IOException e) {
						log.severe("Error while reading facelet-fragment.xml: " + e);
						e.printStackTrace();
					}
				}
			}
		}
		return deployedTaglibs;
	}
	
	private boolean deployFaceletConfig(TDRBundle bundle) {
		URL url = bundle.getBundle().getEntry("META-INF/faces-config.xml");
		if ( url != null ) {
			log.info("Deploying faces-config.xml...");
			this.facesConfigProcessor.processFacesConfig(url);
			
			// TODO: Scan lib/*.jar aswell for faces-config.xml
			// TODO: How to undeploy this data???
			
			return true;
		}
		return false;
	}

	/* (non-Javadoc)
	 * @see org.tdr.runtime.RuntimeDeployer#undeploy(org.tdr.runtime.TDRBundle)
	 */
	@Override
	public synchronized void undeploy(TDRBundle bundle) {
		
		if ( this.deployedBundles.contains(bundle) ) {
			if ( this.compiler == null ) {
				ArrayList<TagLibrary> libList = new ArrayList<TagLibrary>();
				for ( TagLibrary tagLibrary : this.notInitializedTagLibraries.keySet() ) {
					TDRBundle currentBundle = this.notInitializedTagLibraries.get(tagLibrary);
					if ( currentBundle == bundle ) {
						libList.add(tagLibrary);	
					}
				}
				for ( TagLibrary tagLibrary : libList ) {
					this.notInitializedTagLibraries.remove(tagLibrary);
				}
			}
			else {
				ArrayList<TagLibrary> libList = new ArrayList<TagLibrary>();
				for ( TagLibrary tagLibrary : this.initializedTagLibraries.keySet() ) {
					TDRBundle currentBundle = this.initializedTagLibraries.get(tagLibrary);
					if ( currentBundle == bundle ) {
						libList.add(tagLibrary);	
					}
				}
				for ( TagLibrary tagLibrary : libList ) {
					this.removeTagLibrary(tagLibrary);
				}
			}
			this.deployedBundles.remove(bundle);
		}
		
		// TODO: Undeploy application factories etc here!!!
	}
	
	/*
	 * (non-Javadoc)
	 * @see org.tdr.runtime.RuntimeDeployer#getDeployedBundles()
	 */
	@Override
	public List<TDRBundle> getDeployedBundles() {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public void shutdown() {
		FactoryFinder.releaseFactories();
	}
	
	private void addTagLibrary(TagLibrary tagLibrary, TDRBundle bundle) {
		this.compiler.addTagLibrary(tagLibrary);
		this.initializedTagLibraries.put(tagLibrary, bundle);
	}
	
	private void removeTagLibrary(TagLibrary tagLibrary) {
		this.compiler.removeTagLibrary(tagLibrary);
		this.initializedTagLibraries.remove(tagLibrary);
	}

	@Override
	public void renderView(FacesContext context, UIViewRoot viewToRender) throws IOException, FacesException {
		log.info("Render view: " + viewToRender.getViewId());
		super.renderView(context, viewToRender);
	}

}
