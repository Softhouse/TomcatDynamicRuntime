package org.tdr.webresource.processor.css;

import org.tdr.webresource.model.ResourceType;
import org.tdr.webresource.model.WebModule;
import org.tdr.webresource.model.WebResource;
import org.tdr.webresource.processor.ProcessingException;
import org.tdr.webresource.processor.WebResourceProcessor;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

/**
 * Processor for aggregrating LESS modules into one Less file by using ImportOnce directive.
 *
 * @author nic
 */
public class AggregateLessModulesProcessor implements WebResourceProcessor {

    @Override
    public void process(WebResource webResource, String workAreaPath) throws ProcessingException {

        // TODO: Analyse the paths and build smarter import statements if the CSS files are not in the same directory
        // -> Is needed if we copy the source less files to work area??
        // Have some kind of control mechanism to control when a resource is copied to the work area path
        // -> Done by a dedicated processor or processor manager???


        if ( ! webResource.getType().equals(ResourceType.CSS) ) {
            return;
        }

        try {
            File aggregatedLessFile = new File(workAreaPath + "/" + webResource.getName().replace(".css", ".less"));
            //aggregatedLessFile.getParentFile().mkdirs();

            PrintWriter writer = new PrintWriter(new FileOutputStream(aggregatedLessFile));
            for ( WebModule module : webResource.getModulesByTypes(ResourceType.LESS) ) {

                int pathIndex = module.getName().lastIndexOf("/");
                String moduleFilename = module.getName().substring(pathIndex+1);

                writer.println("@import-once \"" + moduleFilename + "\";");
            }
            writer.close();

        }
        catch ( IOException e ) {
            throw new ProcessingException("Could not aggregate modules to one single LESS file." , e);
        }

    }
}
