package org.tdr.webresource.processor.css;

import com.github.sommeri.less4j.Less4jException;
import com.github.sommeri.less4j.LessCompiler;
import com.github.sommeri.less4j.core.ThreadUnsafeLessCompiler;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.tdr.webresource.model.ResourceType;
import org.tdr.webresource.model.WebResource;
import org.tdr.webresource.processor.ProcessingException;
import org.tdr.webresource.processor.WebResourceProcessor;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;

/**
 * Less Compilation Processor
 *
 * @author nic
 */
public class LessCompilationProcessor implements WebResourceProcessor {

    private static final Log log = LogFactory.getLog(LessCompilationProcessor.class);

    @Override
    public void process(WebResource webResource, String workAreaPath) throws ProcessingException {

        if ( ! webResource.getType().equals(ResourceType.CSS) ) {
            return;
        }

        File lessFile = new File(workAreaPath + "/" + webResource.getName().replace(".css", ".less"));
        File cssFile = new File(workAreaPath + "/" + webResource.getName());

        try {
            LessCompiler lessCompiler = new ThreadUnsafeLessCompiler();
            log.info("Compiling Less file: " + lessFile);
            LessCompiler.CompilationResult result = lessCompiler.compile(lessFile);
            for ( LessCompiler.Problem problem : result.getWarnings() ) {
                log.warn("Less warning: " + problem.getMessage());
            }
            PrintWriter writer = new PrintWriter(new FileOutputStream(cssFile));
            writer.print(result.getCss());
            writer.close();
            log.info("Successfully compiled to CSS file: " + cssFile);
            lessFile.delete();
        }
        catch ( Less4jException e ) {
            for ( LessCompiler.Problem problem : e.getErrors() ) {
                log.error("Less error: " + problem.getMessage());
            }
            throw new ProcessingException("Compilation erros in LESS file: " + lessFile, e);
        }
        catch ( Exception e ) {
            throw new ProcessingException("Could not compile LESS file: " + lessFile, e);
        }

    }

}
