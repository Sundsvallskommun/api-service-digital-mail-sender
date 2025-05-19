package se.sundsvall.digitalmail.schedule;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.springframework.test.util.ReflectionTestUtils.setField;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.client.ClientAuthorizationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import se.sundsvall.dept44.scheduling.health.Dept44HealthUtility;
import se.sundsvall.digitalmail.integration.kivra.KivraIntegration;

@ExtendWith(MockitoExtension.class)
class CertificateHealthSchedulerTests {
	private static final String SCHEDULER_NAME = "certificate-health";

	@Mock
	private KivraIntegration kivraIntegrationMock;

	@Mock
	private Dept44HealthUtility dept44HealthUtilityMock;

	@InjectMocks
	private CertificateHealthScheduler scheduler;

	@BeforeEach
	void setup() {
		setField(scheduler, "schedulerName", SCHEDULER_NAME);
	}

	@AfterEach
	void verifyNoMoreMockInteractions() {
		verifyNoMoreInteractions(kivraIntegrationMock, dept44HealthUtilityMock);
	}

	@Test
	void testCertificateValid() {

		scheduler.execute();

		verify(kivraIntegrationMock).healthCheck();
		verify(dept44HealthUtilityMock).setHealthIndicatorHealthy(SCHEDULER_NAME);
	}

	@Test
	void testCertificateInvalid() {
		doThrow(new ClientAuthorizationException(new OAuth2Error("401"), "regid", "some prefix [invalid_token_response] some suffix")).when(kivraIntegrationMock).healthCheck();

		scheduler.execute();

		verify(kivraIntegrationMock).healthCheck();
		verify(dept44HealthUtilityMock).setHealthIndicatorUnhealthy(SCHEDULER_NAME, "A potential certificate issue has been detected and needs to be investigated");
	}

	@Test
	void testOnlySendSlackAndEmailOnFirstInvalidCheck() {
		doThrow(new ClientAuthorizationException(new OAuth2Error("401"), "regid", "some prefix [invalid_token_response] some suffix")).when(kivraIntegrationMock).healthCheck();

		scheduler.execute();
		scheduler.execute();

		// TODO: This test will be extended when the messaging integration is implemented

		verify(kivraIntegrationMock, times(2)).healthCheck();
		verify(dept44HealthUtilityMock, times(2)).setHealthIndicatorUnhealthy(SCHEDULER_NAME, "A potential certificate issue has been detected and needs to be investigated");
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
