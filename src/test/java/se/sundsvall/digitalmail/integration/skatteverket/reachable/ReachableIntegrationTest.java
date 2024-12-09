package se.sundsvall.digitalmail.integration.skatteverket.reachable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ws.client.core.WebServiceTemplate;
import org.zalando.problem.ThrowableProblem;
import se.gov.minameddelanden.schema.recipient.AccountStatus;
import se.gov.minameddelanden.schema.recipient.ReachabilityStatus;
import se.gov.minameddelanden.schema.recipient.v3.IsReachable;
import se.gov.minameddelanden.schema.recipient.v3.IsReachableResponse;
import se.sundsvall.digitalmail.integration.skatteverket.MailboxDto;

@ExtendWith(MockitoExtension.class)
class ReachableIntegrationTest {

	@Mock
	private WebServiceTemplate mockReachableTemplate;
	@Mock
	private RecipientIntegrationMapper mockMapper;

	@InjectMocks
	private ReachableIntegration reachableIntegration;

	@BeforeEach
	void setUp() {
		when(mockMapper.createIsReachableRequest(any())).thenCallRealMethod();
	}

	// Not really testing much but behavior
	@Test
	void testCallIsReachable_whenOk_shouldReturnResponse() {
		final var accountStatus = new AccountStatus();
		accountStatus.setRecipientId("recipientId");

		final var reachabilityStatus = new ReachabilityStatus();
		reachabilityStatus.setAccountStatus(accountStatus);

		final var response = new IsReachableResponse();
		response.getReturn().add(reachabilityStatus);

		when(mockMapper.getMailboxSettings(response))
			.thenReturn(List.of(new MailboxDto("someRecipientId", "someServiceAddress", "someServiceName")));

		when(mockReachableTemplate.marshalSendAndReceive(any(IsReachable.class))).thenReturn(response);

		final var isReachableResponse = reachableIntegration.isReachable(List.of("somePersonalNumber"));

		assertThat(isReachableResponse).isNotNull().hasSize(1);
	}

	@Test
	void testCallIsRegistered_whenException_shouldThrowProblem() {

		final var personalNumbers = List.of("personalNumber");
		when(mockReachableTemplate.marshalSendAndReceive(any(IsReachable.class))).thenThrow(new RuntimeException());

		assertThatExceptionOfType(ThrowableProblem.class)
			.isThrownBy(() -> reachableIntegration.isReachable(personalNumbers))
			.withMessage("Error while getting digital mailbox from skatteverket");
	}
}
