package org.tdr.runtime;

/**
 * Factory interface for creation of runtime deployers.
 * The factory class name is specified in the OSGi manifest
 * if there no other natural way of creating deployers at startup of the TDR bundle.
 *
 * @author nic
 */
public interface RuntimeDeployerFactory {

    /**
     * Create new deployer instance
     * @return deployer
     */
    public RuntimeDeployer createDeployer();
}
