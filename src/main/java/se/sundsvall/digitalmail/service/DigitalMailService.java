package se.sundsvall.digitalmail.service;

import static org.apache.commons.collections4.CollectionUtils.isEmpty;
import static org.zalando.problem.Status.NOT_FOUND;
import static se.sundsvall.dept44.util.LogUtils.sanitizeForLogging;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.zalando.problem.Problem;
import se.sundsvall.digitalmail.api.model.DigitalInvoiceResponse;
import se.sundsvall.digitalmail.api.model.DigitalMailResponse;
import se.sundsvall.digitalmail.api.model.Mailbox;
import se.sundsvall.digitalmail.integration.kivra.InvoiceDto;
import se.sundsvall.digitalmail.integration.kivra.KivraIntegration;
import se.sundsvall.digitalmail.integration.party.PartyIntegration;
import se.sundsvall.digitalmail.integration.skatteverket.DigitalMailDto;
import se.sundsvall.digitalmail.integration.skatteverket.MailboxDto;
import se.sundsvall.digitalmail.integration.skatteverket.sendmail.DigitalMailIntegration;
import se.sundsvall.digitalmail.util.PdfCompressor;

@Service
public class DigitalMailService {

	private static final Logger LOGGER = LoggerFactory.getLogger(DigitalMailService.class);
	private static final String ERROR_NO_LEGAL_ID_FOUND = "No legal Id found for partyId: %s";

	private final PartyIntegration partyIntegration;

	private final DigitalMailIntegration digitalMailIntegration;

	private final KivraIntegration kivraIntegration;

	private final AvailabilityService availabilityService;

	DigitalMailService(
		final PartyIntegration partyIntegration,
		final DigitalMailIntegration digitalMailIntegration,
		final KivraIntegration kivraIntegration,
		final AvailabilityService availabilityService) {
		this.partyIntegration = partyIntegration;
		this.digitalMailIntegration = digitalMailIntegration;
		this.kivraIntegration = kivraIntegration;
		this.availabilityService = availabilityService;
	}

	/**
	 * Send a digital mail to a recipient
	 *
	 * @param  requestDto containing message and recipient
	 * @return            Response whether the sending went ok or not.
	 */
	public DigitalMailResponse sendDigitalMail(final DigitalMailDto requestDto, final String municipalityId) {
		PdfCompressor.compress(requestDto.getAttachments());

		final var legalId = partyIntegration.getLegalId(municipalityId, requestDto.getPartyId())
			.orElseThrow(() -> Problem.builder()
				.withTitle("Error while sending digital mail")
				.withDetail(ERROR_NO_LEGAL_ID_FOUND.formatted(requestDto.getPartyId()))
				.withStatus(NOT_FOUND)
				.build());

		final var mailboxes = availabilityService.getRecipientMailboxesAndCheckAvailability(List.of(legalId), requestDto.getOrganizationNumber());

		// Check if we didn't get any mailboxes at all
		if (isEmpty(mailboxes) || mailboxes.stream().noneMatch(MailboxDto::isValidMailbox)) {
			throw Problem.builder()
				.withTitle("Couldn't find any mailboxes")
				.withDetail("No mailbox could be found for any of the given partyIds or the recipients doesn't allow the sender.")
				.withStatus(NOT_FOUND)
				.build();
		}

		// We'll only have one mailbox as we only handle one legal Id at a time.
		final var mailbox = mailboxes.getFirst();
		requestDto.setRecipientId(mailbox.getRecipientId());

		// Send digital mail, since the serviceAddress may differ we set this as a parameter into the integration.
		return digitalMailIntegration.sendDigitalMail(requestDto, mailbox.getServiceAddress());
	}

	public DigitalInvoiceResponse sendDigitalInvoice(final InvoiceDto invoiceDto, final String municipalityId) {
		final var ssn = partyIntegration.getLegalId(municipalityId, invoiceDto.getPartyId())
			.orElseThrow(() -> Problem.builder()
				.withTitle("Error while sending digital invoice")
				.withDetail(ERROR_NO_LEGAL_ID_FOUND.formatted(invoiceDto.getPartyId()))
				.withStatus(NOT_FOUND)
				.build());

		invoiceDto.setSsn(ssn);

		if (kivraIntegration.verifyValidRecipient(ssn)) {
			return new DigitalInvoiceResponse(invoiceDto.getPartyId(), kivraIntegration.sendInvoice(invoiceDto));
		}

		return new DigitalInvoiceResponse(invoiceDto.getPartyId(), false);
	}

