package se.sundsvall.digitalmail.integration.minameddelanden;

import java.util.Objects;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import se.sundsvall.digitalmail.api.model.DigitalMailRequest;

@Getter
@Setter
@NoArgsConstructor
public class DigitalMailDto extends DigitalMailRequest {

	private String recipientId; // Recipient id from e.g. kivra.

	public DigitalMailDto(final DigitalMailRequest request) {
		super(request.getSender(), request.getPartyId(), request.getMunicipalityId(), request.getHeaderSubject(), request.getSupportInfo(), request.getAttachments(), request.getBodyInformation());
	}

	@Override
	public boolean equals(Object o) {
		if (o == null || getClass() != o.getClass())
			return false;
		if (!super.equals(o))
			return false;
		DigitalMailDto that = (DigitalMailDto) o;
		return Objects.equals(recipientId, that.recipientId);
	}

	@Override
	public int hashCode() {
		return Objects.hash(super.hashCode(), recipientId);
	}
}
