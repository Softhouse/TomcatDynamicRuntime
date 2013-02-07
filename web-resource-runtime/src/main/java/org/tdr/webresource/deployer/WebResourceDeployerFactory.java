package org.tdr.webresource.deployer;

import org.tdr.runtime.RuntimeDeployer;
import org.tdr.runtime.RuntimeDeployerFactory;

/**
 * Factory for web resource deployer.
 * @author nic
 */
public class WebResourceDeployerFactory implements RuntimeDeployerFactory {

    @Override
    public RuntimeDeployer createDeployer() {
        return new WebResourceDeployer();
    }
}
