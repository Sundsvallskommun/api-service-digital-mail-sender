package se.sundsvall.digitalmail.integration.skatteverket;

import lombok.Getter;
import lombok.Setter;
import se.sundsvall.digitalmail.api.model.DigitalMailRequest;

@Getter
@Setter
public class DigitalMailDto extends DigitalMailRequest {

	private String recipientId; // Recipient id from e.g. kivra.

	public DigitalMailDto(final DigitalMailRequest request) {
		super(request.getPartyId(), request.getMunicipalityId(), request.getHeaderSubject(), request.getSupportInfo(), request.getAttachments(), request.getBodyInformation());
	}
}
