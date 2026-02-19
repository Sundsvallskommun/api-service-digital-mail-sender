package se.sundsvall.digitalmail.api.model;

import java.util.List;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Test;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEquals;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCode;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToString;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.springframework.http.MediaType.TEXT_PLAIN_VALUE;

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
		final var partyId = "somePartyId";
		final var municipalityId = "someMunicipalityId";
		final var headerSubject = "someSubject";

		final var digitalMailRequest = new DigitalMailRequest();
		digitalMailRequest.setPartyId(partyId);
		digitalMailRequest.setMunicipalityId(municipalityId);
		digitalMailRequest.setHeaderSubject(headerSubject);
		digitalMailRequest.setSupportInfo(new SupportInfo());
		digitalMailRequest.setBodyInformation(BodyInformation.builder().withContentType(TEXT_PLAIN_VALUE).build());
		digitalMailRequest.setAttachments(List.of(new File(), new File()));

		assertThat(digitalMailRequest.getPartyId()).isEqualTo(partyId);
		assertThat(digitalMailRequest.getMunicipalityId()).isEqualTo(municipalityId);
		assertThat(digitalMailRequest.getHeaderSubject()).isEqualTo(headerSubject);
		assertThat(digitalMailRequest.getSupportInfo()).isNotNull();
		assertThat(digitalMailRequest.getBodyInformation()).isNotNull();
		assertThat(digitalMailRequest.getAttachments()).isNotNull().hasSize(2);
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(DigitalMailRequest.builder().build()).hasAllNullFieldsOrPropertiesExcept("attachments").satisfies(
			digitalMailRequest -> assertThat(digitalMailRequest.getAttachments()).isNotNull().isEmpty());
		assertThat(new DigitalMailRequest()).hasAllNullFieldsOrPropertiesExcept("attachments").satisfies(
			digitalMailRequest -> assertThat(digitalMailRequest.getAttachments()).isNotNull().isEmpty());
	}
}
