package se.sundsvall.digitalmail.integration.messaging;

import generated.se.sundsvall.messaging.EmailRequest;
import generated.se.sundsvall.messaging.SlackRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MessagingIntegrationTest {

	private static final String MUNICIPALITY_ID = "municipalityId";

	@Mock
	private MessagingClient messagingClientMock;

	@Mock
	private EmailRequest emailRequestMock;

	@Mock
	private SlackRequest slackRequestMock;

	@InjectMocks
	private MessagingIntegration messagingIntegration;

	@AfterEach
	void verifyNoMoreMockInteractions() {
		verifyNoMoreInteractions(messagingClientMock, emailRequestMock, slackRequestMock);
	}

	@Test
	void sendEmail() {
		// Act
		messagingIntegration.sendEmail(MUNICIPALITY_ID, emailRequestMock);

		// Assert and verify
		verify(messagingClientMock).sendEmail(MUNICIPALITY_ID, emailRequestMock);
	}

	@Test
	void sendEmail_serverError() {
		// Arrange
		when(messagingClientMock.sendEmail(eq(MUNICIPALITY_ID), any())).thenThrow(new RuntimeException());

		// Act
		assertDoesNotThrow(() -> messagingIntegration.sendEmail(MUNICIPALITY_ID, emailRequestMock));

		// Assert and verify
		verify(messagingClientMock).sendEmail(MUNICIPALITY_ID, emailRequestMock);
	}

	@Test
	void sendSlack() {
		// Act
		messagingIntegration.sendSlack(MUNICIPALITY_ID, slackRequestMock);

		// Assert and verify
		verify(messagingClientMock).sendSlackMessage(MUNICIPALITY_ID, slackRequestMock);
	}

	@Test
	void sendSlack_serverError() {
		// Arrange
		when(messagingClientMock.sendSlackMessage(eq(MUNICIPALITY_ID), any())).thenThrow(new RuntimeException());

		// Act
		assertDoesNotThrow(() -> messagingIntegration.sendSlack(MUNICIPALITY_ID, slackRequestMock));

		// Assert and verify
		verify(messagingClientMock).sendSlackMessage(MUNICIPALITY_ID, slackRequestMock);
	}
}
