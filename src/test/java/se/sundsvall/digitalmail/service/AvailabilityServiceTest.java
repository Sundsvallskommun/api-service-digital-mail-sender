package se.sundsvall.digitalmail.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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

	@Test
	void testPresentMailbox_shouldReturnMailbox() {
		when(mockReachableIntegration.isReachable(any())).thenReturn(List.of(new MailboxDto("someRecipientId", "someServiceAddress", "someServiceName")));

		final var mailboxes = availabilityService.getRecipientMailboxesAndCheckAvailability(List.of("personalNumber"));

		assertThat(mailboxes).isNotEmpty();
	}

	@Test
	void testEmptyOptionalMailbox_shouldOnlyReturnPresentMailboxes() {
		final var response = Stream.concat(generateEmptyMailboxResponse().stream(), generatePresentMailboxResponse().stream()).toList();

		when(mockReachableIntegration.isReachable(any())).thenReturn(response);

		final var mailboxes = availabilityService.getRecipientMailboxesAndCheckAvailability(List.of("personalNumber"));

		assertThat(mailboxes).isNotEmpty().singleElement().isNotNull();
	}

	@Test
	void testNoMailbox_shouldThrowException() {
		when(mockReachableIntegration.isReachable(any())).thenReturn(List.of());

		final var personalNumbers = List.of("personalNumber");
		assertThatExceptionOfType(ThrowableProblem.class)
			.isThrownBy(() -> availabilityService.getRecipientMailboxesAndCheckAvailability(personalNumbers));
	}

	private List<MailboxDto> generateEmptyMailboxResponse() {
		return List.of();
	}

	private List<MailboxDto> generatePresentMailboxResponse() {
		final var mailbox = new MailboxDto("recipientId", "serviceAddress", "serviceName");

		return List.of(mailbox);
	}
}
