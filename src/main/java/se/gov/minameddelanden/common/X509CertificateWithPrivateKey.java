package se.gov.minameddelanden.common;

import java.security.PrivateKey;
import java.security.cert.X509Certificate;

public record X509CertificateWithPrivateKey(X509Certificate certificate, PrivateKey privateKey) {
}
