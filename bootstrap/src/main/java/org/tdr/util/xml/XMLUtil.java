package org.tdr.util.xml;

import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * XML utility methods.
 * 
 * @author nic
 */
public abstract class XMLUtil {
	
	private static final Log log = LogFactory.getLog(XMLUtil.class);

	/** The XML transformer attached to each thread it is used from. */
	private static ThreadLocal<Map<String, Transformer>> transformers = new ThreadLocal<Map<String, Transformer>>();

	/** The XML document builder attached to each thread it is used from. */
	private static ThreadLocal<DocumentBuilder> documentBuilder = new ThreadLocal<DocumentBuilder>();

	static final String DEFAULT_ENCODING = "ISO-8859-1";

	/**
	 * Get an instance of the Transformer. This method guarantees that each
	 * thread will have it's own unique instance.
	 * 
	 * @return
	 */
	private static Transformer getTransformer(String encoding) {
		Map<String, Transformer> transformerMap = transformers.get();
		if (transformerMap == null) {
			transformerMap = new HashMap<String, Transformer>();
			transformers.set(transformerMap);
		}
		Transformer transformerInstance = transformerMap.get(encoding);
		if (transformerInstance == null) {
			try {
				transformerInstance = TransformerFactory.newInstance()
						.newTransformer();
				transformerInstance.setOutputProperty(OutputKeys.ENCODING,
						encoding);
				transformerInstance.setOutputProperty(OutputKeys.STANDALONE,
						"yes");
				transformerInstance.setOutputProperty(OutputKeys.INDENT, "yes");
				transformerMap.put(encoding, transformerInstance);
			} catch (Exception ex) {
				log.error("Could not configure XML transformer", ex);
			}
		}
		return transformerInstance;
	}

	/**
	 * Get an instance of the DocumentBuilder. This method guarantees that each
	 * thread will have it's own unique instance.
	 * 
	 * @return
	 */
	private static DocumentBuilder getDocumentBuilder() {
		DocumentBuilder documentBuilderInstance = documentBuilder.get();
		if (documentBuilderInstance == null) {
			try {
				DocumentBuilderFactory factory = DocumentBuilderFactory
						.newInstance();
				factory.setNamespaceAware(true);
				factory.setValidating(false);
				factory.setIgnoringElementContentWhitespace(true);
				documentBuilderInstance = factory.newDocumentBuilder();

				// TODO: Do we need to specify another entity resolver impl as
				// well...??
				documentBuilderInstance
						.setEntityResolver(new DefaultEntityResolverImpl());
				documentBuilderInstance
						.setErrorHandler(new DefaultErrorHandler());
				documentBuilder.set(documentBuilderInstance);
			} catch (Exception ex) {
				log.error("Could not configure XML document builder", ex);
			}
		}
		return documentBuilderInstance;
	}

	/**
	 * Creates a new empty XML document. This method is thread safe since it
	 * uses a static DocumentBuilder initiated as ThreadLocal.
	 * 
	 * @param fileSystemResource
	 *            The resource pointing to the XML file
	 * @return The XML document
	 * @throws SAXException
	 * @throws IOException
	 */
	public static Document getDocument() {
		return getDocumentBuilder().newDocument();
	}

	/**
	 * Get an XML document from the provided InputStream. This method is thread
	 * safe since it uses a static DocumentBuilder initiated as ThreadLocal.
	 * 
	 * @param fileSystemResource
	 *            The resource pointing to the XML file
	 * @return The XML document
	 * @throws SAXException
	 * @throws IOException
	 */
	public static Document getDocument(InputStream inputStream)
			throws SAXException, IOException {
		DocumentBuilder documentBuilder = getDocumentBuilder();
		return documentBuilder.parse(inputStream);
	}

	/**
	 * Get an XML document from the provided XML formatted string. This method
	 * is thread safe since it uses a static DocumentBuilder initiated as
	 * ThreadLocal.
	 * 
	 * @param xmlString
	 *            The XML formatted string
	 * @return The XML document
	 * @throws SAXException
	 * @throws IOException
	 */
	public static Document getDocument(String xmlString) throws SAXException,
			IOException {
		DocumentBuilder documentBuilder = getDocumentBuilder();
		return documentBuilder.parse(new InputSource(
				new StringReader(xmlString)));
	}

