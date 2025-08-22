package se.sundsvall.digitalmail.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static se.sundsvall.digitalmail.TestObjectFactory.ORGANIZATION_NUMBER;

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
		when(mockReachableIntegration.isReachable(anyList(), anyString())).thenReturn(List.of(new MailboxDto("someRecipientId", "someServiceAddress", "someServiceName", true)));

		final var mailboxes = availabilityService.getRecipientMailboxesAndCheckAvailability(List.of("personalNumber"), ORGANIZATION_NUMBER);

		assertThat(mailboxes).isNotEmpty();
	}

	@Test
	void testEmptyOptionalMailbox_shouldOnlyReturnPresentMailboxes() {
		final var response = Stream.concat(generateEmptyMailboxResponse().stream(), generatePresentMailboxResponse().stream()).toList();

		when(mockReachableIntegration.isReachable(any(), anyString())).thenReturn(response);

		final var mailboxes = availabilityService.getRecipientMailboxesAndCheckAvailability(List.of("personalNumber"), ORGANIZATION_NUMBER);

		assertThat(mailboxes).isNotEmpty().singleElement().isNotNull();
	}

	@Test
	void testNoMailbox_shouldThrowException() {
		when(mockReachableIntegration.isReachable(any(), anyString())).thenReturn(List.of());

		final var personalNumbers = List.of("personalNumber");
		assertThatExceptionOfType(ThrowableProblem.class)
			.isThrownBy(() -> availabilityService.getRecipientMailboxesAndCheckAvailability(personalNumbers, ORGANIZATION_NUMBER));
	}

	private List<MailboxDto> generateEmptyMailboxResponse() {
		return List.of();
	}

	private List<MailboxDto> generatePresentMailboxResponse() {
		final var mailbox = new MailboxDto("recipientId", "serviceAddress", "serviceName", true);

		return List.of(mailbox);
	}
}
