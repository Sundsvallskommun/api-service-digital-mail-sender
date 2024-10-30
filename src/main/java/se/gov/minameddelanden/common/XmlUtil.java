package se.gov.minameddelanden.common;

import static org.zalando.problem.Status.INTERNAL_SERVER_ERROR;
import static se.gov.minameddelanden.common.EncodingUtils.bytesToString;
import static se.gov.minameddelanden.common.EncodingUtils.stringToBytes;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.StringReader;
import java.nio.charset.Charset;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.zalando.problem.Problem;

public final class XmlUtil {

	private static final XMLInputFactory XML_INPUT_FACTORY = getXmlInputFactory();

	private XmlUtil() {}

	static byte[] domToBytes(final Document document) {
		var byteArrayOutputStream = new ByteArrayOutputStream();
		transform(getTransformer(), new DOMSource(document), new StreamResult(byteArrayOutputStream));
		return byteArrayOutputStream.toByteArray();
	}

	static Transformer getTransformer() {
		try {
			var transformer = TransformerFactory.newInstance().newTransformer();
			transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
			return transformer;
		} catch (TransformerException e) {
			throw Problem.builder()
				.withTitle("Couldn't create transformer")
				.withStatus(INTERNAL_SERVER_ERROR)
				.build();
		}
	}

	static void transform(final Transformer transformer, final Source source, final Result result) {
		try {
			transformer.transform(source, result);
		} catch (TransformerException e) {
			throw Problem.builder()
				.withTitle("Couldn't transform XML")
				.withStatus(INTERNAL_SERVER_ERROR)
				.build();
		}
	}

	public static void removeXmlNsPrefixes(final Document document) {
		removeXmlNsPrefixes((Node) document);
		document.normalizeDocument();
	}

	public static void removeXmlNsPrefixes(final Node node) {
		for (var n : domList(node.getChildNodes())) {
			if (n instanceof Element element) {
				for (var attribute : getAttributes(element)) {
					var attributeName = attribute.getNodeName();
					if (attributeName.startsWith("xmlns:")) {
						element.removeAttributeNode(attribute);
					}
				}
				removeXmlNsPrefixes(n);
				element.setPrefix("");
			}
		}
	}

	private static List<Attr> getAttributes(final Element e) {
		var attributes = new ArrayList<Attr>();
		var attributesNodeMap = e.getAttributes();
		for (var i = 0; i < attributesNodeMap.getLength(); ++i) {
			attributes.add((Attr) attributesNodeMap.item(i));
		}
		return attributes;
	}

	private static List<Node> domList(final NodeList childNodes) {
		return new AbstractList<>() {

			@Override
			public Node get(final int index) {
				return childNodes.item(index);
			}

			@Override
			public int size() {
				return childNodes.getLength();
			}
		};
	}

	private static DocumentBuilderFactory createDocumentBuilderFactory(final boolean namespaceAware) {
		try {
			var documentBuilderFactory = DocumentBuilderFactory.newInstance();
			documentBuilderFactory.setNamespaceAware(namespaceAware);
			return documentBuilderFactory;
		} catch (Exception e) {
			throw Problem.builder()
				.withTitle("Couldn't create document builder factory")
				.withStatus(INTERNAL_SERVER_ERROR)
				.build();
		}
	}

	static DocumentBuilder createDocumentBuilder(final boolean namespaceAware) {
		try {
			return createDocumentBuilderFactory(namespaceAware).newDocumentBuilder();
		} catch (Exception e) {
			throw Problem.builder()
				.withTitle("Couldn't create document builder")
				.withStatus(INTERNAL_SERVER_ERROR)
				.build();
		}
	}

	static Charset getEncodingFromXmlHeader(final XMLStreamReader xmlStreamReader) {
		var encoding = xmlStreamReader.getCharacterEncodingScheme();
		return null == encoding ? Xml.DEFAULT_ENCODING : Charset.forName(encoding);
	}

	static byte[] getXmlBytesFromString(final String s) {
		return stringToBytes(s, getEncodingFromXmlHeader(createXmlStreamReader(s)));
	}

	private static XMLStreamReader createXmlStreamReader(final String s) {
		XMLStreamReader xmlStreamReader;
		try {
			xmlStreamReader = XMLInputFactory.newInstance().createXMLStreamReader(new StringReader(s));
		} catch (XMLStreamException e) {
			throw Problem.builder()
				.withTitle("Couldn't create XML stream reader")
				.withStatus(INTERNAL_SERVER_ERROR)
				.build();
		}
		return xmlStreamReader;
	}

	static String getXmlStringFromBytes(final byte[] bytes) {
		return bytesToString(bytes, getEncodingFromXmlHeader(createXmlStreamReader(bytes)));
	}

	static XMLStreamReader createXmlStreamReader(final byte[] bytes) {
		XMLStreamReader xmlStreamReader;
		try {
			xmlStreamReader = XML_INPUT_FACTORY.createXMLStreamReader(new ByteArrayInputStream(bytes));
		} catch (XMLStreamException e) {
			throw Problem.builder()
				.withTitle("Couldn't create XML stream reader")
				.withStatus(INTERNAL_SERVER_ERROR)
				.build();
		}
		return xmlStreamReader;
	}

	static XMLInputFactory getXmlInputFactory() {
		var xmlInputFactory = XMLInputFactory.newInstance();
		Logger.getLogger(Xml.class.getName()).finest("Using XMLInputFactory implementation " + xmlInputFactory.getClass().getName());
		return xmlInputFactory;
	}

	public static void transform(final Source source, final Result result) {
		transform(getTransformer(), source, result);
	}
}
