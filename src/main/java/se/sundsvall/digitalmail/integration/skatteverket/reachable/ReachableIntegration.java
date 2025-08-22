package se.sundsvall.digitalmail.integration.skatteverket.reachable;

import static org.zalando.problem.Status.INTERNAL_SERVER_ERROR;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.ws.client.core.WebServiceTemplate;
import org.zalando.problem.Problem;
import se.gov.minameddelanden.schema.recipient.v3.IsReachableResponse;
import se.sundsvall.digitalmail.integration.skatteverket.MailboxDto;

@Component
@CircuitBreaker(name = "reachableIntegration")
public class ReachableIntegration {
	private static final Logger LOGGER = LoggerFactory.getLogger(ReachableIntegration.class);
	private final WebServiceTemplate isReachableTemplate;
	private final RecipientIntegrationMapper mapper;

	ReachableIntegration(
		@Qualifier("skatteverketIsReachableWebserviceTemplate") final WebServiceTemplate isReachableTemplate,
		final RecipientIntegrationMapper mapper) {
		this.isReachableTemplate = isReachableTemplate;
		this.mapper = mapper;
	}

	/**
	 * Fetches a mailbox and if a mailbox is reachable.
	 *
	 * @param  personalNumbers    All personal numbers to fetch mailboxes for
	 * @param  organizationNumber The organization number of the sender
	 * @return                    List of MailboxDto containing the mailboxes for the given personal numbers
	 */
	public List<MailboxDto> isReachable(final List<String> personalNumbers, String organizationNumber) {
		try {
			// Call Skatteverket to see which mailbox(es) (if any) the person has
			final var isReachableRequest = mapper.createIsReachableRequest(personalNumbers, organizationNumber);

			LOGGER.info("Sending is reachable request");
			final var isReachableResponse = (IsReachableResponse) isReachableTemplate.marshalSendAndReceive(isReachableRequest);

			LOGGER.info("Mapping and getting mailbox settings");
			return mapper.toMailboxDtoList(isReachableResponse);
		} catch (Exception e) {
			throw Problem.builder()
				.withTitle("Error while getting digital mailboxes from skatteverket")
				.withStatus(INTERNAL_SERVER_ERROR)
				.withDetail(e.getMessage())
				.build();
		}
	}
}
