package se.sundsvall.digitalmail.integration.messaging;

import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThat;

class MessagingMapperTest {
	private static final String EMAIL_ADDRESS = "emailAddress";
	private static final String SUBJECT = "subject";
	private static final String MESSAGE = "message";
	private static final String SENDER_EMAIL = "senderEmail";
	private static final String SENDER_NAME = "senderName";
	private static final String CHANNEL = "channel";
	private static final String TOKEN = "token";

	private static Stream<Arguments> emailNullParameterProvider() {
		return Stream.of(
			Arguments.of(null, SUBJECT, MESSAGE, SENDER_EMAIL, SENDER_NAME),
			Arguments.of(EMAIL_ADDRESS, null, MESSAGE, SENDER_EMAIL, SENDER_NAME),
			Arguments.of(EMAIL_ADDRESS, SUBJECT, null, SENDER_EMAIL, SENDER_NAME),
			Arguments.of(EMAIL_ADDRESS, SUBJECT, MESSAGE, null, SENDER_NAME),
			Arguments.of(EMAIL_ADDRESS, SUBJECT, MESSAGE, SENDER_EMAIL, null));
	}

	@ParameterizedTest
	@MethodSource("emailNullParameterProvider")
	void toEmailRequestWhenAnyParameterNull(String emailAddress, String subject, String textMessage, String senderEmail, String senderName) {
		assertThat(MessagingMapper.toEmailRequest(emailAddress, subject, textMessage, senderEmail, senderName)).isNull();
	}

	@Test
	void toEmailRequest() {
		final var bean = MessagingMapper.toEmailRequest(EMAIL_ADDRESS, SUBJECT, MESSAGE, SENDER_EMAIL, SENDER_NAME);

		assertThat(bean).isNotNull();
		assertThat(bean.getAttachments()).isNullOrEmpty();
		assertThat(bean.getEmailAddress()).isEqualTo(EMAIL_ADDRESS);
		assertThat(bean.getHeaders()).isNullOrEmpty();
		assertThat(bean.getHtmlMessage()).isNull();
		assertThat(bean.getMessage()).isEqualTo(MESSAGE);
		assertThat(bean.getParty()).isNull();
		assertThat(bean.getSubject()).isEqualTo(SUBJECT);
		assertThat(bean.getSender()).isNotNull().satisfies(sender -> {
			assertThat(sender.getAddress()).isEqualTo(SENDER_EMAIL);
			assertThat(sender.getName()).isEqualTo(SENDER_NAME);
			assertThat(sender.getReplyTo()).isNull();
		});
	}

	private static Stream<Arguments> slackNullParameterProvider() {
		return Stream.of(
			Arguments.of(null, MESSAGE, TOKEN),
			Arguments.of(CHANNEL, null, TOKEN),
			Arguments.of(CHANNEL, MESSAGE, null));
	}

	@ParameterizedTest
	@MethodSource("slackNullParameterProvider")
	void toSlackRequestWhenAnyParameterNull(String channel, String message, String token) {
		assertThat(MessagingMapper.toSlackRequest(channel, message, token)).isNull();
	}

	@Test
	void toSlackRequest() {
		final var bean = MessagingMapper.toSlackRequest(CHANNEL, MESSAGE, TOKEN);

		assertThat(bean).isNotNull();
		assertThat(bean.getChannel()).isEqualTo(CHANNEL);
		assertThat(bean.getMessage()).isEqualTo(MESSAGE);
		assertThat(bean.getToken()).isEqualTo(TOKEN);

	}
}