	/**
	 * Get the first found child element with the provided name that is a direct
	 * child the provided element.
	 * 
	 * @param parent
	 *            The parent element
	 * @param name
	 *            The name of the child element to find
	 * @return The first found child element, null if no matching children
	 */
	public static Element getFirstChildElementByName(Element parent, String name) {
		assertNotNull(parent);
		NodeList children = parent.getChildNodes();
		Node node;
		for (int i = 0; i < children.getLength(); i++) {
			node = children.item(i);
			if (node.getNodeType() == Node.ELEMENT_NODE
					&& node.getNodeName().equals(name)) {
				return (Element) node;
			}
		}

		return null;
	}

	/**
	 * Get the first found child element with the provided local name that is a
	 * direct child the provided element.
	 * 
	 * @param parent
	 *            The parent element
	 * @param name
	 *            The name of the child element to find
	 * @return The first found child element, null if no matching children
	 */
	public static Element getFirstChildElementByLocalName(Element parent,
			String localName) {
		assertNotNull(parent);
		NodeList children = parent.getChildNodes();
		Node node;
		for (int i = 0; i < children.getLength(); i++) {
			node = children.item(i);
			if (node.getNodeType() == Node.ELEMENT_NODE
					&& node.getLocalName().equals(localName)) {
				return (Element) node;
			}
		}

		return null;
	}

	/**
	 * Get all child elements that are direct children the provided element.
	 * 
	 * @param parent
	 *            The parent element
	 * @return The list with child elements, empty list if no children
	 */
	public static Collection<Element> getChildElements(Element parent) {
		assertNotNull(parent);
		Collection<Element> childList = new ArrayList<Element>();

		NodeList children = parent.getChildNodes();
		Node node;
		for (int i = 0; i < children.getLength(); i++) {
			node = children.item(i);
			if (node.getNodeType() == Node.ELEMENT_NODE) {
				childList.add((Element) node);
			}
		}

		return childList;
	}

	/**
	 * Get all child elements with the provided name that are direct children
	 * the provided element.
	 * 
	 * @param parent
	 *            The parent element
	 * @param name
	 *            The name of the child elements to find
	 * @return The list with child elements, empty list if no matching children
	 */
	public static Collection<Element> getChildElementsByName(Element parent,
			String name) {
		assertNotNull(parent);
		Collection<Element> childList = new ArrayList<Element>();

		NodeList children = parent.getChildNodes();
		Node node;
		for (int i = 0; i < children.getLength(); i++) {
			node = children.item(i);
			if (node.getNodeType() == Node.ELEMENT_NODE
					&& node.getNodeName().equals(name)) {
				childList.add((Element) node);
			}
		}

		return childList;
	}

	/**
	 * Get all child elements with the provided local name that are direct
	 * children the provided element.
	 * 
	 * @param parent
	 *            The parent element
	 * @param name
	 *            The local name of the child elements to find
	 * @return The list with child elements, empty list if no matching children
	 */
	public static Collection<Element> getChildElementsByLocalName(
			Element parent, String name) {
		assertNotNull(parent);
		Collection<Element> childList = new ArrayList<Element>();

		NodeList children = parent.getChildNodes();
		Node node;
		for (int i = 0; i < children.getLength(); i++) {
			node = children.item(i);
			if (node.getNodeType() == Node.ELEMENT_NODE
					&& node.getLocalName().equals(name)) {
				childList.add((Element) node);
			}
		}

		return childList;
	}

	/**
	 * Get first child element with the provided node name and attribute that
	 * are direct child the provided element.
	 * 
	 * @param parent
	 * @param name
	 * @param attributeName
	 * @param attributeValue
	 * @return element if found, otherwise null
	 */
	public static Element getChildElementByNameAndAttribute(Element parent,
			String name, String attributeName, String attributeValue) {
		assertNotNull(parent);

		NodeList children = parent.getChildNodes();
		Node node;
		for (int i = 0; i < children.getLength(); i++) {
			node = children.item(i);
			if (node.getNodeType() == Node.ELEMENT_NODE
					&& node.getNodeName().equals(name)) {
				Element element = (Element) node;
				if (element.getAttribute(attributeName).equals(attributeValue)) {
					return element;
				}
			}
		}

		return null;
	}

