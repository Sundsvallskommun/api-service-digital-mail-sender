package se.sundsvall.digitalmail.api.healthcheck;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.actuate.health.Health;

@ExtendWith(MockitoExtension.class)
class SenderHealthIndicatorTest {

	@Mock
	private Health.Builder mockHealthBuilder;

	@InjectMocks
	private SenderHealthIndicator healthIndicator;

	@Test
	void setHealthy() {
		healthIndicator = new SenderHealthIndicator();

		try (var mockStaticHealth = mockStatic(Health.class)) {
			mockStaticHealth.when(Health::up).thenReturn(mockHealthBuilder);

			when(mockHealthBuilder.withDetail(any(), any())).thenReturn(mockHealthBuilder);

			// Set healthy and fake that health() is called
			healthIndicator.setHealthy();

			verify(mockHealthBuilder).withDetail("Supported sender configuration", "Configuration present");
			verify(mockHealthBuilder).build();

			mockStaticHealth.verify(Health::up);
			mockStaticHealth.verifyNoMoreInteractions();

			verifyNoMoreInteractions(mockHealthBuilder);
		}
	}

	@Test
	void setUnhealthy() {
		healthIndicator = new SenderHealthIndicator();

		try (var mockStaticHealth = mockStatic(Health.class)) {
			mockStaticHealth.when(Health::down).thenReturn(mockHealthBuilder);

			when(mockHealthBuilder.withDetail(anyString(), anyString())).thenReturn(mockHealthBuilder);

			// Set unhealthy and fake that health() is called
			healthIndicator.setUnhealthy();

			verify(mockHealthBuilder).withDetail("Supported sender configuration", "No configuration present");
			verify(mockHealthBuilder).build();

			mockStaticHealth.verify(Health::down);
			mockStaticHealth.verifyNoMoreInteractions();

			verifyNoMoreInteractions(mockHealthBuilder);
		}
	}
}
