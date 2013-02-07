package org.tdr.webresource.util;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;

/**
 * @author nic
 */
public abstract class FileUtilities {

    private FileUtilities() {}

    static public void moveFile(String sourceFilename, String targetFilename) throws IOException {

        File sourceFile = new File(sourceFilename);
        File targetFile = new File(targetFilename);

        if ( targetFile.exists() ) {
            targetFile.delete();
        }
        FileUtils.moveFile(sourceFile, targetFile);

    }
}
