package se.sundsvall.digitalmail.integration.minameddelanden.reachable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static se.sundsvall.digitalmail.TestObjectFactory.generateSenderProperties;

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
import se.sundsvall.digitalmail.integration.minameddelanden.MailboxDto;
import se.sundsvall.digitalmail.integration.minameddelanden.configuration.MinaMeddelandenClientFactory;

@ExtendWith(MockitoExtension.class)
class ReachableIntegrationTest {

	@Mock
	private WebServiceTemplate mockReachableTemplate;

	@Mock
	private RecipientIntegrationMapper mockMapper;

	@Mock
	private MinaMeddelandenClientFactory mockMinaMeddelandenClientFactory;

	@InjectMocks
	private ReachableIntegration reachableIntegration;

	@BeforeEach
	void setUp() {
		when(mockMapper.createIsReachableRequest(any(), any())).thenCallRealMethod();
	}

	// Not really testing much but behavior
	@Test
	void testCallIsReachable_whenOk_shouldReturnResponse() {
		final var senderProperties = generateSenderProperties();

		final var accountStatus = new AccountStatus();
		accountStatus.setRecipientId("recipientId");

		final var reachabilityStatus = new ReachabilityStatus();
		reachabilityStatus.setAccountStatus(accountStatus);

		final var response = new IsReachableResponse();
		response.getReturns().add(reachabilityStatus);

		when(mockMapper.getMailboxSettings(response))
			.thenReturn(List.of(new MailboxDto("someRecipientId", "someServiceAddress", "someServiceName")));

		when(mockMinaMeddelandenClientFactory.getIsReachableWebServiceTemplate(senderProperties.name())).thenReturn(mockReachableTemplate);
		when(mockReachableTemplate.marshalSendAndReceive(any(IsReachable.class))).thenReturn(response);

		final var isReachableResponse = reachableIntegration.isReachable(senderProperties, "somePersonalNumber");

		assertThat(isReachableResponse).isNotNull().hasSize(1);
	}

	@Test
	void testCallIsRegistered_whenException_shouldThrowProblem() {

		final var personalNumbers = "personalNumber";
		final var senderProperties = generateSenderProperties();
		when(mockMinaMeddelandenClientFactory.getIsReachableWebServiceTemplate(senderProperties.name())).thenReturn(mockReachableTemplate);
		when(mockReachableTemplate.marshalSendAndReceive(any(IsReachable.class))).thenThrow(new RuntimeException());

		assertThatExceptionOfType(ThrowableProblem.class)
			.isThrownBy(() -> reachableIntegration.isReachable(senderProperties, personalNumbers))
			.withMessage("Error while getting digital mailbox from skatteverket");
	}
}
