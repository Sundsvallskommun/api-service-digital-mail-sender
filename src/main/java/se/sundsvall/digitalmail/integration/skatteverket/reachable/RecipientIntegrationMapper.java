package se.sundsvall.digitalmail.integration.skatteverket.reachable;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

import se.sundsvall.digitalmail.integration.skatteverket.MailboxDto;
import se.sundsvall.digitalmail.integration.skatteverket.SkatteverketProperties;

import se.gov.minameddelanden.schema.recipient.ReachabilityStatus;
import se.gov.minameddelanden.schema.recipient.v3.IsReachable;
import se.gov.minameddelanden.schema.recipient.v3.IsReachableResponse;
import se.gov.minameddelanden.schema.recipient.v3.ObjectFactory;

@Component
class RecipientIntegrationMapper {

	static final String SENDER_ORG_NR = "162120002411"; // We will always send as sundsvalls kommun.

	private static final ObjectFactory OBJECT_FACTORY = new ObjectFactory();

	private final SkatteverketProperties skatteverketProperties;

	RecipientIntegrationMapper(final SkatteverketProperties skatteverketProperties) {
		this.skatteverketProperties = skatteverketProperties;
	}

	/**
	 * 
	 * @param  personalNumbers map of personalnumbers with corresponding partyIds
	 * @return
	 */
	IsReachable createIsReachableRequest(final List<String> personalNumbers) {
		final var isReachable = OBJECT_FACTORY.createIsReachable();
		isReachable.getRecipientId().addAll(personalNumbers);
		isReachable.setSenderOrgNr(SENDER_ORG_NR);
		return isReachable;
	}

	List<MailboxDto> getMailboxSettings(final IsReachableResponse response) {
		if (response.getReturn() != null && !response.getReturn().isEmpty()) {
			// There will only be one since we only ever ask for one, get it (for now at least).
			return response.getReturn().stream()
				.map(this::getMailboxSettings)
				.filter(Optional::isPresent)
				.map(Optional::get)
				.toList();
		}

		return List.of();
	}

	/**
	 * Check that:
	 * - there's not a pending accountregistration (that we have somewhere to send the message)
	 * - The sender is accepted by the recipient (no difference between disallowing and no mailbox)
	 * - that there's an existing servicesupplier object.
	 *
	 * @param  reachabilityStatus status of the recipient
	 * @return                    Optional {@link MailboxDto} containing the url and recipientId.
	 */
	private Optional<MailboxDto> getMailboxSettings(final ReachabilityStatus reachabilityStatus) {
		if (reachabilityStatus.isSenderAccepted() &&                                                            // Make sure the recipient accepts the sender (Sundsvalls kommun)
			reachabilityStatus.getAccountStatus().getServiceSupplier() != null &&                           // If the recipient doesn't have a mailbox this will not be present
			!reachabilityStatus.getAccountStatus().isPending() &&                                           // It should not be a pending account registration
			isSupportedSupplier(reachabilityStatus.getAccountStatus().getServiceSupplier().getName()) &&    // Check that we support the supplier
			isNotBlank(reachabilityStatus.getAccountStatus().getServiceSupplier().getServiceAdress())) {    // Make sure we have an address to send something to.
			final var recipientId = reachabilityStatus.getAccountStatus().getRecipientId();
			final var serviceAdress = reachabilityStatus.getAccountStatus().getServiceSupplier().getServiceAdress();
			final var serviceName = getShortSupplierName(reachabilityStatus.getAccountStatus().getServiceSupplier().getName());

			return Optional.of(new MailboxDto(recipientId, serviceAdress, serviceName));
		}

		return Optional.empty();
	}

	/**
	 * Check if the service supplier "name" is one that we support.
	 *
	 * @param  supplier
	 * @return
	 */
	boolean isSupportedSupplier(final String supplier) {
		return skatteverketProperties.supportedSuppliers().stream()
			.anyMatch(supportedSupplier -> supplier.toLowerCase().contains(supportedSupplier.toLowerCase()));
	}

	/**
	 * Map the supplier name into our own "short" name.
	 *
	 * @return the "short" name or the provided supplier as a fallback
	 */
	String getShortSupplierName(final String supplier) {
		return skatteverketProperties.supportedSuppliers().stream()
			.filter(supportedSupplier -> supplier.toLowerCase().contains(supportedSupplier.toLowerCase()))
			.findFirst()
			.map(String::toLowerCase)
			.orElse(supplier);
	}
}