	/**
	 * Get first child element with the provided local name and attribute that
	 * are direct child the provided element.
	 * 
	 * @param parent
	 * @param name
	 * @param attributeName
	 * @param attributeValue
	 * @return element if found, otherwise null
	 */
	public static Element getChildElementByLocalNameAndAttribute(
			Element parent, String name, String attributeName,
			String attributeValue) {
		assertNotNull(parent);

		NodeList children = parent.getChildNodes();
		Node node;
		for (int i = 0; i < children.getLength(); i++) {
			node = children.item(i);
			if (node.getNodeType() == Node.ELEMENT_NODE
					&& node.getLocalName().equals(name)) {
				Element element = (Element) node;
				if (element.getAttribute(attributeName).equals(attributeValue)) {
					return element;
				}
			}
		}

		return null;
	}

	/**
	 * Get the node value of the provided element. The method will list and
	 * concat the values of all children of the type <code>Node.TEXT_NODE</code>
	 * .
	 * 
	 * @param element
	 * @return
	 */
	public static String getElementValue(Element element) {
		assertNotNull(element);
		StringBuilder sb = new StringBuilder();
		NodeList children = element.getChildNodes();
		for (int i = 0; i < children.getLength(); i++) {
			short nodeType = children.item(i).getNodeType();
			if (nodeType == Node.TEXT_NODE
					|| nodeType == Node.CDATA_SECTION_NODE) {
				sb.append(children.item(i).getNodeValue());
			}
		}

		return sb.toString();
	}

	/**
	 * Create a new element with specified text value.
	 * 
	 * @param document
	 * @param nodeName
	 * @param textValue
	 * @return created element
	 */
	public static Element createElement(Document document, String nodeName,
			String textValue) {
		Element element = document.createElement(nodeName);
		Node textNode = document.createTextNode(textValue);
		element.appendChild(textNode);
		return element;
	}

	/**
	 * Create a new element belonging to a namespace with specified text value.
	 * 
	 * @param document
	 * @param nodeName
	 * @param textValue
	 * @return created element
	 */
	public static Element createElementNS(Document document,
			String namespaceURI, String nodeName, String textValue) {
		Element element = document.createElementNS(namespaceURI, nodeName);
		Node textNode = document.createTextNode(textValue);
		element.appendChild(textNode);
		return element;
	}

	/**
	 * Recursively removes all empty child text nodes starting from the provided
	 * node. <br>
	 * The method will also trim the non empty text nodes, i.e. any white spaces
	 * in the beginning and the end of the text node will be removed.
	 * 
	 * @param node
	 *            The node to normalize
	 */
	public static void normalizeDocument(Node node) {
		assertNotNull(node);
		if (!node.hasChildNodes()) {
			return;
		}

		NodeList nodeList = node.getChildNodes();
		String nodeValue;
		for (int i = 0; i < nodeList.getLength(); i++) {
			Node n = nodeList.item(i);

			if (n.getNodeType() == Node.TEXT_NODE) {
				nodeValue = n.getNodeValue().trim();

				// It the node is empty remove it
				if (XMLUtil.isBlank(nodeValue)) {
					node.removeChild(n);
					i--;
				}
				// Overwrite the node with the trimmed text
				else {
					n.setNodeValue(nodeValue);
				}
			} else {
				normalizeDocument(n);
			}
		}
	}

	/**
	 * Checks if a node value is blank (contains only white spaces).
	 * 
	 * @param value
	 *            The value to check
	 * @return false if the value is null or a non white space character is
	 *         found
	 */
	public static boolean isBlank(String value) {
		if (value == null) {
			return false;
		}

		for (int i = 0; i < value.length(); i++) {
			char c = value.charAt(i);
			if (!Character.isWhitespace(c)) {
				return false;
			}
		}

		return true;
	}

