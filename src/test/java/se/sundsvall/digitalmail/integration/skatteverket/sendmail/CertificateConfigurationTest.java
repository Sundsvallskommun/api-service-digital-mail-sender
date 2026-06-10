package se.sundsvall.digitalmail.integration.skatteverket.sendmail;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.dept44.problem.ThrowableProblem;
import se.sundsvall.digitalmail.SkatteverketTestKeystore;
import se.sundsvall.digitalmail.integration.skatteverket.SkatteverketProperties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CertificateConfigurationTest {

	@Mock
	private SkatteverketProperties mockProperties;

	@Test
	void signingCertificateBeanIsCreatedFromKeystore() throws Exception {
		when(mockProperties.keyStoreAsBase64()).thenReturn(SkatteverketTestKeystore.base64());
		when(mockProperties.keyStorePassword()).thenReturn(SkatteverketTestKeystore.PASSWORD);

		// Building the bean loads the keystore and extracts the signing entry, so a non-null certificate and private key
		// proves the whole CertificateConfiguration wiring works.
		final var signingCertificate = new CertificateConfiguration().skatteverketSigningCertificate(mockProperties);

		assertThat(signingCertificate).isNotNull();
		assertThat(signingCertificate.certificate()).isNotNull();
		assertThat(signingCertificate.privateKey()).isNotNull();
	}

	@Test
	void getAliasFromKeystore() throws Exception {
		final var alias = CertificateConfiguration.getAliasFromKeystore(SkatteverketTestKeystore.load(), CertificateConfiguration.SKATTEVERKET_CERT_NAME);

		assertThat(alias).isEqualTo("skatteverket");
	}

	@Test
	void getAliasFromKeystoreThrowsWhenNotFound() {
		final var keyStore = SkatteverketTestKeystore.load();

		assertThatExceptionOfType(ThrowableProblem.class)
			.isThrownBy(() -> CertificateConfiguration.getAliasFromKeystore(keyStore, "notFound"))
			.withMessage("Couldn't find certificate for notFound");
	}
}
