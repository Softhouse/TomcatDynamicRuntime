package org.tdr.webresource.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 *
 * Web Resource. The target resource available for use on the web page.
 *
 * @author nic
 */
public class WebResource {

    private final ResourceType type;
    private final String name;
    private final List<WebModule> modules = new ArrayList<>();

    // TODO: Has some kind of merge status on the web resource??

    public WebResource(ResourceType type, String name) {
        this.type = type;
        this.name = name;
    }

    public ResourceType getType() {
        return this.type;
    }

    public String getName() {
        return this.name;
    }

    public void addModule(WebModule module) {
        this.modules.add(module);
        Collections.sort(this.modules);
    }

    public List<WebModule> getModules() {
        return this.modules;
    }

    public List<WebModule> getModulesByTypes(ResourceType ... types) {
        ArrayList<WebModule> filteredModuleList = new ArrayList<>();
        for ( WebModule module : this.modules ) {
            for ( ResourceType type : types ) {
                if ( module.getType() == type ) {
                    filteredModuleList.add(module);
                }
            }
        }
        return filteredModuleList;
    }

    public void addModulesFrom(WebResource webResource) {
        this.modules.addAll(webResource.getModules());
        Collections.sort(this.modules);

    }

    // TODO: How to handle undeploy??

}
