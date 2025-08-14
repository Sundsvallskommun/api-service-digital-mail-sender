package se.sundsvall.digitalmail.integration.minameddelanden.reachable;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

import java.util.List;
import java.util.Optional;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Component;
import se.gov.minameddelanden.schema.recipient.ReachabilityStatus;
import se.gov.minameddelanden.schema.recipient.v3.IsReachable;
import se.gov.minameddelanden.schema.recipient.v3.IsReachableResponse;
import se.gov.minameddelanden.schema.recipient.v3.ObjectFactory;
import se.sundsvall.digitalmail.integration.minameddelanden.MailboxDto;
import se.sundsvall.digitalmail.integration.minameddelanden.configuration.MinaMeddelandenProperties;

@Component
class RecipientIntegrationMapper {

	private static final ObjectFactory OBJECT_FACTORY = new ObjectFactory();

	private final MinaMeddelandenProperties minaMeddelandenProperties;

	RecipientIntegrationMapper(final MinaMeddelandenProperties minaMeddelandenProperties) {
		this.minaMeddelandenProperties = minaMeddelandenProperties;
	}

	IsReachable createIsReachableRequest(final MinaMeddelandenProperties.Sender senderProperties, final String legalId) {
		final var isReachable = OBJECT_FACTORY.createIsReachable();
		isReachable.getRecipientIds().add(legalId);
		isReachable.setSenderOrgNr(senderProperties.id());
		return isReachable;
	}

	List<MailboxDto> getMailboxSettings(final IsReachableResponse response) {
		if (CollectionUtils.isNotEmpty(response.getReturns())) {
			// There will only be one since we only ever ask for one, get it (for now at least).
			return response.getReturns().stream()
				.map(this::getMailboxSettings)
				.filter(Optional::isPresent)
				.map(Optional::get)
				.toList();
		}
		return List.of();
	}

	/**
	 * Check that: - there's not a pending accountregistration (that we have somewhere to send the message) - The sender is
	 * accepted by the recipient (no difference between disallowing and no mailbox) - that there's an existing
	 * servicesupplier object.
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
			final var serviceAddress = reachabilityStatus.getAccountStatus().getServiceSupplier().getServiceAdress();
			final var serviceName = getShortSupplierName(reachabilityStatus.getAccountStatus().getServiceSupplier().getName());

			return Optional.of(new MailboxDto(recipientId, serviceAddress, serviceName));
		}

		return Optional.empty();
	}

	/**
	 * Check if the service supplier "name" is one that we support.
	 */
	boolean isSupportedSupplier(final String supplier) {
		return minaMeddelandenProperties.supportedSuppliers().stream()
			.anyMatch(supportedSupplier -> supplier.toLowerCase().contains(supportedSupplier.toLowerCase()));
	}

	/**
	 * Map the supplier name into our own "short" name.
	 *
	 * @return the "short" name or the provided supplier as a fallback
	 */
	String getShortSupplierName(final String supplier) {
		return minaMeddelandenProperties.supportedSuppliers().stream()
			.filter(supportedSupplier -> supplier.toLowerCase().contains(supportedSupplier.toLowerCase()))
			.findFirst()
			.map(String::toLowerCase)
			.orElse(supplier);
	}
}
