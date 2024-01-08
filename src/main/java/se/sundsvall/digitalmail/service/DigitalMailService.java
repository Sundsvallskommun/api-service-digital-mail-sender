package se.sundsvall.digitalmail.service;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import se.sundsvall.digitalmail.api.model.DigitalInvoiceResponse;
import se.sundsvall.digitalmail.api.model.DigitalMailResponse;
import se.sundsvall.digitalmail.integration.kivra.InvoiceDto;
import se.sundsvall.digitalmail.integration.kivra.KivraIntegration;
import se.sundsvall.digitalmail.integration.party.PartyClient;
import se.sundsvall.digitalmail.integration.skatteverket.DigitalMailDto;
import se.sundsvall.digitalmail.integration.skatteverket.sendmail.DigitalMailIntegration;

@Service
public class DigitalMailService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DigitalMailService.class);
    
    private final PartyClient partyClient;
    private final DigitalMailIntegration digitalMailIntegration;
    private final KivraIntegration kivraIntegration;
    private final AvailabilityService availabilityService;

    DigitalMailService(final PartyClient partyClient,
            final DigitalMailIntegration digitalMailIntegration,
            final KivraIntegration kivraIntegration,
            final AvailabilityService availabilityService) {
        this.partyClient = partyClient;
        this.digitalMailIntegration = digitalMailIntegration;
        this.kivraIntegration = kivraIntegration;
        this.availabilityService = availabilityService;
    }
    
    /**
     * Send a digital mail to a recipient
     * @param requestDto containing message and recipient
     * @return Response whether the sending went ok or not.
     */
    public DigitalMailResponse sendDigitalMail(final DigitalMailDto requestDto) {
        final var personalNumber = partyClient.getLegalId(requestDto.getPartyId());
        
        final var possibleMailbox = availabilityService.getRecipientMailboxesAndCheckAvailability(List.of(personalNumber));

        // We will always only have one here if no exception has been thrown, then we wouldn't be here
        final var mailbox = possibleMailbox.get(0);
        requestDto.setRecipientId(mailbox.recipientId());
        
        // Send message, since the serviceAddress may differ we set this as a parameter into the integration.
        return digitalMailIntegration.sendDigitalMail(requestDto, mailbox.serviceAddress());
    }

    public DigitalInvoiceResponse sendDigitalInvoice(final InvoiceDto invoiceDto) {
        final var ssn = partyClient.getLegalId(invoiceDto.getPartyId());
        invoiceDto.setSsn(ssn);

        var result = kivraIntegration.sendInvoice(invoiceDto);

        return new DigitalInvoiceResponse(invoiceDto.getPartyId(), result);
    }

    public boolean verifyRecipientHasSomeAvailableMailbox(final String partyId) {
        try {
            final var personalNumber = partyClient.getLegalId(partyId);
            // If this doesn't throw an exception, the recipient has an available mailbox
            availabilityService.getRecipientMailboxesAndCheckAvailability(List.of(personalNumber));

            return true;
        } catch (Exception e) {
            LOGGER.error("Couldn't get legalId from party", e);
            return false;
        }
    }
}