	/**
	 * Writes the document to a supplied outputstream.
	 * 
	 * <pre>
	 * As well as writing the output to a any type of OutputStream (e.g. sockets, files), 
	 * this method is also suitable to use for debugging. 
	 * The output could be either of the <code>System.out</code> or the <code>System.err</code> streams.
	 * This method is thread safe since it uses a static Transformer initiated as ThreadLocal.
	 * </pre>
	 * 
	 * @param ostream
	 *            The target stream
	 * @param closeStream
	 *            If the outputstream is to be closed after write
	 * @throws IOException
	 * @throws TransformerException
	 */
	public static void toStream(Document document, OutputStream ostream,
			boolean closeStream) throws IOException, TransformerException {
		toStream(document, ostream, closeStream, DEFAULT_ENCODING);
	}

	/**
	 * Writes the document to a supplied outputstream.
	 * 
	 * <pre>
	 * As well as writing the output to a any type of OutputStream (e.g. sockets, files), 
	 * this method is also suitable to use for debugging. 
	 * The output could be either of the <code>System.out</code> or the <code>System.err</code> streams.
	 * This method is thread safe since it uses a static Transformer initiated as ThreadLocal.
	 * </pre>
	 * 
	 * @param ostream
	 *            The target stream
	 * @param closeStream
	 *            If the outputstream is to be closed after write
	 * @param encoding
	 *            The encoding to use
	 * @throws IOException
	 * @throws TransformerException
	 */
	public static void toStream(Document document, OutputStream ostream,
			boolean closeStream, String encoding) throws IOException,
			TransformerException {
		assertNotNull(document);
		if (ostream == null) {
			throw new IllegalArgumentException(
					"The OutputStream cannot be null");
		}

		getTransformer(encoding).transform(new DOMSource(document),
				new StreamResult(new BufferedOutputStream(ostream)));
		ostream.flush();

		if (closeStream) {
			ostream.close();
		}
	}

	/**
	 * Writes the document to a supplied writer. This method is thread safe
	 * since it uses a static Transformer initiated as ThreadLocal.
	 * 
	 * @param writer
	 *            The target writer
	 * @throws IOException
	 * @throws TransformerException
	 */
	public static void toWriter(Document document, Writer writer)
			throws IOException, TransformerException {
		toWriter(document, writer, DEFAULT_ENCODING);
	}

	/**
	 * Writes the document to a supplied writer. This method is thread safe
	 * since it uses a static Transformer initiated as ThreadLocal.
	 * 
	 * @param writer
	 *            The target writer
	 * @param encoding
	 *            The encoding to use
	 * @throws IOException
	 * @throws TransformerException
	 */
	public static void toWriter(Document document, Writer writer,
			String encoding) throws IOException, TransformerException {
		assertNotNull(document);
		if (writer == null) {
			throw new IllegalArgumentException("The Writer cannot be null");
		}

		getTransformer(encoding).transform(new DOMSource(document),
				new StreamResult(new BufferedWriter(writer)));
		writer.flush();
	}

	/**
	 * Write the XML document to a string. This method is thread safe since it
	 * uses a static Transformer initiated as ThreadLocal.
	 * 
	 * @return The string, null if failed
	 */
	public static String toString(Document document) {
		return toString(document, DEFAULT_ENCODING);
	}

	/**
	 * Write the XML document to a string. This method is thread safe since it
	 * uses a static Transformer initiated as ThreadLocal.
	 * 
	 * @return The string, null if failed
	 */
	public static String toString(Document document, String encoding) {
		assertNotNull(document);
		try {
			StringWriter sw = new StringWriter();
			toWriter(document, sw, encoding);
			return sw.toString().trim();
		} catch (Exception ex) {
			log.warn("Failed to write XML to string", ex);
			return null;
		}
	}

	/**
	 * Performs a check that the provided Nodeobject is not null.
	 * 
	 * @param node
	 *            The node to check
	 * @throws IllegalArgumentException
	 *             In case the node is null
	 */
	private static void assertNotNull(Node node) {
		if (node == null) {
			throw new IllegalArgumentException("The input node cannot be null");
		}
	}
}
