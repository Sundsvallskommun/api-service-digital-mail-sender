package se.sundsvall.digitalmail.integration.messaging;

import static java.time.temporal.ChronoUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import se.sundsvall.digitalmail.Application;

@SpringBootTest(classes = Application.class)
@ActiveProfiles("junit")
class MessagingPropertiesTest {

	@Autowired
	private MessagingProperties properties;

	@Test
	void testProperties() {
		assertThat(properties.apiUrl()).isEqualTo("http://messaging-url.com");
		assertThat(properties.connectTimeout()).isEqualTo(Duration.of(1, SECONDS));
		assertThat(properties.readTimeout()).isEqualTo(Duration.of(2, SECONDS));
		assertThat(properties.oauth2()).isNotNull().satisfies(oauth2 -> {
			assertThat(oauth2.clientId()).isEqualTo("some-client-id");
			assertThat(oauth2.clientSecret()).isEqualTo("some-client-secret");
			assertThat(oauth2.tokenUrl()).isEqualTo("http://token-url.com");
		});
	}
}
