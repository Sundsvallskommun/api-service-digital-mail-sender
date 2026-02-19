package se.sundsvall.digitalmail.api.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DigitalMailResponseTests {

	@Test
	void gettersAndSetters() {
		final var digitalMailResponse = new DigitalMailResponse();
		digitalMailResponse.setDeliveryStatus(new DeliveryStatus(false, "somePartyId", "someTransactionId"));

		assertThat(digitalMailResponse.getDeliveryStatus()).isNotNull();
	}
}
