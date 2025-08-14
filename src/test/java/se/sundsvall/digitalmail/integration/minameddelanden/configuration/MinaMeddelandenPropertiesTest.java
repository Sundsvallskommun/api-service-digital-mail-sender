package se.sundsvall.digitalmail.integration.minameddelanden.configuration;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import se.sundsvall.digitalmail.Application;

@SpringBootTest(classes = Application.class)
@ActiveProfiles("junit")
class MinaMeddelandenPropertiesTest {

	@Autowired
	private MinaMeddelandenProperties properties;

	@Test
	void testProperties() {
		assertThat(properties.supportedSuppliers()).containsExactly("kivra", "minmyndighetspost", "billo", "fortnox");
		assertThat(properties.recipientUrl()).isEqualTo("https://minameddelanden-url.com");
		assertThat(properties.connectTimeout()).isEqualTo(5);
		assertThat(properties.readTimeout()).isEqualTo(30);
		assertThat(properties.messageMaxSize()).isEqualTo(2097152);

		assertThat(properties.senders().getFirst()).satisfies(sender1 -> {
			assertThat(sender1.name()).isEqualTo("Sundsvalls Kommun");
			assertThat(sender1.id()).isEqualTo("12345");
			assertThat(sender1.alias()).isEqualTo("skatteverket");
			assertThat(sender1.keyStoreAsBase64()).isNotNull().isBase64();
			assertThat(sender1.keyStorePassword()).isEqualTo("changeit");
		});

		assertThat(properties.senders().getLast()).satisfies(sender2 -> {
			assertThat(sender2.name()).isEqualTo("Kommun Sundsvalls");
			assertThat(sender2.id()).isEqualTo("54321");
			assertThat(sender2.alias()).isEqualTo("skatteverket");
			assertThat(sender2.keyStoreAsBase64()).isNotNull().isBase64();
			assertThat(sender2.keyStorePassword()).isEqualTo("changeit");
		});
	}
}
