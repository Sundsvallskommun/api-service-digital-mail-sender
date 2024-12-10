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
	 * This is a list in case we need to expand upon this in the future.
	 * 
	 * @param  personalNumbers containing all partyIds we should fetch mailboxes for
	 * @return
	 */
	public List<MailboxDto> getRecipientMailboxesAndCheckAvailability(final List<String> personalNumbers) {
		final var mailboxes = reachableIntegration.isReachable(personalNumbers);

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
