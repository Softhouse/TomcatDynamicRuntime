package org.tdr.runtime;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletContext;

import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;

public class RuntimeDeployerRegistry {
	
	private static final Log log = LogFactory.getLog(RuntimeDeployerRegistry.class);
	
	public static final String CONTEXT_NAME = "TDR.RuntimeDeployerRegistry";
	
	private List<RuntimeDeployer> deployers = new ArrayList<RuntimeDeployer>();
	private Map<RuntimeDeployer, TDRBundle> deployerOwners = new HashMap<RuntimeDeployer, TDRBundle>();
	private ThreadLocal<TDRBundle> currentbundle = new ThreadLocal<TDRBundle>();
	private ServletContext servletContext;
	private static ThreadLocal<RuntimeDeployerRegistry> instance = new ThreadLocal<RuntimeDeployerRegistry>();
	
	public RuntimeDeployerRegistry(ServletContext servletContext) {
		this.servletContext = servletContext;
	}
	
	public List<RuntimeDeployer> getDeployers() {
		return this.deployers;
	}
	
	public List<RuntimeDeployer> getDeployersInReverseOrder() {
		ArrayList<RuntimeDeployer> reverseList = new ArrayList<RuntimeDeployer>();
		reverseList.addAll(this.deployers);
		Collections.reverse(reverseList);
		return reverseList;
	}
	
	public void setCurrentBundle(TDRBundle currentBundle) {
		this.currentbundle.set(currentBundle);
	}
	
	public void clearCurrentBundle() {
		this.currentbundle.remove();
	}
	
	public void setInstance() {
		instance.set(this);
	}
	
	public void clearInstance() {
		instance.remove();
	}
	
	public void addDeployer(RuntimeDeployer deployer) {
		List<RuntimeDeployer> newList = new ArrayList<RuntimeDeployer>();
		newList.addAll(this.deployers);
		newList.add(deployer);
		this.deployers = newList;
		this.deployerOwners.put(deployer, this.currentbundle.get());
	}
	
	public synchronized void removeDeployer(RuntimeDeployer deployer) {
		List<RuntimeDeployer> newList = new ArrayList<RuntimeDeployer>();
		newList.addAll(this.deployers);
		newList.remove(deployer);
		this.deployers = newList;
		this.deployerOwners.remove(deployer);
	}
	
	public void removeDeployersOwnedBy(TDRBundle bundle) {
		ArrayList<RuntimeDeployer> removeList = new ArrayList<RuntimeDeployer>();
		for ( RuntimeDeployer deployer : this.deployerOwners.keySet() ) {
			TDRBundle owner = this.deployerOwners.get(deployer);
			if ( owner == bundle ) {
				removeList.add(deployer);
			}
		}
		for ( RuntimeDeployer deployer : removeList ) {
			log.info("Removing deployer (owned by bundle: " + bundle.getBundle().getSymbolicName() + " : " + deployer);
			removeDeployer(deployer);
		}
	}
	
	public TDRBundle getDeployerOwner(RuntimeDeployer deployer) {
		return this.deployerOwners.get(deployer);
	}
	
	public Collection<RuntimeDeployer> getDeployersOwnedBy(TDRBundle bundle) {
		ArrayList<RuntimeDeployer> deployerList = new ArrayList<RuntimeDeployer>();
		for ( RuntimeDeployer deployer : this.deployerOwners.keySet() ) {
			TDRBundle owner = this.deployerOwners.get(deployer);
			if ( owner == bundle ) {
				deployerList.add(deployer);
			}
		}
		return deployerList;
	}
	
	static public RuntimeDeployerRegistry instance(ServletContext servletContext) {
		return (RuntimeDeployerRegistry) servletContext.getAttribute(CONTEXT_NAME);
	}
	
	static public RuntimeDeployerRegistry instance() {
		return instance.get();
	}
	
	public ServletContext getServletContext() {
		return this.servletContext;
	}
}
