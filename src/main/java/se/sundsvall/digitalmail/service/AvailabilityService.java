package se.sundsvall.digitalmail.service;

import java.util.List;
import org.springframework.stereotype.Service;
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
	 * Throws a problem in case no valid mailboxes are found for the given legal Ids.
	 *
	 * @param  legalIds           containing all legal Ids we should fetch mailboxes for
	 * @param  organizationNumber the organization number of the sender
	 * @return                    List of MailboxDto containing the mailboxes for the given legal Ids
	 */
	public List<MailboxDto> getRecipientMailboxesAndCheckAvailability(final List<String> legalIds, String organizationNumber) {
		return reachableIntegration.isReachable(legalIds, organizationNumber);
	}
}
