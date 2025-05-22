package se.sundsvall.digitalmail.schedule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.util.ReflectionTestUtils.setField;

import generated.se.sundsvall.messaging.EmailRequest;
import generated.se.sundsvall.messaging.SlackRequest;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.client.ClientAuthorizationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import se.sundsvall.dept44.scheduling.health.Dept44HealthUtility;
import se.sundsvall.digitalmail.integration.kivra.KivraIntegration;
import se.sundsvall.digitalmail.integration.messaging.MessagingIntegration;
import se.sundsvall.digitalmail.schedule.NotificationProperties.Mail;
import se.sundsvall.digitalmail.schedule.NotificationProperties.Sender;
import se.sundsvall.digitalmail.schedule.NotificationProperties.Slack;

@ExtendWith(MockitoExtension.class)
class CertificateHealthSchedulerTests {
	private static final String SCHEDULER_NAME = "certificate-health";

	@Mock
	private NotificationProperties notificationPropertiesMock;

	@Mock
	private MessagingIntegration messagingIntegrationmock;

	@Mock
	private KivraIntegration kivraIntegrationMock;

	@Mock
	private Mail mailMock;

	@Mock
	private Sender senderMock;

	@Mock
	private Slack slackMock;

	@Mock
	private Dept44HealthUtility dept44HealthUtilityMock;

	@InjectMocks
	private CertificateHealthScheduler scheduler;

	@Captor
	private ArgumentCaptor<EmailRequest> emailRequestCaptor;

	@Captor
	private ArgumentCaptor<SlackRequest> slackRequestCaptor;

	@BeforeEach
	void setup() {
		setField(scheduler, "schedulerName", SCHEDULER_NAME);
	}

	@AfterEach
	void verifyNoMoreMockInteractions() {
		verifyNoMoreInteractions(notificationPropertiesMock, messagingIntegrationmock, kivraIntegrationMock, dept44HealthUtilityMock);
	}

	@Test
	void testCertificateValid() {

		scheduler.execute();

		verify(kivraIntegrationMock).healthCheck();
		verify(dept44HealthUtilityMock).setHealthIndicatorHealthy(SCHEDULER_NAME);
	}

	@Test
	void testCertificateInvalidAndNoNotificationReceiversEnabled() {
		doThrow(new ClientAuthorizationException(new OAuth2Error("401"), "regid", "some prefix [invalid_token_response] some suffix")).when(kivraIntegrationMock).healthCheck();

		scheduler.execute();

		verify(kivraIntegrationMock).healthCheck();
		verify(dept44HealthUtilityMock).setHealthIndicatorUnhealthy(SCHEDULER_NAME, "A potential certificate issue has been detected and needs to be investigated");
		verify(notificationPropertiesMock).mail();
		verify(notificationPropertiesMock).slack();
	}

	@Test
	void testCertificateInvalidAndEmailEnabled() {
		final var emailAddress = "emailAddress";
		final var name = "name";
		final var subject = "subject";
		final var recipient1 = "recipient1";
		final var recipient2 = "recipient2";

		when(notificationPropertiesMock.mail()).thenReturn(mailMock);
		when(mailMock.sender()).thenReturn(senderMock);
		when(senderMock.emailAddress()).thenReturn(emailAddress);
		when(senderMock.name()).thenReturn(name);
		when(mailMock.subject()).thenReturn(subject);
		when(mailMock.recipients()).thenReturn(List.of(recipient1, recipient2));
		doThrow(new ClientAuthorizationException(new OAuth2Error("401"), "regid", "some prefix [invalid_token_response] some suffix")).when(kivraIntegrationMock).healthCheck();

		scheduler.execute();

		verify(kivraIntegrationMock).healthCheck();
		verify(dept44HealthUtilityMock).setHealthIndicatorUnhealthy(SCHEDULER_NAME, "A potential certificate issue has been detected and needs to be investigated");
		verify(notificationPropertiesMock, times(8)).mail();
		verify(notificationPropertiesMock).slack();
		verify(mailMock).recipients();
		verify(mailMock, times(2)).subject();
		verify(mailMock, times(4)).sender();
		verify(senderMock, times(2)).emailAddress();
		verify(senderMock, times(2)).name();
		verify(messagingIntegrationmock, times(2)).sendEmail(eq("2281"), emailRequestCaptor.capture());

		assertThat(emailRequestCaptor.getAllValues()).hasSize(2)
			.allSatisfy(request -> {
				assertThat(request.getAttachments()).isNullOrEmpty();
				assertThat(request.getHeaders()).isNullOrEmpty();
				assertThat(request.getHtmlMessage()).isNull();
				assertThat(request.getMessage()).isNotBlank();
				assertThat(request.getParty()).isNull();
				assertThat(request.getSender()).isNotNull();
				assertThat(request.getSender().getAddress()).isEqualTo(emailAddress);
				assertThat(request.getSender().getName()).isEqualTo(name);
				assertThat(request.getSender().getReplyTo()).isNull();
			})
			.satisfiesExactlyInAnyOrder(request -> {
				assertThat(request.getEmailAddress()).isEqualTo(recipient1);
			}, request -> {
				assertThat(request.getEmailAddress()).isEqualTo(recipient2);
			});
	}

