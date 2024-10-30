package se.sundsvall.digitalmail.integration.skatteverket;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.TEXT_PLAIN_VALUE;

import java.util.ArrayList;

import org.junit.jupiter.api.Test;

import se.sundsvall.digitalmail.api.model.BodyInformation;
import se.sundsvall.digitalmail.api.model.DigitalMailRequest;
import se.sundsvall.digitalmail.api.model.SupportInfo;

class DigitalMailDtoTest {

	@Test
	void testConstructor() {
		final var request = DigitalMailRequest.builder()
			.withPartyId("partyId")
			.withHeaderSubject("Subject")
			.withSupportInfo(new SupportInfo())
			.withAttachments(new ArrayList<>())
			.withBodyInformation(BodyInformation.builder().withContentType(TEXT_PLAIN_VALUE).build())
			.build();

		final var dto = new DigitalMailDto(request);

		assertThat(dto.getPartyId()).isEqualTo("partyId");
		assertThat(dto.getHeaderSubject()).isEqualTo("Subject");
		assertThat(dto.getSupportInfo()).isNotNull();
		assertThat(dto.getAttachments()).isNotNull();
		assertThat(dto.getBodyInformation()).isNotNull();
	}
}
