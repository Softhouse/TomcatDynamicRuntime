package org.tdr.runtime;

import java.util.List;

public interface RuntimeDeployer {

	// TODO: Throw some kind of exception here?? IOException?
		
	public boolean deploy(TDRBundle bundle);
	
	public void undeploy(TDRBundle bundle);
	
	public List<TDRBundle> getDeployedBundles();
	
	public void shutdown();
}
