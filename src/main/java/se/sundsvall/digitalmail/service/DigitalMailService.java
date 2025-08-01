package se.sundsvall.digitalmail.service;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import se.sundsvall.digitalmail.api.model.DigitalInvoiceResponse;
import se.sundsvall.digitalmail.api.model.DigitalMailResponse;
import se.sundsvall.digitalmail.integration.kivra.InvoiceDto;
import se.sundsvall.digitalmail.integration.kivra.KivraIntegration;
import se.sundsvall.digitalmail.integration.party.PartyIntegration;
import se.sundsvall.digitalmail.integration.skatteverket.DigitalMailDto;
import se.sundsvall.digitalmail.integration.skatteverket.sendmail.DigitalMailIntegration;
import se.sundsvall.digitalmail.util.PdfCompressor;

@Service
public class DigitalMailService {

	private static final Logger LOGGER = LoggerFactory.getLogger(DigitalMailService.class);

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
		final var personalNumber = partyIntegration.getLegalId(municipalityId, requestDto.getPartyId());

		final var possibleMailbox = availabilityService.getRecipientMailboxesAndCheckAvailability(List.of(personalNumber));

		// We will always only have one here if no exception has been thrown, then we wouldn't be here
		final var mailbox = possibleMailbox.getFirst();
		requestDto.setRecipientId(mailbox.recipientId());

		// Send message, since the serviceAddress may differ we set this as a parameter into the integration.
		return digitalMailIntegration.sendDigitalMail(requestDto, mailbox.serviceAddress());
	}

	public DigitalInvoiceResponse sendDigitalInvoice(final InvoiceDto invoiceDto, final String municipalityId) {
		final var ssn = partyIntegration.getLegalId(municipalityId, invoiceDto.getPartyId());
		invoiceDto.setSsn(ssn);

		if (kivraIntegration.verifyValidRecipient(ssn)) {
			return new DigitalInvoiceResponse(invoiceDto.getPartyId(), kivraIntegration.sendInvoice(invoiceDto));
		}

		return new DigitalInvoiceResponse(invoiceDto.getPartyId(), false);
	}

	public boolean verifyRecipientHasSomeAvailableMailbox(final String partyId, final String municipalityId) {
		try {
			final var personalNumber = partyIntegration.getLegalId(municipalityId, partyId);
			// If this doesn't throw an exception, the recipient has an available mailbox
			availabilityService.getRecipientMailboxesAndCheckAvailability(List.of(personalNumber));

			return true;
		} catch (final Exception e) {
			LOGGER.error("Couldn't get legalId from party", e);
			return false;
		}
	}

}
