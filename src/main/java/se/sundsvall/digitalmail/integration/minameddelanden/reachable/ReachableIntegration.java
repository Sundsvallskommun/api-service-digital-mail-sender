package se.sundsvall.digitalmail.integration.minameddelanden.reachable;

import static org.zalando.problem.Status.INTERNAL_SERVER_ERROR;
import static org.zalando.problem.Status.NOT_FOUND;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.zalando.problem.Problem;
import se.gov.minameddelanden.schema.recipient.v3.IsReachableResponse;
import se.sundsvall.digitalmail.integration.minameddelanden.MailboxDto;
import se.sundsvall.digitalmail.integration.minameddelanden.configuration.MinaMeddelandenClientFactory;
import se.sundsvall.digitalmail.integration.minameddelanden.configuration.MinaMeddelandenProperties;

@Component
public class ReachableIntegration {
	private static final Logger LOGGER = LoggerFactory.getLogger(ReachableIntegration.class);

	private final RecipientIntegrationMapper mapper;
	private final MinaMeddelandenClientFactory clientFactory;

	ReachableIntegration(
		final RecipientIntegrationMapper mapper,
		final MinaMeddelandenClientFactory clientFactory) {
		this.mapper = mapper;
		this.clientFactory = clientFactory;
	}

	/**
	 * Fetches a mailbox and if a mailbox is reachable.
	 *
	 * @param  senderProperties configuration for a given sender.
	 * @param  legalId          legalId to check for reachability.
	 * @return                  a list of MailboxDto containing mailbox settings for the given personal numbers.
	 */
	public List<MailboxDto> isReachable(final MinaMeddelandenProperties.Sender senderProperties, final String legalId) {
		try {
			// Call Skatteverket to see which mailbox(es) (if any) the person has
			final var isReachableRequest = mapper.createIsReachableRequest(senderProperties, legalId);
			final var isReachableTemplate = clientFactory.getIsReachableWebServiceTemplate(senderProperties.name());

			LOGGER.info("Sending is reachable request");
			final var isReachableResponse = (IsReachableResponse) isReachableTemplate.marshalSendAndReceive(isReachableRequest);

			LOGGER.info("Mapping and getting mailbox settings");
			var mailboxSettings = mapper.getMailboxSettings(isReachableResponse);

			if (mailboxSettings.isEmpty()) {
				throw Problem.builder()
					.withTitle("Couldn't send digital mail")
					.withDetail("No mailbox could be found for any of the given partyIds or the recipients doesn't allow the sender.")
					.withStatus(NOT_FOUND)
					.build();
			}
			return mailboxSettings;
		} catch (Exception e) {
			throw Problem.builder()
				.withTitle("Error while getting digital mailbox from skatteverket")
				.withStatus(INTERNAL_SERVER_ERROR)
				.withDetail(e.getMessage())
				.build();
		}
	}
}