	public List<Mailbox> getMailboxes(final List<String> partyIds, final String municipalityId, final String organizationNumber) {
		var partyIdLegalIdMap = getPartyIdLegalIdMap(partyIds, municipalityId);

		// Now we know which partyIds have a legal Id and which do not, extract the ones we have and check availability.
		var foundLegalIds = partyIdLegalIdMap.values().stream()
			.filter(Objects::nonNull)
			.toList();

		// If we have no legal Ids we cannot continue, create unreachable mailboxes for all partyIds.
		if (foundLegalIds.isEmpty()) {
			// And return it directly, no need to call availabilityService with an empty list.
			LOGGER.info("No legal Ids found for any of the given partyIds, returning unreachable mailboxes for all partyIds.");
			return createUnreachableMailboxes(partyIds);
		}

		// Otherwise, call availabilityService with the found legal Ids.
		var mailBoxDtoList = availabilityService.getRecipientMailboxesAndCheckAvailability(foundLegalIds, organizationNumber);

		// Same thing here, if we got no mailboxes back (unlikely), create unreachable mailboxes for all partyIds.
		if (mailBoxDtoList.isEmpty()) {
			LOGGER.info("No mailboxes found for any of the given legal Ids, returning unreachable mailboxes for all partyIds.");
			return createUnreachableMailboxes(partyIds);
		}

		return createMailboxes(mailBoxDtoList, partyIdLegalIdMap);
	}

	private Map<String, String> getPartyIdLegalIdMap(final List<String> partyIds, final String municipalityId) {
		var partyIdLegalIdNumberMap = new HashMap<String, String>();
		partyIds.forEach(partyId -> {
			// Add the partyId to the map even if the legal Id is null, so we can keep track of which partyIds have no legal Id.
			partyIntegration.getLegalId(municipalityId, partyId).ifPresentOrElse(
				legalId -> partyIdLegalIdNumberMap.put(partyId, legalId),
				() -> {
					partyIdLegalIdNumberMap.put(partyId, null);
					LOGGER.warn("No legal id found for partyId: {}", sanitizeForLogging(partyId));
				});
		});

		return partyIdLegalIdNumberMap;
	}

	private List<Mailbox> createMailboxes(final List<MailboxDto> mailBoxDtoList, final Map<String, String> partyIdLegalIdMap) {
		var mailboxes = new ArrayList<Mailbox>();
		// Create a reversed hashmap so we don't need to iterate through the whole original hashmap to find a legal Id
		var legalIdPartyIdMap = createLegalIdPartyIdMap(partyIdLegalIdMap);

		// Map each MailboxDto to a Mailbox object.
		for (MailboxDto mailboxDto : mailBoxDtoList) {
			var legalId = mailboxDto.getRecipientId();
			// Find the corresponding partyId
			var partyId = legalIdPartyIdMap.get(legalId);

			// Create a Mailbox object and add it to the list to return
			mailboxes.add(toMailbox(mailboxDto, partyId));
		}

		// Create "unreachable" mailboxes for all partyIds with missing legal Ids
		partyIdLegalIdMap.forEach((partyId, legalId) -> {
			if (legalId == null) {
				mailboxes.add(createUnreachableMailbox(partyId));
			}
		});

		return mailboxes;
	}

	private Map<String, String> createLegalIdPartyIdMap(Map<String, String> partyIdLegalIdMap) {
		return partyIdLegalIdMap.entrySet().stream()
			.filter(entry -> entry.getValue() != null) // Filter out null values, i.e. legal Ids not found.
			.collect(Collectors.toMap(
				Map.Entry::getValue,
				Map.Entry::getKey));
	}

	private List<Mailbox> createUnreachableMailboxes(final List<String> partyIds) {
		return partyIds.stream()
			.map(this::createUnreachableMailbox)
			.toList();
	}

	private Mailbox createUnreachableMailbox(final String partyId) {
		return Mailbox.builder()
			.withPartyId(partyId)
			.withReachable(false)
			.withReason(ERROR_NO_LEGAL_ID_FOUND.formatted(partyId))
			.build();
	}

	private Mailbox toMailbox(final MailboxDto mailboxDto, final String partyId) {
		return Mailbox.builder()
			.withPartyId(partyId)
			.withSupplier(mailboxDto.getServiceName())
			.withReachable(mailboxDto.isValidMailbox())
			.withReason(mailboxDto.getReason())
			.build();
	}
}
