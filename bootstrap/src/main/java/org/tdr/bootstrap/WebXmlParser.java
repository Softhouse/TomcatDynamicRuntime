package org.tdr.bootstrap;

import java.net.URL;

import org.apache.catalina.deploy.WebXml;
import org.apache.catalina.startup.DigesterFactory;
import org.apache.catalina.startup.WebRuleSet;
import org.apache.catalina.startup.XmlErrorHandler;
import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;
import org.apache.tomcat.util.digester.Digester;
import org.xml.sax.InputSource;
import org.xml.sax.SAXParseException;

public class WebXmlParser {

	private static final Log log = LogFactory.getLog(WebXmlParser.class);
	
    /**
     * The <code>Digester</code> we will use to process web fragment
     * deployment descriptor files.
     */
    protected Digester webFragmentDigester = null;
    protected WebRuleSet webFragmentRuleSet = null;
			
    public WebXmlParser() {
    	webFragmentRuleSet = new WebRuleSet(true);
        webFragmentDigester = DigesterFactory.newDigester(false, false, webFragmentRuleSet);
        webFragmentDigester.getParser();
    }
    
	public synchronized WebXml parseWebXmlFragment(URL url) {
        
        if (url == null) return null;
        
        WebXml fragment = new WebXml();
        
        try {     
	        InputSource source = new InputSource(url.openStream());
	        //InputSource source = new InputSource(url.toString());
	        //source.setByteStream(url.openStream());       
	
	        XmlErrorHandler handler = new XmlErrorHandler();
	
	        webFragmentDigester.push(fragment);
	        webFragmentDigester.setErrorHandler(handler);
           
	        webFragmentDigester.parse(source);

            if (handler.getWarnings().size() > 0 ||
                    handler.getErrors().size() > 0) {
                handler.logFindings(log, source.getSystemId());
            }
        } 
        catch (SAXParseException e) {
        	// TODO: Throw an exception here instead?
            log.error("Could not parse web fragment", e);
        } 
        catch (Exception e) {
            log.error("Error while parsing web fragment", e);
       
        } 
        finally {
        	webFragmentDigester.reset();
        	webFragmentRuleSet.recycle();
        }
        fragment.setURL(url);
        if (fragment.getName() == null) {
            fragment.setName(fragment.getURL().toString());
        }
        return fragment;
    }

}
