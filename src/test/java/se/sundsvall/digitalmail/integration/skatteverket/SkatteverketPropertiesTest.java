package se.sundsvall.digitalmail.integration.skatteverket;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import se.sundsvall.digitalmail.Application;

@SpringBootTest(classes = Application.class)
@ActiveProfiles("junit")
class SkatteverketPropertiesTest {

	@Autowired
	private SkatteverketProperties properties;

	@Test
	void testProperties() {
		assertThat(properties.recipientUrl()).isEqualTo("https://skatteverket-url.com");
		assertThat(properties.connectTimeout()).isEqualTo(5);
		assertThat(properties.readTimeout()).isEqualTo(6);
		assertThat(properties.shouldUseKeystore()).isTrue();
		assertThat(properties.keyStoreAsBase64()).isBase64();
		assertThat(properties.keyStorePassword()).isEqualTo("changeit");
	}
}
