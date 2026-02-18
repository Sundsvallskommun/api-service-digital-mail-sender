package se.sundsvall.digitalmail.integration.messaging;

import generated.se.sundsvall.messaging.EmailRequest;
import generated.se.sundsvall.messaging.EmailSender;
import generated.se.sundsvall.messaging.SlackRequest;

import static org.apache.commons.lang3.ObjectUtils.anyNull;

public final class MessagingMapper {

	private MessagingMapper() {
		// private constructor
	}

	public static EmailRequest toEmailRequest(final String emailAddress, final String subject, final String textMessage, final String senderEmail, final String senderName) {
		if (anyNull(emailAddress, subject, textMessage, senderEmail, senderName)) {
			return null;
		}

		return new EmailRequest()
			.subject(subject)
			.emailAddress(emailAddress)
			.message(textMessage)
			.sender(new EmailSender()
				.address(senderEmail)
				.name(senderName));
	}

	public static SlackRequest toSlackRequest(final String channel, final String message, final String token) {
		if (anyNull(channel, message, token)) {
			return null;
		}

		return new SlackRequest()
			.channel(channel)
			.message(message)
			.token(token);
	}
}
