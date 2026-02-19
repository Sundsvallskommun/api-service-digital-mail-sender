package se.sundsvall.digitalmail.api.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DigitalInvoiceResponseTests {

	@Test
	void creationAndGetters() {
		final var partyId = "somePartyId";
		final var response = new DigitalInvoiceResponse(partyId, true);

		assertThat(response.partyId()).isEqualTo(partyId);
		assertThat(response.sent()).isTrue();
	}
}