	@Test
	void testWhenMappedMailRequestIsNull() {
		when(notificationPropertiesMock.mail()).thenReturn(mailMock);
		doThrow(new ClientAuthorizationException(new OAuth2Error("401"), "regid", "some prefix [invalid_token_response] some suffix")).when(kivraIntegrationMock).healthCheck();

		scheduler.execute();

		verify(kivraIntegrationMock).healthCheck();
		verify(dept44HealthUtilityMock).setHealthIndicatorUnhealthy(SCHEDULER_NAME, "A potential certificate issue has been detected and needs to be investigated");
		verify(notificationPropertiesMock, times(2)).mail();
		verify(notificationPropertiesMock).slack();
	}

	@Test
	void testCertificateInvalidAndSlackEnabled() {
		final var channel = "channel";
		final var message = "message";
		final var token = "token";

		when(notificationPropertiesMock.slack()).thenReturn(slackMock);
		when(slackMock.channel()).thenReturn(channel);
		when(slackMock.message()).thenReturn(message);
		when(slackMock.token()).thenReturn(token);
		doThrow(new ClientAuthorizationException(new OAuth2Error("401"), "regid", "some prefix [invalid_token_response] some suffix")).when(kivraIntegrationMock).healthCheck();

		scheduler.execute();

		verify(kivraIntegrationMock).healthCheck();
		verify(dept44HealthUtilityMock).setHealthIndicatorUnhealthy(SCHEDULER_NAME, "A potential certificate issue has been detected and needs to be investigated");
		verify(notificationPropertiesMock).mail();
		verify(notificationPropertiesMock, times(4)).slack();
		verify(slackMock).channel();
		verify(slackMock).message();
		verify(slackMock).token();
		verify(messagingIntegrationmock).sendSlack(eq("2281"), slackRequestCaptor.capture());

		assertThat(slackRequestCaptor.getValue().getChannel()).isEqualTo(channel);
		assertThat(slackRequestCaptor.getValue().getMessage()).isEqualTo(message);
		assertThat(slackRequestCaptor.getValue().getToken()).isEqualTo(token);
	}

	@Test
	void testWhenMappedSlackRequestIsNull() {
		when(notificationPropertiesMock.slack()).thenReturn(slackMock);
		doThrow(new ClientAuthorizationException(new OAuth2Error("401"), "regid", "some prefix [invalid_token_response] some suffix")).when(kivraIntegrationMock).healthCheck();

		scheduler.execute();

		verify(kivraIntegrationMock).healthCheck();
		verify(dept44HealthUtilityMock).setHealthIndicatorUnhealthy(SCHEDULER_NAME, "A potential certificate issue has been detected and needs to be investigated");
		verify(notificationPropertiesMock).mail();
		verify(notificationPropertiesMock, times(4)).slack();

	}

	@Test
	void testOnlySendSlackAndEmailOnFirstInvalidCheck() {
		final var municipalityId = "2281";
		final var value = "value";
		when(notificationPropertiesMock.slack()).thenReturn(slackMock);
		when(slackMock.channel()).thenReturn(value);
		when(slackMock.message()).thenReturn(value);
		when(slackMock.token()).thenReturn(value);
		when(notificationPropertiesMock.mail()).thenReturn(mailMock);
		when(mailMock.subject()).thenReturn(value);
		when(mailMock.recipients()).thenReturn(List.of(value));
		when(mailMock.sender()).thenReturn(senderMock);
		when(senderMock.emailAddress()).thenReturn(value);
		when(senderMock.name()).thenReturn(value);

		doThrow(new ClientAuthorizationException(new OAuth2Error("401"), "regid", "some prefix [invalid_token_response] some suffix")).when(kivraIntegrationMock).healthCheck();

		scheduler.execute();
		scheduler.execute();

		verify(kivraIntegrationMock, times(2)).healthCheck();
		verify(notificationPropertiesMock, times(5)).mail();
		verify(notificationPropertiesMock, times(4)).slack();
		verify(dept44HealthUtilityMock, times(2)).setHealthIndicatorUnhealthy(SCHEDULER_NAME, "A potential certificate issue has been detected and needs to be investigated");
		verify(messagingIntegrationmock).sendEmail(eq(municipalityId), any());
		verify(messagingIntegrationmock).sendSlack(eq(municipalityId), any());
	}

	@ParameterizedTest
	@ValueSource(strings = {
		"message"
	})
	@NullSource
	void testOtherException(String message) {
		doThrow(new RuntimeException(message)).when(kivraIntegrationMock).healthCheck();

		scheduler.execute();

		verify(kivraIntegrationMock).healthCheck();

	}
}
