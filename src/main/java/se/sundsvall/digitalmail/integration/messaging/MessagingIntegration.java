package se.sundsvall.digitalmail.integration.messaging;

import generated.se.sundsvall.messaging.EmailRequest;
import generated.se.sundsvall.messaging.SlackRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class MessagingIntegration {

	private static final Logger LOGGER = LoggerFactory.getLogger(MessagingIntegration.class);

	private final MessagingClient messagingClient;

	public MessagingIntegration(final MessagingClient messagingClient) {
		this.messagingClient = messagingClient;
	}

	public void sendEmail(final String municipalityId, final EmailRequest request) {
		try {
			messagingClient.sendEmail(municipalityId, request);
		} catch (final Exception e) {
			LOGGER.warn("Error when sending email", e);
		}
	}

	public void sendSlack(final String municipalityId, final SlackRequest request) {
		try {
			messagingClient.sendSlackMessage(municipalityId, request);
		} catch (final Exception e) {
			LOGGER.warn("Error when sending slack message", e);
		}
	}
}
