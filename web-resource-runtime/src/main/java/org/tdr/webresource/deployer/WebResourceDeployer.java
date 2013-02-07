package org.tdr.webresource.deployer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.tdr.runtime.RuntimeDeployer;
import org.tdr.runtime.RuntimeDeployerRegistry;
import org.tdr.runtime.TDRBundle;
import org.tdr.util.xml.XMLUtil;
import org.tdr.webresource.model.ResourceType;
import org.tdr.webresource.model.WebModule;
import org.tdr.webresource.model.WebResource;
import org.tdr.webresource.processor.*;
import org.tdr.webresource.processor.css.AggregateLessModulesProcessor;
import org.tdr.webresource.processor.css.LessCompilationProcessor;
import org.tdr.webresource.processor.css.SpriteBuilderProcessor;
import org.tdr.webresource.processor.css.YuiCssCompressProcessor;
import org.tdr.webresource.processor.js.AggregateJsModulesProcessor;
import org.tdr.webresource.processor.js.JavascriptCompressProcessor;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.*;

/**
 * Web Resource Deployer
 *
 * User: nic
 */
public class WebResourceDeployer implements RuntimeDeployer {


    private static final Log log = LogFactory.getLog(WebResourceDeployer.class);

    private Map<String, WebResource> deployedWebResources = new HashMap<>();

    // TODO: Have this pluggable (via maybe a web resource processor deployer?)
    // TODO: Or just configurable via OSGi config service where the processors is listed up either by class name or logical name
    private WebResourceProcessor[] resourceProcessors = new WebResourceProcessor[]{
        // CSS processors
        new AggregateLessModulesProcessor(),
        new LessCompilationProcessor(),
        new SpriteBuilderProcessor(),
        new YuiCssCompressProcessor(),
        // Javascript processors
        new AggregateJsModulesProcessor(),
        new JavascriptCompressProcessor()
    };

    @Override
    public boolean deploy(TDRBundle bundle) {

        URL url = bundle.getBundle().getEntry("META-INF/web-resources.xml");
        if ( url != null ) {

            log.info("Deploying web-resources.xml...");

            try {

                File applicationBasePath = RuntimeDeployerRegistry.instance().getCurrentApplication().getApplicationBasePath();

                String applicationName = RuntimeDeployerRegistry.instance().getCurrentApplication().getApplicationBasePath().getName();
                String workAreaPath = System.getProperty("catalina.home") + "/tdr/apps/" + applicationName;

                String resourceSourcePath = bundle.getResourceSourcePath();

                WebResourceProcessManager processManager =
                        new WebResourceProcessManager(workAreaPath, applicationBasePath.getAbsolutePath(), resourceProcessors);

                Document doc = XMLUtil.getDocument(url.openStream());

                //boolean needsToBeProcessed = false;
                ArrayList<String> webResourcesNeededToProcessed = new ArrayList<>();
                Collection<Element> webResourceElements = XMLUtil.getChildElementsByName(doc.getDocumentElement(), "webresource");
                for ( Element webResourceElement : webResourceElements ) {
                    WebResource webResource =
                            this.parseWebResource(webResourceElement, resourceSourcePath);

                    if ( deployedWebResources.containsKey(webResource.getName()) ) {
                        // Merge web resource if already exists
                        deployedWebResources.get(webResource.getName()).addModulesFrom(webResource);
                    }
                    else {
                        deployedWebResources.put(webResource.getName(), webResource);
                    }
                    webResourcesNeededToProcessed.add(webResource.getName());
                    //if ( webResource.getFile().exists() && webResource.getFile().lastModified() < bundle.getBundle().getLastModified() ) {
                    //    needsToBeProcessed = true;
                    //}

                }
                for ( String webResourceName : webResourcesNeededToProcessed ) {
                    log.info("Processing web resource: " + webResourceName);
                    WebResource webResource = this.deployedWebResources.get(webResourceName);
                    processManager.process(webResource);
                }

                return true;
            }
            catch ( Throwable e ) {
                log.error("Could not deploy web resources.", e);
            }
        }
        return false;
    }

    @Override
    public void undeploy(TDRBundle bundle) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public List<TDRBundle> getDeployedBundles() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void shutdown() {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    protected WebResource parseWebResource(Element webResourceElement,
                                           String resourceSourcePath) throws IOException {
        ResourceType type = ResourceType.fromString(webResourceElement.getAttribute("type"));
        String name =  webResourceElement.getAttribute("name");
        WebResource webResource = new WebResource(type, name);

        Collection<Element> moduleElements = XMLUtil.getChildElementsByName(webResourceElement, "module");
        for ( Element moduleElement : moduleElements ) {
              WebModule webModule = this.parseWebModule(moduleElement, resourceSourcePath);
              webResource.addModule(webModule);
        }
        return webResource;
    }

    protected WebModule parseWebModule(Element moduleElement, String resourceSourcePath) throws IOException {
        ResourceType moduleType = ResourceType.fromString(moduleElement.getAttribute("type"));
        String name = moduleElement.getAttribute("name");
        int priority = Integer.parseInt(moduleElement.getAttribute("prio"));
        WebModule webModule = new WebModule(moduleType, name, resourceSourcePath, priority);
        Collection<Element> resourceElements = XMLUtil.getChildElementsByName(moduleElement, "resource");
        for ( Element resourceElement : resourceElements ) {
            String resourcePath = resourceElement.getAttribute("path");
            webModule.addResourcePath(resourcePath);
        }
        return webModule;
    }

}
