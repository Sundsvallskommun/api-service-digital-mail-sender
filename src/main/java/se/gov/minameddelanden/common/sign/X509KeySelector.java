package se.gov.minameddelanden.common.sign;

import static org.zalando.problem.Status.INTERNAL_SERVER_ERROR;

import java.net.URI;
import java.net.URISyntaxException;
import java.security.cert.X509Certificate;

import javax.xml.crypto.AlgorithmMethod;
import javax.xml.crypto.KeySelector;
import javax.xml.crypto.KeySelectorException;
import javax.xml.crypto.KeySelectorResult;
import javax.xml.crypto.XMLCryptoContext;
import javax.xml.crypto.dsig.keyinfo.KeyInfo;
import javax.xml.crypto.dsig.keyinfo.X509Data;

import org.zalando.problem.Problem;

public class X509KeySelector extends KeySelector {

	@Override
    public KeySelectorResult select(final KeyInfo keyInfo, final Purpose purpose,
            final AlgorithmMethod method, final XMLCryptoContext context) throws KeySelectorException {
        for (var info : keyInfo.getContent()) {
            if (!(info instanceof X509Data x509Data)) {
                continue;
            }
            for (var o : x509Data.getContent()) {
                if (!(o instanceof X509Certificate)) {
                    continue;
                }
                var publicKey = ((X509Certificate) o).getPublicKey();
                // Make sure the algorithm is compatible
                // with the method.
                var methodAlgorithm = method.getAlgorithm();
                var keyAlgorithm = publicKey.getAlgorithm();
                if (algEquals(methodAlgorithm, keyAlgorithm)) {
                    return () -> publicKey;
                }
            }
        }
        
        throw new KeySelectorException("No key found!");
    }

    static boolean algEquals(String algURI, String algName) {
        try {
            SignatureUtils.checkJavaVersion6u18OrLaterSupportingRsaSha256And512(algURI);

            return new URI(algURI).getFragment().startsWith(algName.toLowerCase());
        } catch (URISyntaxException e) {
            throw Problem.builder()
                    .withTitle("Wrong URI format")
                    .withStatus(INTERNAL_SERVER_ERROR)
                    .build();
        }
    }
}
