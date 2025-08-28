package se.sundsvall.digitalmail.service;

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
	private static final String ERROR_NO_PERSONAL_NUMBER_FOUND = "No personal number found for partyId: %s";

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

		final var personalNumber = partyIntegration.getLegalId(municipalityId, requestDto.getPartyId())
			.orElseThrow(() -> Problem.builder()
				.withTitle("Error while sending digital mail")
				.withDetail(ERROR_NO_PERSONAL_NUMBER_FOUND.formatted(requestDto.getPartyId()))
				.withStatus(NOT_FOUND)
				.build());

		final var mailboxes = availabilityService.getRecipientMailboxesAndCheckAvailability(List.of(personalNumber), requestDto.getOrganizationNumber());

		// We'll only have one mailbox as we only handle one personal number at a time.
		final var mailbox = mailboxes.getFirst();
		requestDto.setRecipientId(mailbox.getRecipientId());

		// Send digital mail, since the serviceAddress may differ we set this as a parameter into the integration.
		return digitalMailIntegration.sendDigitalMail(requestDto, mailbox.getServiceAddress());
	}

	public DigitalInvoiceResponse sendDigitalInvoice(final InvoiceDto invoiceDto, final String municipalityId) {
		final var ssn = partyIntegration.getLegalId(municipalityId, invoiceDto.getPartyId())
			.orElseThrow(() -> Problem.builder()
				.withTitle("Error while sending digital invoice")
				.withDetail(ERROR_NO_PERSONAL_NUMBER_FOUND.formatted(invoiceDto.getPartyId()))
				.withStatus(NOT_FOUND)
				.build());

		invoiceDto.setSsn(ssn);

		if (kivraIntegration.verifyValidRecipient(ssn)) {
			return new DigitalInvoiceResponse(invoiceDto.getPartyId(), kivraIntegration.sendInvoice(invoiceDto));
		}

		return new DigitalInvoiceResponse(invoiceDto.getPartyId(), false);
	}

	public List<Mailbox> getMailboxes(final List<String> partyIds, final String municipalityId, final String organizationNumber) {
		var partyIdPersonalNumberMap = getPartyIdPersonalNumberMap(partyIds, municipalityId);

		// Create a reversed hashmap so we don't need to iterate through the whole original hashmap to find a personalNumber
		var personalNumberPartyIdMap = createPersonalNumberPartyIdMap(partyIdPersonalNumberMap);

		// Now we know which partyIds have a personal number and which do not, extract the ones we have and check availability.
		var foundPersonalNumbers = partyIdPersonalNumberMap.values().stream()
			.filter(Objects::nonNull)
			.toList();

		var mailBoxDtoList = availabilityService.getRecipientMailboxesAndCheckAvailability(foundPersonalNumbers, organizationNumber);

		return createMailboxes(mailBoxDtoList, personalNumberPartyIdMap, partyIdPersonalNumberMap);
	}

	private Map<String, String> createPersonalNumberPartyIdMap(HashMap<String, String> partyIdPersonalNumberMap) {
		return partyIdPersonalNumberMap.entrySet().stream()
			.filter(entry -> entry.getValue() != null) // Filter out null values, i.e. personal numbers not found.
			.collect(Collectors.toMap(
				Map.Entry::getValue,
				Map.Entry::getKey));
	}

	private HashMap<String, String> getPartyIdPersonalNumberMap(final List<String> partyIds, final String municipalityId) {
		var partyIdPersonalNumberMap = new HashMap<String, String>();
		partyIds.forEach(partyId -> {
			// Add the partyId to the map even if the personal number is null, so we can keep track of which partyIds have no
			// personal number.
			partyIntegration.getLegalId(municipalityId, partyId).ifPresentOrElse(
				personalNumber -> partyIdPersonalNumberMap.put(partyId, personalNumber),
				() -> {
					partyIdPersonalNumberMap.put(partyId, null);
					LOGGER.warn("No personal number found for partyId: {}", sanitizeForLogging(partyId));
				});
		});

		return partyIdPersonalNumberMap;
	}

	private ArrayList<Mailbox> createMailboxes(final List<MailboxDto> mailBoxDtoList, final Map<String, String> personalNumberPartyIdMap, final HashMap<String, String> partyIdPersonalNumberMap) {
		var mailboxes = new ArrayList<Mailbox>();

		// Map each MailboxDto to a Mailbox object.
		for (MailboxDto mailboxDto : mailBoxDtoList) {
			var personalNumber = mailboxDto.getRecipientId();
			// Find the corresponding partyId
			var partyId = personalNumberPartyIdMap.get(personalNumber);

			// Create a Mailbox object and add it to the list to return
			mailboxes.add(toMailbox(mailboxDto, partyId));
		}

		// Create "unreachable" mailboxes for all the missing personal numbers
		partyIdPersonalNumberMap.forEach((partyId, personalNumber) -> {
			if (personalNumber == null) {
				mailboxes.add(Mailbox.builder()
					.withPartyId(partyId)
					.withReachable(false)
					.build());
			}
		});

		return mailboxes;
	}

	private Mailbox toMailbox(final MailboxDto mailboxDto, final String partyId) {
		return Mailbox.builder()
			.withPartyId(partyId)
			.withSupplier(mailboxDto.getServiceName())
			.withReachable(mailboxDto.isValidMailbox())
			.build();
	}
}
