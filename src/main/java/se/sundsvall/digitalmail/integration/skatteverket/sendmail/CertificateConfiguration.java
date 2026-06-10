package se.sundsvall.digitalmail.integration.skatteverket.sendmail;

import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableEntryException;
import java.security.cert.X509Certificate;
import lombok.extern.slf4j.Slf4j;
import org.jose4j.base64url.Base64;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import se.gov.minameddelanden.common.X509CertificateWithPrivateKey;
import se.sundsvall.dept44.problem.Problem;
import se.sundsvall.dept44.util.KeyStoreUtils;
import se.sundsvall.digitalmail.integration.skatteverket.SkatteverketProperties;

import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;

@Configuration
@Slf4j
class CertificateConfiguration {

	static final String SKATTEVERKET_CERT_NAME = "skatteverket";

	/**
	 * Loads the signing keystore and extracts the Skatteverket certificate together with its private key. Exposed as a bean
	 * so that {@link DigitalMailMapper} can be constructed without performing any keystore I/O itself.
	 *
	 * @param  properties the Skatteverket integration properties holding the base64-encoded keystore and its password
	 * @return            the certificate and private key used to sign secure deliveries
	 */
	@Bean
	X509CertificateWithPrivateKey skatteverketSigningCertificate(final SkatteverketProperties properties)
		throws KeyStoreException, UnrecoverableEntryException, NoSuchAlgorithmException {
		final var keyStore = KeyStoreUtils.loadKeyStore(Base64.decode(properties.keyStoreAsBase64()), properties.keyStorePassword());

		final var privateKeyEntry = (KeyStore.PrivateKeyEntry) keyStore.getEntry(
			getAliasFromKeystore(keyStore, SKATTEVERKET_CERT_NAME),
			new KeyStore.PasswordProtection(properties.keyStorePassword().toCharArray()));
		final var certificate = (X509Certificate) privateKeyEntry.getCertificate();

		return new X509CertificateWithPrivateKey(certificate, privateKeyEntry.getPrivateKey());
	}

	/**
	 * Retrieve the alias for the key from the keystore. As we only have one key, we get the first one; if we need to get
	 * more, we need to find it by alias.
	 *
	 * @param  keyStore          the keystore to search in
	 * @param  wantedAlias       the alias we want to find
	 * @return                   the alias of the key in the keystore
	 * @throws KeyStoreException if the keystore cannot be accessed
	 */
	static String getAliasFromKeystore(final KeyStore keyStore, final String wantedAlias) throws KeyStoreException {
		final var aliases = keyStore.aliases();

		var alias = "";
		var foundAlias = false;

		// Find the aliases and stop when we get the one we want.
		while (aliases.hasMoreElements()) {
			alias = aliases.nextElement();

			if (alias.equalsIgnoreCase(wantedAlias)) {
				foundAlias = true;
				log.info("Found keystore-entry with alias: {}", alias);
				break;
			}
		}

		if (foundAlias) {
			return alias;
		}

		throw Problem.builder()
			.withTitle("Couldn't find certificate for " + wantedAlias)
			.withStatus(INTERNAL_SERVER_ERROR)
			.build();
	}
}
