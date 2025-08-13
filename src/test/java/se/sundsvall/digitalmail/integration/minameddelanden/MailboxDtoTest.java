package se.sundsvall.digitalmail.integration.minameddelanden;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class MailboxDtoTest {

	@Test
	void testConstructor() {
		var recipientId = "recipientId";
		var serviceAddress = "serviceAddress";
		var serviceName = "serviceName";
		final var dto = new MailboxDto(recipientId, serviceAddress, serviceName);

		assertThat(dto.recipientId()).isEqualTo(recipientId);
		assertThat(dto.serviceAddress()).isEqualTo(serviceAddress);
		assertThat(dto.serviceName()).isEqualTo(serviceName);

	}
}
