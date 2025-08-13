package se.sundsvall.digitalmail.api.model;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEquals;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCode;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToString;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.springframework.http.MediaType.TEXT_PLAIN_VALUE;

import java.util.List;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Test;

class DigitalMailRequestTests {

	@Test
	void testBean() {
		MatcherAssert.assertThat(DigitalMailRequest.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void gettersAndSetters() {
		final var sender = "someSender";
		final var partyId = "somePartyId";
		final var municipalityId = "someMunicipalityId";
		final var headerSubject = "someSubject";

		final var bodyInformation = BodyInformation.builder()
			.withBody("someBody")
			.withContentType(TEXT_PLAIN_VALUE)
			.build();

		final var supportInfo = SupportInfo.builder()
			.withSupportText("someSupportText")
			.withContactInformationEmail("someContactInformationEmail")
			.withContactInformationUrl("http://someContactInformationUrl")
			.withContactInformationPhoneNumber("someContactInformationPhoneNumber")
			.build();

		final var digitalMailRequest = new DigitalMailRequest();
		digitalMailRequest.setSender(sender);
		digitalMailRequest.setPartyId(partyId);
		digitalMailRequest.setMunicipalityId(municipalityId);
		digitalMailRequest.setHeaderSubject(headerSubject);
		digitalMailRequest.setSupportInfo(supportInfo);
		digitalMailRequest.setBodyInformation(bodyInformation);
		digitalMailRequest.setAttachments(List.of(new File(), new File()));

		assertThat(digitalMailRequest.getSender()).isEqualTo(sender);
		assertThat(digitalMailRequest.getPartyId()).isEqualTo(partyId);
		assertThat(digitalMailRequest.getMunicipalityId()).isEqualTo(municipalityId);
		assertThat(digitalMailRequest.getHeaderSubject()).isEqualTo(headerSubject);
		assertThat(digitalMailRequest.getSupportInfo()).isEqualTo(supportInfo);
		assertThat(digitalMailRequest.getBodyInformation()).isEqualTo(bodyInformation);
		assertThat(digitalMailRequest.getAttachments()).isNotNull().hasSize(2);
		assertThat(digitalMailRequest).hasNoNullFieldsOrProperties();
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(DigitalMailRequest.builder().build()).hasAllNullFieldsOrPropertiesExcept("attachments").satisfies(
			digitalMailRequest -> assertThat(digitalMailRequest.getAttachments()).isNotNull().isEmpty());
		assertThat(new DigitalMailRequest()).hasAllNullFieldsOrPropertiesExcept("attachments").satisfies(
			digitalMailRequest -> assertThat(digitalMailRequest.getAttachments()).isNotNull().isEmpty());
	}
}
