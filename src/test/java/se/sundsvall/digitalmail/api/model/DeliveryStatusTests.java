package se.sundsvall.digitalmail.api.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class DeliveryStatusTests {

    @Test
    void creationAndGetters() {
        final var partyId = "somePartyId";
        final var transactionId = "someTxId";
        final var deliveryStatus = new DeliveryStatus(true, partyId, transactionId);

        assertThat(deliveryStatus.isDelivered()).isTrue();
        assertThat(deliveryStatus.getPartyId()).isEqualTo(partyId);
        assertThat(deliveryStatus.getTransactionId()).isEqualTo(transactionId);
    }
}
