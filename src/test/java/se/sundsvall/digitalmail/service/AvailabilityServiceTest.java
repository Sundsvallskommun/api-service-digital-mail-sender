package se.sundsvall.digitalmail.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.zalando.problem.Status.NOT_FOUND;
import static se.sundsvall.digitalmail.TestObjectFactory.ORGANIZATION_NUMBER;

import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.zalando.problem.ThrowableProblem;
import se.sundsvall.digitalmail.integration.skatteverket.MailboxDto;
import se.sundsvall.digitalmail.integration.skatteverket.reachable.ReachableIntegration;

@ExtendWith(MockitoExtension.class)
class AvailabilityServiceTest {

	@Mock
	private ReachableIntegration mockReachableIntegration;

	@InjectMocks
	private AvailabilityService availabilityService;

	private static final MailboxDto VALID_MAILBOX = new MailboxDto("someRecipient", "someServiceAddress", "someServiceName", true);
	private static final MailboxDto INVALID_MAILBOX = new MailboxDto("someRecipient", "someServiceAddress", "someServiceName", false);

	@AfterEach
	void tearDown() {
		verifyNoMoreInteractions(mockReachableIntegration);
	}

	@Test
	void testGetRecipientMailboxesAndCheckAvailability() {
		var mailboxes = List.of(VALID_MAILBOX, INVALID_MAILBOX);
		when(mockReachableIntegration.isReachable(anyList(), anyString())).thenReturn(mailboxes);

		final var response = availabilityService.getRecipientMailboxesAndCheckAvailability(List.of("personalNumber"), ORGANIZATION_NUMBER);

		assertThat(response).isEqualTo(mailboxes);

		verify(mockReachableIntegration).isReachable(anyList(), anyString());
	}

	@ParameterizedTest
	@MethodSource("invalidMailboxesProvider")
	void testNoValidMailboxesShouldThrowProblem(List<MailboxDto> invalidMailboxes) {
		when(mockReachableIntegration.isReachable(anyList(), anyString())).thenReturn(invalidMailboxes);

		assertThatExceptionOfType(ThrowableProblem.class)
			.isThrownBy(() -> availabilityService.getRecipientMailboxesAndCheckAvailability(List.of("personalNumber"), ORGANIZATION_NUMBER))
			.satisfies(problem -> {
				assertThat(problem.getStatus()).isEqualTo(NOT_FOUND);
				assertThat(problem.getTitle()).isEqualTo("Couldn't find any mailboxes");
				assertThat(problem.getDetail()).isEqualTo("No mailbox could be found for any of the given partyIds or the recipients doesn't allow the sender.");
			});

		verify(mockReachableIntegration).isReachable(anyList(), anyString());
	}

	public static Stream<Arguments> invalidMailboxesProvider() {
		return Stream.of(
			Arguments.of(List.of()),
			Arguments.of(List.of(INVALID_MAILBOX)),
			Arguments.of(List.of(INVALID_MAILBOX, INVALID_MAILBOX)));
	}
}
