package org.tdr.webresource.processor;

import org.tdr.webresource.model.WebModule;
import org.tdr.webresource.model.WebResource;

import java.util.List;

/**
 * @author nic
 */
public class ProcessingData {

    private final WebResource webResource;
    private final List<WebModule> webModules;

    public ProcessingData(final WebResource webResource, final List<WebModule> webModules) {
        this.webResource = webResource;
        this.webModules = webModules;
    }


    public WebResource getWebResource() {
        return this.webResource;
    }

    public List<WebModule> getWebModules() {
        return this.webModules;
    }

    /**
     * Static method to create some syntactical sugar for passing parameters to next processor.
     *
     * @param webResource
     * @param webModules
     * @return new instance of ProcessingData
     */
    public static ProcessingData ProcessNext(final WebResource webResource, final List<WebModule> webModules) {
        return new ProcessingData(webResource, webModules);
    }

}
