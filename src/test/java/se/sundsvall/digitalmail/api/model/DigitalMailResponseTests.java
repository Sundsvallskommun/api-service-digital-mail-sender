package se.sundsvall.digitalmail.api.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class DigitalMailResponseTests {

    @Test
    void gettersAndSetters() {
        final var digitalMailResponse = new DigitalMailResponse();
        digitalMailResponse.setDeliveryStatus(new DeliveryStatus(false, "somePartyId", "someTransactionId"));

        assertThat(digitalMailResponse.getDeliveryStatus()).isNotNull();
    }
}
