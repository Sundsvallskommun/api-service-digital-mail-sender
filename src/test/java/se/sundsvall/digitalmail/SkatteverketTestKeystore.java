package se.sundsvall.digitalmail;

import java.io.ByteArrayInputStream;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.util.Base64;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.io.ClassPathResource;
import se.gov.minameddelanden.common.X509CertificateWithPrivateKey;

/**
 * Loads the (dummy) Skatteverket signing keystore straight from {@code application-junit.yaml}, so unit tests can use
 * the
 * real keystore/certificate without booting Spring and without duplicating the keystore as a separate fixture.
 */
public final class SkatteverketTestKeystore {

	public static final String PASSWORD;

	private static final String CERT_ALIAS = "skatteverket";
	private static final String BASE64;

	static {
		final var yaml = new YamlPropertiesFactoryBean();
		yaml.setResources(new ClassPathResource("application-junit.yaml"));
		final var properties = yaml.getObject();
		BASE64 = properties.getProperty("integration.skatteverket.key-store-as-base64");
		PASSWORD = properties.getProperty("integration.skatteverket.key-store-password");
	}

	private SkatteverketTestKeystore() {}

	public static String base64() {
		return BASE64;
	}

	public static KeyStore load() {
		try {
			final var keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
			keyStore.load(new ByteArrayInputStream(Base64.getDecoder().decode(BASE64)), PASSWORD.toCharArray());
			return keyStore;
		} catch (final Exception e) {
			throw new IllegalStateException("Couldn't load the test keystore", e);
		}
	}

	public static X509CertificateWithPrivateKey signingCertificate() {
		try {
			final var entry = (KeyStore.PrivateKeyEntry) load().getEntry(
				CERT_ALIAS,
				new KeyStore.PasswordProtection(PASSWORD.toCharArray()));
			return new X509CertificateWithPrivateKey((X509Certificate) entry.getCertificate(), entry.getPrivateKey());
		} catch (final Exception e) {
			throw new IllegalStateException("Couldn't extract the signing certificate from the test keystore", e);
		}
	}
}
