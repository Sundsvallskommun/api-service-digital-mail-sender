package se.sundsvall.digitalmail.service;

import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import se.sundsvall.digitalmail.integration.skatteverket.MailboxDto;
import se.sundsvall.digitalmail.integration.skatteverket.reachable.ReachableIntegration;

@Service
public class AvailabilityService {

	private final ReachableIntegration reachableIntegration;
	private static final int MAX_LEGAL_IDS_PER_CALL = 1000;

	public AvailabilityService(final ReachableIntegration reachableIntegration) {
		this.reachableIntegration = reachableIntegration;
	}

	/**
	 * Fetch a list of possible mailboxes.
	 * If the list of legalIds is larger than MAX_LEGAL_IDS_PER_CALL it will be divided into multiple calls to the
	 * integration, and the results
	 * will be merged into one list.
	 * Throws a problem in case no valid mailboxes are found for the given legal Ids.
	 *
	 * @param  legalIds           containing all legal Ids we should fetch mailboxes for
	 * @param  organizationNumber the organization number of the sender
	 * @return                    List of MailboxDto containing the mailboxes for the given legal Ids
	 */
	public List<MailboxDto> getRecipientMailboxesAndCheckAvailability(final List<String> legalIds, final String organizationNumber) {
		final var result = new ArrayList<MailboxDto>();
		for (var i = 0; i < legalIds.size(); i += MAX_LEGAL_IDS_PER_CALL) {
			final var chunk = legalIds.subList(i, Math.min(i + MAX_LEGAL_IDS_PER_CALL, legalIds.size()));
			result.addAll(reachableIntegration.isReachable(chunk, organizationNumber));
		}
		return result;
	}
}
