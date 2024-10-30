package se.gov.minameddelanden.common.sign;

import static java.util.Collections.singletonList;
import static org.zalando.problem.Status.INTERNAL_SERVER_ERROR;
import static se.gov.minameddelanden.common.XmlUtil.removeXmlNsPrefixes;

import java.util.ArrayList;
import java.util.regex.Pattern;

import javax.xml.crypto.dsig.CanonicalizationMethod;
import javax.xml.crypto.dsig.DigestMethod;
import javax.xml.crypto.dsig.SignatureMethod;
import javax.xml.crypto.dsig.Transform;
import javax.xml.crypto.dsig.XMLSignature;
import javax.xml.crypto.dsig.XMLSignatureFactory;
import javax.xml.crypto.dsig.dom.DOMSignContext;
import javax.xml.crypto.dsig.spec.C14NMethodParameterSpec;
import javax.xml.crypto.dsig.spec.TransformParameterSpec;

import org.w3c.dom.Document;
import org.zalando.problem.Problem;

import se.gov.minameddelanden.common.X509CertificateWithPrivateKey;

public final class SignatureUtils {

	private SignatureUtils() {}

	public static XMLSignature signXml(final Document doc, final X509CertificateWithPrivateKey certificateWithPrivateKey) {
		var element = doc.getDocumentElement();
		var ref = element.getAttribute("Id");

		if (ref.length() == 0) {
			ref = "";
		} else {
			ref = "#" + ref;
			setId(doc);
		}

		return signXml(doc, ref, certificateWithPrivateKey);
	}

	/**
	 * Create a DOM XMLSignatureFactory that will be used to
	 * generate the enveloped signature.
	 * Om appservern (ex. vis WebSphere) använder egen implementation av krypto
	 * kan följande behövas användas :
	 * String providerName = "org.jcp.xml.dsig.internal.dom.XMLDSigRI";
	 * XMLSignatureFactory xmlSignatureFactory = XMLSignatureFactory.getInstance("DOM", (Provider)
	 * Class.forName(providerName).newInstance());
	 * 
	 * @param  doc
	 * @param  uriRef
	 * @param  certificateWithPrivateKey
	 * @return
	 */
	public static XMLSignature signXml(final Document doc, final String uriRef,
		final X509CertificateWithPrivateKey certificateWithPrivateKey) {
		removeXmlNsPrefixes(doc);

		try {
			//

			var xmlSignatureFactory = XMLSignatureFactory.getInstance("DOM");

			// Create a Reference to the enveloped document (in this case,
			// you are signing the whole document, so a URI of "" signifies
			// that, and also specify the SHA256 digest algorithm and
			// the ENVELOPED Transform.
			var digestMethod = xmlSignatureFactory.newDigestMethod(DigestMethod.SHA256, null);
			var transform = xmlSignatureFactory.newTransform(Transform.ENVELOPED, (TransformParameterSpec) null);
			var ref = xmlSignatureFactory.newReference(uriRef, digestMethod, singletonList(transform), null, null);

			// Create the SignedInfo.
			var canonicalizationMethod = xmlSignatureFactory.newCanonicalizationMethod(CanonicalizationMethod.EXCLUSIVE, (C14NMethodParameterSpec) null);
			var signatureMethod = xmlSignatureFactory.newSignatureMethod(SignatureMethod.RSA_SHA256, null);
			var signedInfo = xmlSignatureFactory.newSignedInfo(canonicalizationMethod, signatureMethod, singletonList(ref));

			// Create the KeyInfo containing the X509Data.
			var keyInfoFactory = xmlSignatureFactory.getKeyInfoFactory();
			var x509Content = new ArrayList<>();
			x509Content.add(certificateWithPrivateKey.certificate().getSubjectX500Principal().getName());
			x509Content.add(certificateWithPrivateKey.certificate());
			var x509Data = keyInfoFactory.newX509Data(x509Content);
			var keyInfo = keyInfoFactory.newKeyInfo(singletonList(x509Data));

			// Create a DOMSignContext and specify the RSA PrivateKey and
			// location of the resulting XMLSignature's parent element.
			var signContext = new DOMSignContext(certificateWithPrivateKey.privateKey(), doc.getDocumentElement());

			// Create the XMLSignature, but don't sign it yet.
			var signature = xmlSignatureFactory.newXMLSignature(signedInfo, keyInfo);

			// Marshal, generate, and sign the enveloped signature.
			signature.sign(signContext);

			return signature;
		} catch (Exception e) {
			throw Problem.builder()
				.withTitle("Couldn't sign XML document")
				.withStatus(INTERNAL_SERVER_ERROR)
				.build();
		}
	}

	private static void setId(final Document document) {
		var element = document.getDocumentElement();
		var ref = element.getAttribute("Id");

		if (!ref.isEmpty()) {
			var attr = element.getAttributeNode("Id");
			element.setIdAttributeNode(attr, true);
		}
	}

	static String findRsaSha256Or512AlgorithmUri(final String s) {
		var matcher = Pattern.compile(Pattern.quote("http://www.w3.org/2001/04/xmldsig-more#rsa-sha") + "(256|512)").matcher(s);
		String algoritm = null;
		if (matcher.find()) {
			algoritm = matcher.group();
		}
		return algoritm;
	}

	public static void checkJavaVersion6u18OrLaterSupportingRsaSha256And512(final String s) {
		var algURI = findRsaSha256Or512AlgorithmUri(s);
		if (null == algURI) {
			return;
		}
		var javaVersion = System.getProperty("java.version");
		var matcher = Pattern.compile("1\\.6\\.0_(\\d+)").matcher(javaVersion);
		if (matcher.matches()) {
			var updateVersion = Integer.parseInt(matcher.group(1));
			if (updateVersion < 18) {
				var javaVendor = System.getProperty("java.vendor");
				throw new UnsupportedOperationException("Algorithm \"" + algURI + "\" is not supported. Support for rsa-sha256 and rsa-sha512 was added in Java 1.6.0_18, but we are running " + javaVendor + " Java " + javaVersion);
			}
		}
	}
}
