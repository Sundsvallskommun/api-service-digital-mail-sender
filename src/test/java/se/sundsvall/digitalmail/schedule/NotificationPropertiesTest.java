package se.sundsvall.digitalmail.schedule;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import se.sundsvall.digitalmail.Application;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = Application.class)
@ActiveProfiles("junit")
class NotificationPropertiesTest {

	@Autowired
	private NotificationProperties properties;

	@Test
	void testProperties() {
		assertThat(properties.mail()).isNotNull().satisfies(mail -> {
			assertThat(mail.subject()).isEqualTo("someMailSubject");
			assertThat(mail.sender()).isNotNull();
			assertThat(mail.sender().emailAddress()).isEqualTo("some.sender@address.com");
			assertThat(mail.sender().name()).isEqualTo("someSender");
			assertThat(mail.recipients()).isNotNull();
			assertThat(mail.recipients()).isNotNull().containsExactly("some.recipient@address.com");
		});

		assertThat(properties.slack()).isNotNull().satisfies(slack -> {
			assertThat(slack.channel()).isEqualTo("someSlackChannel");
			assertThat(slack.message()).isEqualTo("someSlackMessage");
			assertThat(slack.token()).isEqualTo("someSlackToken");
		});
	}
}
