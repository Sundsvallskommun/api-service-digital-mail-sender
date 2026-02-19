package se.sundsvall.digitalmail.integration.skatteverket.reachable;

import java.util.List;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Component;
import se.gov.minameddelanden.schema.recipient.ReachabilityStatus;
import se.gov.minameddelanden.schema.recipient.v3.IsReachable;
import se.gov.minameddelanden.schema.recipient.v3.IsReachableResponse;
import se.gov.minameddelanden.schema.recipient.v3.ObjectFactory;
import se.sundsvall.digitalmail.integration.skatteverket.MailboxDto;
import se.sundsvall.digitalmail.integration.skatteverket.SkatteverketProperties;
import se.sundsvall.digitalmail.util.LegalIdUtil;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static se.sundsvall.digitalmail.util.LegalIdUtil.prefixOrgNumber;

@Component
class RecipientIntegrationMapper {

	private static final ObjectFactory OBJECT_FACTORY = new ObjectFactory();

	// Possible reasons when a mailbox is invalid
	private static final String REASON_SENDER_NOT_ACCEPTED = "Sender not accepted by recipient";
	private static final String REASON_MAILBOX_PENDING = "Mailbox is pending activation";
	private static final String REASON_NO_SERVICE_SUPPLIER = "No service supplier available";
	private static final String REASON_RECIPIENT_NOT_ADULT = "Recipient is not an adult";
	private static final String REASON_UNSUPPORTED_SUPPLIER = "Unsupported service supplier";
	private static final String REASON_SERVICE_ADDRESS_BLANK = "Service address is blank";

	// Internal record for handling mailbox validation results together with possible reason for failure
	private record ValidationResult(boolean valid, String reason) {

		public static ValidationResult success() {
			return new ValidationResult(true, null);
		}

		public static ValidationResult failure(final String reason) {
			return new ValidationResult(false, reason);
		}
	}

	private final SkatteverketProperties skatteverketProperties;

	RecipientIntegrationMapper(final SkatteverketProperties skatteverketProperties) {
		this.skatteverketProperties = skatteverketProperties;
	}

	/**
	 *
	 * @param  legalIds           map of legalIds
	 * @param  organizationNumber the organization number of the sender
	 * @return                    a request object that can be sent to Skatteverket to check if the legal Ids are reachable
	 */
	IsReachable createIsReachableRequest(final List<String> legalIds, String organizationNumber) {
		final var isReachable = OBJECT_FACTORY.createIsReachable();
		isReachable.getRecipientIds().addAll(legalIds);
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
		final var validationResult = validateMailbox(reachabilityStatus);

		if (validationResult.valid()) {
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
				.withReason(validationResult.reason())
				.build();
		}
	}

	private ValidationResult validateMailbox(final ReachabilityStatus reachabilityStatus) {
		if (!reachabilityStatus.isSenderAccepted()) {
			return ValidationResult.failure(REASON_SENDER_NOT_ACCEPTED);
		}

		final var accountStatus = reachabilityStatus.getAccountStatus();
		if (accountStatus.isPending()) {
			return ValidationResult.failure(REASON_MAILBOX_PENDING);
		}

		if (accountStatus.getServiceSupplier() == null) {
			return ValidationResult.failure(REASON_NO_SERVICE_SUPPLIER);
		}

		// If recipientId is not an organization number, check that it is an adult person, otherwise return false
		if (!LegalIdUtil.isOrgNumber(accountStatus.getRecipientId()) && !LegalIdUtil.isAnAdult(accountStatus.getRecipientId())) {
			return ValidationResult.failure(REASON_RECIPIENT_NOT_ADULT);
		}

		final var serviceSupplier = accountStatus.getServiceSupplier();

		if (!isSupportedSupplier(serviceSupplier.getName())) {
			return ValidationResult.failure(REASON_UNSUPPORTED_SUPPLIER);
		}

		if (isBlank(serviceSupplier.getServiceAdress())) {
			return ValidationResult.failure(REASON_SERVICE_ADDRESS_BLANK);
		}

		return ValidationResult.success();
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
