package org.tdr.jsf.runtime;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

import javax.faces.context.FacesContext;
import javax.servlet.ServletContext;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import com.sun.faces.config.DbfFactory;
import com.sun.faces.config.FacesConfigManager;
import com.sun.faces.config.processor.ApplicationConfigProcessor;
import com.sun.faces.config.processor.ComponentConfigProcessor;
import com.sun.faces.config.processor.ConfigProcessor;
import com.sun.faces.config.processor.ConverterConfigProcessor;
import com.sun.faces.config.processor.FactoryConfigProcessor;
import com.sun.faces.config.processor.LifecycleConfigProcessor;
import com.sun.faces.config.processor.ManagedBeanConfigProcessor;
import com.sun.faces.config.processor.NavigationConfigProcessor;
import com.sun.faces.config.processor.RenderKitConfigProcessor;
import com.sun.faces.config.processor.ValidatorConfigProcessor;

public class FacesConfigProcessor {

	private static final Log log = LogFactory.getLog(FacesConfigProcessor.class);
	
	private static final ConfigProcessor CONFIG_PROCESSOR_CHAIN;
	
	static {
        ConfigProcessor[] configProcessors = {
             new FactoryConfigProcessor(),
             new LifecycleConfigProcessor(),
             new ApplicationConfigProcessor(),
             new ComponentConfigProcessor(),
             new ConverterConfigProcessor(),
             new ValidatorConfigProcessor(),
             new ManagedBeanConfigProcessor(),
             new RenderKitConfigProcessor(),
             new NavigationConfigProcessor()
        };
        for (int i = 0; i < configProcessors.length; i++) {
            ConfigProcessor p = configProcessors[i];
            if ((i + 1) < configProcessors.length) {
                p.setNext(configProcessors[i + 1]);
            }
        }
        CONFIG_PROCESSOR_CHAIN = configProcessors[0];
    }
	
	private ServletContext servletContext;
	
	public FacesConfigProcessor(ServletContext servletContext) {
		this.servletContext = servletContext;
	}
	
	public void processFacesConfig(URL configUrl) {
		
		FacesContext initContext = FacesConfigManager.createInitFacesContext(this.servletContext);
		try {
			Document doc = getDocument(configUrl);
			log.info("Processing XML document: " + doc);
			CONFIG_PROCESSOR_CHAIN.process(new Document[] {doc});
		}
		catch ( Exception e ) {
			log.error("Error while processing faces-config.xml", e);
		}
		finally {
			initContext.release();
		}
	}
	
	/**
     * @return <code>Document</code> based on <code>documentURL</code>.
     * @throws Exception if an error occurs during the process of building a
     *  <code>Document</code>
     */
    private Document getDocument(URL configUrl) throws Exception {
        
        // validation isn't required, parse and return
        DocumentBuilder builder = getNonValidatingBuilder();
        InputSource is = new InputSource(getInputStream(configUrl));
        is.setSystemId(configUrl.toExternalForm());
        return builder.parse(is);
       
    }

	private DocumentBuilder getNonValidatingBuilder() throws Exception {
	
	    DocumentBuilderFactory tFactory = DbfFactory.getFactory();
	    tFactory.setValidating(false);
	    DocumentBuilder tBuilder = tFactory.newDocumentBuilder();
	    tBuilder.setEntityResolver(DbfFactory.FACES_ENTITY_RESOLVER);
	    tBuilder.setErrorHandler(DbfFactory.FACES_ERROR_HANDLER);
	    return tBuilder;
	
	}
	
    private static InputStream getInputStream(URL url) throws IOException {

        URLConnection conn = url.openConnection();
        conn.setUseCaches(false);
        return new BufferedInputStream(conn.getInputStream());

    }
	
}
