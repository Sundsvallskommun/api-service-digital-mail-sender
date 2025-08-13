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
		final var bodyInformation = BodyInformation.builder()
			.withContentType(TEXT_PLAIN_VALUE)
			.build();

		final var supportInfo = SupportInfo.builder()
			.withSupportText("supportText")
			.build();

		final var request = DigitalMailRequest.builder()
			.withSender("sender")
			.withPartyId("partyId")
			.withHeaderSubject("Subject")
			.withSupportInfo(supportInfo)
			.withAttachments(new ArrayList<>())
			.withBodyInformation(bodyInformation)
			.build();

		final var dto = new DigitalMailDto(request);

		assertThat(dto.getSender()).isEqualTo("sender");
		assertThat(dto.getPartyId()).isEqualTo("partyId");
		assertThat(dto.getHeaderSubject()).isEqualTo("Subject");
		assertThat(dto.getSupportInfo()).isEqualTo(supportInfo);
		assertThat(dto.getBodyInformation()).isEqualTo(bodyInformation);
		assertThat(dto.getAttachments()).isNotNull().isEmpty();
	}
}
