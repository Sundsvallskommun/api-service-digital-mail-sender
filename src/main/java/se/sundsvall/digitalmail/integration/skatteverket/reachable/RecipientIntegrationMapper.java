package se.sundsvall.digitalmail.integration.skatteverket.reachable;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static se.sundsvall.digitalmail.util.LegalIdUtil.prefixOrgNumber;

import java.util.List;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Component;
import se.gov.minameddelanden.schema.recipient.ReachabilityStatus;
import se.gov.minameddelanden.schema.recipient.v3.IsReachable;
import se.gov.minameddelanden.schema.recipient.v3.IsReachableResponse;
import se.gov.minameddelanden.schema.recipient.v3.ObjectFactory;
import se.sundsvall.digitalmail.integration.skatteverket.MailboxDto;
import se.sundsvall.digitalmail.integration.skatteverket.SkatteverketProperties;

@Component
class RecipientIntegrationMapper {

	private static final ObjectFactory OBJECT_FACTORY = new ObjectFactory();

	private final SkatteverketProperties skatteverketProperties;

	RecipientIntegrationMapper(final SkatteverketProperties skatteverketProperties) {
		this.skatteverketProperties = skatteverketProperties;
	}

	/**
	 *
	 * @param  personalNumbers    map of personalnumbers
	 * @param  organizationNumber the organization number of the sender
	 * @return                    a request object that can be sent to Skatteverket to check if the personal numbers are
	 *                            reachable
	 */
	IsReachable createIsReachableRequest(final List<String> personalNumbers, String organizationNumber) {
		final var isReachable = OBJECT_FACTORY.createIsReachable();
		isReachable.getRecipientIds().addAll(personalNumbers);
		isReachable.setSenderOrgNr(prefixOrgNumber(organizationNumber));
		return isReachable;
	}

	/**
	 * Get mailbox settings.
	 * This method maps the response from Skatteverket to a list of {@link MailboxDto} objects.
	 * If no mailboxes could be found an empty list is returned.
	 *
	 * @param  response the response from Skatteverket
	 * @return          a list of {@link MailboxDto} objects.
	 */
	List<MailboxDto> toMailboxDtos(final IsReachableResponse response) {
		if (CollectionUtils.isNotEmpty(response.getReturns())) {
			return response.getReturns().stream()
				.map(this::toMailboxDto)
				.toList();
		}

		return List.of();
	}

	private MailboxDto toMailboxDto(final ReachabilityStatus reachabilityStatus) {
		if (isValidMailbox(reachabilityStatus)) {
			return MailboxDto.builder()
				.withValidMailbox(true)
				.withRecipientId(reachabilityStatus.getAccountStatus().getRecipientId())
				.withServiceAddress(reachabilityStatus.getAccountStatus().getServiceSupplier().getServiceAdress())
				.withServiceName(reachabilityStatus.getAccountStatus().getServiceSupplier().getName())
				.build();
		} else {
			return MailboxDto.builder()
				.withRecipientId(reachabilityStatus.getAccountStatus().getRecipientId())
				.withValidMailbox(false)
				.build();
		}
	}

	private boolean isValidMailbox(final ReachabilityStatus reachabilityStatus) {
		if (!reachabilityStatus.isSenderAccepted()) {
			return false;
		}

		var accountStatus = reachabilityStatus.getAccountStatus();
		if (accountStatus.isPending() || accountStatus.getServiceSupplier() == null) {
			return false;
		}

		var serviceSupplier = accountStatus.getServiceSupplier();
		return isSupportedSupplier(serviceSupplier.getName()) && !isBlank(serviceSupplier.getServiceAdress());
	}

	/**
	 * Check if the service supplier "name" is one that we support.
	 *
	 * @param  supplier name of the supplier
	 * @return          If the supplier is supported
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
