package org.tdr.webresource.processor.js;

import org.tdr.webresource.model.ResourceType;
import org.tdr.webresource.model.WebResource;
import org.tdr.webresource.processor.ProcessingException;
import org.tdr.webresource.processor.WebResourceProcessor;
import org.tdr.webresource.util.FileUtilities;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;

/**
 * Processor for compressing Javascript resources.
 *
 * @author nic
 */
public class JavascriptCompressProcessor implements WebResourceProcessor {

    @Override
    public void process(WebResource webResource, String workAreaPath) throws ProcessingException {

        if ( ! webResource.getType().equals(ResourceType.JAVASCRIPT) ) {
            return;
        }

        try {
            String jsFilename = workAreaPath + "/" + webResource.getName();
            String compressedJsFilename = jsFilename.replace(".js", ".zjs");
            JSMin jsmin = new JSMin(new InputStreamReader(new FileInputStream(jsFilename)), new FileOutputStream(compressedJsFilename));
            jsmin.jsmin();
            FileUtilities.moveFile(compressedJsFilename, jsFilename);
        }
        catch ( Exception e ) {
            throw new ProcessingException("Could not compress Javascript file: " + webResource.getName(), e);
        }
    }
}
