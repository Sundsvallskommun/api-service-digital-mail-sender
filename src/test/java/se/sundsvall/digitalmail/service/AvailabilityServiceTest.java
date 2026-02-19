package se.sundsvall.digitalmail.service;

import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.digitalmail.integration.skatteverket.MailboxDto;
import se.sundsvall.digitalmail.integration.skatteverket.reachable.ReachableIntegration;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static se.sundsvall.digitalmail.TestObjectFactory.ORGANIZATION_NUMBER;

@ExtendWith(MockitoExtension.class)
class AvailabilityServiceTest {

	@Mock
	private ReachableIntegration mockReachableIntegration;

	@InjectMocks
	private AvailabilityService availabilityService;

	private static final MailboxDto VALID_MAILBOX = new MailboxDto(null, "someRecipient", "someServiceAddress", "someServiceName", true);
	private static final MailboxDto INVALID_MAILBOX = new MailboxDto("Sender not accepted by recipient", "someRecipient", "someServiceAddress", "someServiceName", false);

	@AfterEach
	void tearDown() {
		verifyNoMoreInteractions(mockReachableIntegration);
	}

	@Test
	void testGetRecipientMailboxesAndCheckAvailability() {
		final var mailboxes = List.of(VALID_MAILBOX, INVALID_MAILBOX);
		when(mockReachableIntegration.isReachable(anyList(), anyString())).thenReturn(mailboxes);

		final var response = availabilityService.getRecipientMailboxesAndCheckAvailability(List.of("legalId"), ORGANIZATION_NUMBER);

		assertThat(response).isEqualTo(mailboxes);

		verify(mockReachableIntegration).isReachable(anyList(), anyString());
	}

	@Test
	void testNoValidMailboxesShouldReturnEmptyList() {
		when(mockReachableIntegration.isReachable(anyList(), anyString())).thenReturn(emptyList());

		final var availability = availabilityService.getRecipientMailboxesAndCheckAvailability(List.of("legalId"), ORGANIZATION_NUMBER);
		assertThat(availability).isEmpty();

		verify(mockReachableIntegration).isReachable(anyList(), anyString());
	}
}
