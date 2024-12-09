package se.sundsvall.digitalmail;

import java.security.Key;
import java.security.KeyException;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.util.List;
import javax.xml.crypto.AlgorithmMethod;
import javax.xml.crypto.KeySelector;
import javax.xml.crypto.KeySelectorException;
import javax.xml.crypto.KeySelectorResult;
import javax.xml.crypto.XMLCryptoContext;
import javax.xml.crypto.XMLStructure;
import javax.xml.crypto.dsig.SignatureMethod;
import javax.xml.crypto.dsig.keyinfo.KeyInfo;
import javax.xml.crypto.dsig.keyinfo.KeyValue;
import javax.xml.crypto.dsig.keyinfo.X509Data;

public class KeyValueKeySelector extends KeySelector {
	static boolean algEquals(final String algURI, final String algName) {
		if (algName.equalsIgnoreCase("DSA") && algURI.equalsIgnoreCase(SignatureMethod.DSA_SHA1)) {
			return true;
		} else {
			return algName.equalsIgnoreCase("RSA") && algURI.equalsIgnoreCase(SignatureMethod.RSA_SHA1);
		}
	}

	@Override
	public KeySelectorResult select(final KeyInfo keyInfo, final KeySelector.Purpose purpose, final AlgorithmMethod method, final XMLCryptoContext context) throws KeySelectorException {

		if (keyInfo == null) {
			throw new KeySelectorException("Null KeyInfo object!");
		}
		final SignatureMethod sm = (SignatureMethod) method;
		final List<?> list = keyInfo.getContent();

		for (int i = 0; i < list.size(); i++) {
			final XMLStructure xmlStructure = (XMLStructure) list.get(i);
			PublicKey pk = null;
			if (xmlStructure instanceof KeyValue) {
				try {
					pk = ((KeyValue) xmlStructure).getPublicKey();
				} catch (final KeyException ke) {
					throw new KeySelectorException(ke);
				}
				// make sure algorithm is compatible with method
				if (algEquals(sm.getAlgorithm(), pk.getAlgorithm())) {
					return new SimpleKeySelectorResult(pk);
				}
			} else if (xmlStructure instanceof X509Data) {
				for (final Object data : ((X509Data) xmlStructure).getContent()) {
					if (data instanceof X509Certificate) {
						pk = ((X509Certificate) data).getPublicKey();
					}
				}
				// make sure algorithm is compatible with method
				if (algEquals(sm.getAlgorithm(), pk.getAlgorithm())) {
					return new SimpleKeySelectorResult(pk);
				}
			}
		}
		throw new KeySelectorException("No KeyValue element found!");
	}

	private static class SimpleKeySelectorResult implements KeySelectorResult {

		private final PublicKey pk;

		SimpleKeySelectorResult(final PublicKey pk) {
			this.pk = pk;
		}

		@Override
		public Key getKey() {
			return pk;
		}
	}
}
