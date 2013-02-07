package org.tdr.webresource.test;

import org.tdr.util.file.DirUtils;

import java.nio.file.Paths;

/**
 * @author nic
 */
public class TestCopyFiles {

    public static void main(String[] args) throws Exception {
        DirUtils.copy(Paths.get("web-resource-runtime/src/test/resources/from"),
                      Paths.get("web-resource-runtime/src/test/resources/to"));
    }
}
