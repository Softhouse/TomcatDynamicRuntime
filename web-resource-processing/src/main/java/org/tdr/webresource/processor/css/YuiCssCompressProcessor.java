package org.tdr.webresource.processor.css;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.tdr.webresource.model.ResourceType;
import org.tdr.webresource.model.WebResource;
import org.tdr.webresource.processor.ProcessingException;
import org.tdr.webresource.processor.WebResourceProcessor;
import org.tdr.webresource.util.FileUtilities;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;

/**
 * @author nic
 */
public class YuiCssCompressProcessor implements WebResourceProcessor {

    private static final Log log = LogFactory.getLog(YuiCssCompressProcessor.class);

    @Override
    public void process(WebResource webResource, String workAreaPath) throws ProcessingException {

        if ( webResource.getType() != ResourceType.CSS ) {
            return;
        }

        try {
            String cssFilename = workAreaPath + "/" + webResource.getName();
            String compressedCssFilename = cssFilename.replace(".css", ".zcss");
            FileReader reader = new FileReader(cssFilename);
            FileWriter writer = new FileWriter(compressedCssFilename);
            YuiCssCompressor compressor = new YuiCssCompressor(reader);
            compressor.compress(writer, -1);
            writer.close();
            FileUtilities.moveFile(compressedCssFilename, cssFilename);
        }
        catch ( IOException e ) {
             throw new ProcessingException("Could not compress CSS file: " + webResource.getName(), e);
        }
    }
}
