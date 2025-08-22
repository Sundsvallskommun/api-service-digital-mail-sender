package se.sundsvall.digitalmail.service;

import static org.zalando.problem.Status.NOT_FOUND;

import java.util.List;
import org.springframework.stereotype.Service;
import org.zalando.problem.Problem;
import se.sundsvall.digitalmail.integration.skatteverket.MailboxDto;
import se.sundsvall.digitalmail.integration.skatteverket.reachable.ReachableIntegration;

@Service
public class AvailabilityService {

	private final ReachableIntegration reachableIntegration;

	public AvailabilityService(final ReachableIntegration reachableIntegration) {
		this.reachableIntegration = reachableIntegration;
	}

	/**
	 * Fetch a list of possible mailboxes.
	 * Throws a problem in case no mailboxes are found for the given partyIds.
	 *
	 * @param  personalNumbers    containing all partyIds we should fetch mailboxes for
	 * @param  organizationNumber the organization number of the sender
	 * @return                    List of MailboxDto containing the mailboxes for the given partyIds
	 */
	public List<MailboxDto> getRecipientMailboxesAndCheckAvailability(final List<String> personalNumbers, String organizationNumber) {
		final var mailboxes = reachableIntegration.isReachable(personalNumbers, organizationNumber);

		// Check if we didn't get any mailboxes at all
		if (mailboxes.isEmpty()) {
			throw Problem.builder()
				.withTitle("Couldn't send digital mail")
				.withDetail("No mailbox could be found for any of the given partyIds or the recipients doesn't allow the sender.")
				.withStatus(NOT_FOUND)
				.build();
		}

		return mailboxes;
	}
}
