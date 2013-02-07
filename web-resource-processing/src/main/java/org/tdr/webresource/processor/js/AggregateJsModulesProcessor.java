package org.tdr.webresource.processor.js;

import org.apache.commons.io.FileUtils;
import org.tdr.webresource.model.ResourceType;
import org.tdr.webresource.model.WebModule;
import org.tdr.webresource.model.WebResource;
import org.tdr.webresource.processor.ProcessingException;
import org.tdr.webresource.processor.WebResourceProcessor;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * Processor for aggregating Javascript modules.
 *
 * @author nic
 */
public class AggregateJsModulesProcessor implements WebResourceProcessor {

    // TODO: Is this a generic modules that can aggregate any kind of module??
    // How to control that this is not processed? Have some kind of source to target switch???
    // I.e. a source2target processor
    // TODO: When to remove web modules???

    @Override
    public void process(WebResource webResource, String workAreaPath) throws ProcessingException {

        if ( ! webResource.getType().equals(ResourceType.JAVASCRIPT) ) {
            return;
        }

        try {
            File aggregatedJsFile = new File(workAreaPath + "/" + webResource.getName());

            PrintWriter writer = new PrintWriter(new FileOutputStream(aggregatedJsFile));
            for ( WebModule module : webResource.getModulesByTypes(ResourceType.JAVASCRIPT) ) {
                String jsContent = FileUtils.readFileToString(new File(workAreaPath + "/" + module.getName()));
                writer.println(jsContent);
            }
            writer.close();

        }
        catch ( IOException e ) {
            throw new ProcessingException("Could not aggregate modules to one single Javascript file." , e);
        }

    }
}
