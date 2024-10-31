package se.gov.minameddelanden.common;

import static org.zalando.problem.Status.INTERNAL_SERVER_ERROR;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.util.Arrays;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.zalando.problem.Problem;

import jakarta.xml.bind.JAXBException;
import se.gov.minameddelanden.common.sign.SignatureUtils;

public class Xml {

	static final Charset DEFAULT_ENCODING = EncodingUtils.UTF_8;

	private byte[] bytes;
	private String string;
	private Source source;

	Xml(final byte[] bytes) {
		if (null == bytes) {
			throw new IllegalArgumentException("bytes cannot be null");
		}
		this.bytes = bytes;
	}

	public static Xml fromBytes(final byte[] bytes) {
		return new Xml(bytes);
	}

	public static Xml fromDOM(final Document document) {
		return new Xml(XmlUtil.domToBytes(document));
	}

	public SignedXml sign(final X509CertificateWithPrivateKey certificateWithPrivateKey) {
		var xmlDocument = toDOM();
		var xmlSignature = SignatureUtils.signXml(xmlDocument, certificateWithPrivateKey);
		return new SignedXml(XmlUtil.domToBytes(xmlDocument), xmlSignature);
	}

	public byte[] toBytes() {
		initBytes();
		return bytes;
	}

	public <T> T toJaxbObject(final Class<T> resultType) {
		try {
			return JAXBMarshal.deserialize(toBytes(), resultType);
		} catch (JAXBException e) {
			throw Problem.builder()
				.withTitle("Couldn't deserialize result to JAXB object")
				.withStatus(INTERNAL_SERVER_ERROR)
				.build();
		}
	}

	public Document toDOM() {
		return toDOM(true);
	}

	private Document toDOM(final boolean namespaceAware) {
		return toDOM(XmlUtil.createDocumentBuilder(namespaceAware));
	}

	private Document toDOM(final DocumentBuilder documentBuilder) {
		try {
			return documentBuilder.parse(new ByteArrayInputStream(toBytes()));
		} catch (Exception e) {
			throw Problem.builder()
				.withTitle("Couldn't parse XML to DOM")
				.withStatus(INTERNAL_SERVER_ERROR)
				.build();
		}
	}

	private void initBytes() {
		if (null != bytes) {
			return;
		}
		if (null != source) {
			var outputStream = new ByteArrayOutputStream();
			transformSource(new StreamResult(outputStream));
			bytes = outputStream.toByteArray();
		} else {
			bytes = XmlUtil.getXmlBytesFromString(string);
		}
	}

	private void transformSource(final StreamResult result) {
		XmlUtil.transform(source, result);
		source = null;
	}

	private void initString() {
		if (null != string) {
			return;
		}
		if (null != source) {
			var writer = new StringWriter();
			transformSource(new StreamResult(writer));
			string = writer.toString();
		} else {
			string = XmlUtil.getXmlStringFromBytes(toBytes());
		}
	}

	@Override
	public int hashCode() {
		return Arrays.hashCode(toBytes());
	}

	@Override
	public boolean equals(final Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}

		var xml = (Xml) o;

		return Arrays.equals(toBytes(), xml.toBytes());
	}

	@Override
	public String toString() {
		initString();
		return string;
	}
}
