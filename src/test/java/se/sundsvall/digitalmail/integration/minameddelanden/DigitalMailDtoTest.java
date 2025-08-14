package se.sundsvall.digitalmail.integration.minameddelanden;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEquals;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCode;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.springframework.http.MediaType.TEXT_PLAIN_VALUE;

import java.util.ArrayList;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Test;
import se.sundsvall.digitalmail.api.model.BodyInformation;
import se.sundsvall.digitalmail.api.model.DigitalMailRequest;
import se.sundsvall.digitalmail.api.model.SupportInfo;

class DigitalMailDtoTest {

	@Test
	void testBean() {
		MatcherAssert.assertThat(DigitalMailDto.class, allOf(
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals()));
	}

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
