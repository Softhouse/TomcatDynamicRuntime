package org.tdr.webresource.processor;

import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.tdr.webresource.model.WebModule;
import org.tdr.webresource.model.WebResource;

import java.io.File;
import java.io.IOException;

/**
 * Web Resource Process Manager.
 * Orchestrates the processing of web resources.
 * Input to processing is a number of web modules and a path to a web resource.
 * Output is processed and compiled web resource, e.g. a CSS or Javascript file.
 *
 * @author nic
 */
public class WebResourceProcessManager {

    private static final Log log = LogFactory.getLog(WebResourceProcessManager.class);

    private final String workAreaPath;
    private final String publishPath;

    // TODO: Should the configuration of the processor chain be inserted here???

    // To start with the processor chain is hardcoded...


    private final WebResourceProcessor[] processors;

    public WebResourceProcessManager(String workAreaPath, String publishPath, WebResourceProcessor[] processors) throws ProcessingException {

        this.workAreaPath = workAreaPath;
        this.publishPath = publishPath;
        this.processors = processors;
        this.createWorkArea();
    }

    protected void createWorkArea() throws ProcessingException {
        File workAreaDir = new File(this.workAreaPath);
        boolean successfulMkdir = true;
        if ( !workAreaDir.exists() ) {
            successfulMkdir = workAreaDir.mkdirs();
        }
        if ( !successfulMkdir ||
             !workAreaDir.canRead() ||
             !workAreaDir.canWrite() ) {
            throw new ProcessingException("Invalid work area for web resource processing: " + workAreaPath);
        }
    }

    public void process(WebResource webResource) throws ProcessingException {

        String resourceWorkAreaPath = this.workAreaPath + "/" + webResource.getName().replace("/", "_").replace(".", "_");

        File resourceWorkAreaDir = new File(resourceWorkAreaPath);
        resourceWorkAreaDir.mkdir();
        this.copyResourcesToWorkArea(webResource, resourceWorkAreaPath);
        for ( WebResourceProcessor processor : this.processors ) {
            log.info("Invoking processor: " + processor.getClass().getName());
            processor.process(webResource, resourceWorkAreaPath);
        }
        publishResources(webResource, resourceWorkAreaPath);
    }

    protected void copyResourcesToWorkArea(WebResource webResource, String resourceWorkAreaPath) throws ProcessingException {

        for ( WebModule module : webResource.getModules() ) {
            try {
                String moduleWorkAreaPath = resourceWorkAreaPath + "/" + module.getName();
                new File(moduleWorkAreaPath).getParentFile().mkdirs();

                FileUtils.copyFile(new File(module.getSourcePath() + "/" + module.getName()),
                                   new File(moduleWorkAreaPath));
                //Files.copy(Paths.get(module.getSourcePath() + "/" + module.getName()),
                //           Paths.get(moduleWorkAreaPath),
                //           StandardCopyOption.REPLACE_EXISTING);

                for ( String resourcePath : module.getResourcePaths() ) {
                    FileUtils.copyDirectory(new File(module.getSourcePath() + "/" + resourcePath),
                                            new File(resourceWorkAreaPath + "/" + resourcePath));
                    //DirUtils.copy(Paths.get(module.getSourcePath() + "/" + resourcePath), Paths.get(resourceWorkAreaPath + "/" + resourcePath));
                }
            } catch (IOException e) {
                throw new ProcessingException("Could not copy resources to work area: " + resourceWorkAreaPath, e);
            }
        }

    }

    protected void publishResources(WebResource webResource, String resourcePath) throws ProcessingException {

        try {
            FileUtils.copyDirectory(new File(resourcePath), new File(this.publishPath));
            //DirUtils.copy(Paths.get(resourcePath), Paths.get(this.publishPath));
        } catch (IOException e) {
            throw new ProcessingException("Could copy web resources to: " + this.publishPath, e);
        }
    }

}
