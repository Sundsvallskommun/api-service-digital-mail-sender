package se.sundsvall.digitalmail.integration.messaging;

import generated.se.sundsvall.messaging.EmailRequest;
import generated.se.sundsvall.messaging.SlackRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class MessagingIntegration {

	private final MessagingClient messagingClient;

	public void sendEmail(final String municipalityId, final EmailRequest request) {
		try {
			messagingClient.sendEmail(municipalityId, request);
		} catch (final Exception e) {
			log.warn("Error when sending email", e);
		}
	}

	public void sendSlack(final String municipalityId, final SlackRequest request) {
		try {
			messagingClient.sendSlackMessage(municipalityId, request);
		} catch (final Exception e) {
			log.warn("Error when sending slack message", e);
		}
	}
}
