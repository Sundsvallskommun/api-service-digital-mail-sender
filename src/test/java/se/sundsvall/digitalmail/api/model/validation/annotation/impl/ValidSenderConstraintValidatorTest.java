package se.sundsvall.digitalmail.api.model.validation.annotation.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import jakarta.validation.ConstraintValidatorContext;
import java.util.HashMap;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.digitalmail.api.healthcheck.SenderHealthIndicator;
import se.sundsvall.digitalmail.integration.skatteverket.SkatteverketProperties;

@ExtendWith(MockitoExtension.class)
class ValidSenderConstraintValidatorTest {

	@Mock
	private ConstraintValidatorContext mockContext;

	@Mock
	private SkatteverketProperties mockProperties;

	@Mock
	private SenderHealthIndicator mockHealthIndicator;

	@InjectMocks
	private ValidSenderConstraintValidator validator;

	public static Stream<Arguments> sendersProvider() {
		return Stream.of(
			// value, expected
			Arguments.of("1234567890", true),
			Arguments.of("0987654321", false),
			Arguments.of(null, false),
			Arguments.of("", false),
			Arguments.of(" ", false));
	}

	@ParameterizedTest
	@MethodSource("sendersProvider")
	void testValidSenders(String organizationNumber, boolean expected) {
		var supportedSenders = new HashMap<String, String>();
		supportedSenders.put("1234567890", "Some Sender");

		when(mockProperties.supportedSenders()).thenReturn(supportedSenders);

		assertThat(validator.isValid(organizationNumber, mockContext)).isEqualTo(expected);

		verify(mockProperties).supportedSenders();
		verify(mockHealthIndicator).setHealthy();

		verifyNoMoreInteractions(mockProperties, mockHealthIndicator);
	}

	@Test
	void testEmptyPropertiesShouldSetUnhealthy() {
		when(mockProperties.supportedSenders()).thenReturn(null);

		assertThat(validator.isValid("someOrganization", mockContext)).isFalse();

		verify(mockProperties).supportedSenders();
		verify(mockHealthIndicator).setUnhealthy();

		verifyNoMoreInteractions(mockProperties, mockHealthIndicator);
	}
}
