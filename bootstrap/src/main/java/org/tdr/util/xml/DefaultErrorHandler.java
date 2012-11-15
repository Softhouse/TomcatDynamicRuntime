package org.tdr.util.xml;

import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

public class DefaultErrorHandler implements ErrorHandler {
	static private Log log = LogFactory.getLog(DefaultErrorHandler.class);

	public void warning(SAXParseException exception) throws SAXException {
		log.warn("XML parsing warning.", exception);
	}

	public void error(SAXParseException exception) throws SAXException {
		log.error("XML parsing error.", exception);
	}

	public void fatalError(SAXParseException exception) throws SAXException {
		log.error("XML parsing fatal error.", exception);
	}

}
