package se.sundsvall.digitalmail.integration.skatteverket;

import se.sundsvall.digitalmail.api.model.DigitalMailRequest;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DigitalMailDto extends DigitalMailRequest {

	private String recipientId; // Recipient id from e.g. kivra.

	public DigitalMailDto(final DigitalMailRequest request) {
		super(request.getPartyId(), request.getMunicipalityId(), request.getHeaderSubject(), request.getSupportInfo(), request.getAttachments(), request.getBodyInformation());
	}
}
