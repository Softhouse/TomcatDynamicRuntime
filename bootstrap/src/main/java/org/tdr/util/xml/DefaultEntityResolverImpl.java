package org.tdr.util.xml;

import java.io.IOException;
import java.io.StringReader;

import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * A dummy <code>org.xml.sax.EntityResolver</code> implementation.
 * An enitity resolver is used by the XML parser to get the various resources
 * referenced by the XML document, e.g. DTD's or XML schemas.<br>
 * The class is intended to be used when the XML document uses
 * either a DTD or schema that is not available.<br>
 * This class will ignore any request to a DTD or a schema.
 * 
 * @author nic
 */
public class DefaultEntityResolverImpl implements EntityResolver {

    /**
     * The method will always return an empty <code>java.io.StringReader</code>.
     * I.e. <code>return new InputSource(new StringReader(""))</code>
     */
    public InputSource resolveEntity(String publicId, String systemId) throws SAXException, IOException 
    {
        return new InputSource(new StringReader(""));
    }

}

