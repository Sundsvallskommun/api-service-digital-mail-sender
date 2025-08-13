package se.sundsvall.digitalmail.service;

import static org.zalando.problem.Status.INTERNAL_SERVER_ERROR;
import static se.sundsvall.digitalmail.Constants.DEFAULT_SENDER_NAME;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.zalando.problem.Problem;
import se.sundsvall.digitalmail.api.model.DigitalInvoiceResponse;
import se.sundsvall.digitalmail.api.model.DigitalMailResponse;
import se.sundsvall.digitalmail.integration.kivra.InvoiceDto;
import se.sundsvall.digitalmail.integration.kivra.KivraIntegration;
import se.sundsvall.digitalmail.integration.minameddelanden.DigitalMailDto;
import se.sundsvall.digitalmail.integration.minameddelanden.configuration.MinaMeddelandenProperties;
import se.sundsvall.digitalmail.integration.minameddelanden.reachable.ReachableIntegration;
import se.sundsvall.digitalmail.integration.minameddelanden.sendmail.DigitalMailIntegration;
import se.sundsvall.digitalmail.integration.party.PartyIntegration;
import se.sundsvall.digitalmail.util.PdfCompressor;

@Service
public class DigitalMailService {

	private static final Logger LOGGER = LoggerFactory.getLogger(DigitalMailService.class);

	private final PartyIntegration partyIntegration;
	private final DigitalMailIntegration digitalMailIntegration;
	private final KivraIntegration kivraIntegration;
	private final ReachableIntegration reachableIntegration;
	private final MinaMeddelandenProperties properties;

	DigitalMailService(
		final PartyIntegration partyIntegration,
		final DigitalMailIntegration digitalMailIntegration,
		final KivraIntegration kivraIntegration,
		final ReachableIntegration reachableIntegration,
		final MinaMeddelandenProperties properties) {
		this.partyIntegration = partyIntegration;
		this.digitalMailIntegration = digitalMailIntegration;
		this.kivraIntegration = kivraIntegration;
		this.reachableIntegration = reachableIntegration;
		this.properties = properties;
	}

	/**
	 * Send a digital mail to a recipient
	 *
	 * @param  requestDto containing message and recipient
	 * @return            Response whether the sending went ok or not.
	 */
	public DigitalMailResponse sendDigitalMail(final DigitalMailDto requestDto, final String municipalityId) {
		var senderProperties = getSenderProperties(requestDto.getSender());

		PdfCompressor.compress(requestDto.getAttachments());
		final var legalId = partyIntegration.getLegalId(municipalityId, requestDto.getPartyId());

		final var possibleMailbox = reachableIntegration.isReachable(senderProperties, legalId);

		// We will always only have one here if no exception has been thrown, then we wouldn't be here
		final var mailbox = possibleMailbox.getFirst();
		requestDto.setRecipientId(mailbox.recipientId());

		// Send message, since the serviceAddress may differ we set this as a parameter into the integration.
		return digitalMailIntegration.sendDigitalMail(senderProperties, requestDto, mailbox.serviceAddress());
	}

	public MinaMeddelandenProperties.Sender getSenderProperties(final String name) {
		return properties.senders().stream()
			.filter(sender -> sender.name().equals(name))
			.findFirst()
			.orElseGet(() -> properties.senders().stream()
				.filter(sender -> sender.name().equals(DEFAULT_SENDER_NAME))
				.findFirst().orElseThrow(() -> Problem.valueOf(INTERNAL_SERVER_ERROR, "Couldn't find default sender properties")));
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
			final var senderProperties = getSenderProperties(DEFAULT_SENDER_NAME);
			final var legalId = partyIntegration.getLegalId(municipalityId, partyId);
			// If this doesn't throw an exception, the recipient has an available mailbox
			reachableIntegration.isReachable(senderProperties, legalId);

			return true;
		} catch (final Exception e) {
			LOGGER.error("Couldn't get legalId from party", e);
			return false;
		}
	}

}
