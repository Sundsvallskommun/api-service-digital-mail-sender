package se.sundsvall.digitalmail.api;

import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.zalando.problem.Problem;
import org.zalando.problem.violations.ConstraintViolationProblem;
import org.zalando.problem.violations.Violation;
import se.sundsvall.digitalmail.Application;
import se.sundsvall.digitalmail.api.model.validation.HtmlValidator;
import se.sundsvall.digitalmail.service.DigitalMailService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON;
import static org.springframework.web.reactive.function.BodyInserters.fromValue;
import static org.zalando.problem.Status.BAD_REQUEST;
import static se.sundsvall.digitalmail.TestObjectFactory.generateDigitalMailRequestDtoWithHtmlBody;
import static se.sundsvall.digitalmail.TestObjectFactory.generateInvoiceRequest;

@ActiveProfiles("junit")
@SpringBootTest(classes = Application.class, webEnvironment = RANDOM_PORT)
class DigitalMailResourceFailuresTest {

	private static final String MUNICIPALITY_ID = "2281";

	private static final String ORGANIZATION_NUMBER = "2120002411";

	private static final String SEND_DIGITAL_MAIL_PATH = "/" + MUNICIPALITY_ID + "/" + ORGANIZATION_NUMBER + "/send-digital-mail";

	private static final String SEND_DIGITAL_INVOICE_PATH = "/" + MUNICIPALITY_ID + "/send-digital-invoice";

	private static final String HAS_AVAILABLE_MAILBOXES_PATH = "/" + MUNICIPALITY_ID + "/" + ORGANIZATION_NUMBER + "/mailboxes";

	@MockitoBean
	private HtmlValidator mockHtmlValidator;

	@MockitoBean
	private DigitalMailService mockDigitalMailService;

	@Autowired
	private WebTestClient webTestClient;

	@AfterEach
	void tearDown() {
		verifyNoMoreInteractions(mockDigitalMailService, mockHtmlValidator);
	}

	@Test
	void sendDigitalMailWithInvalidHtmlThrowsProblem() {
		final var request = generateDigitalMailRequestDtoWithHtmlBody();

		when(mockHtmlValidator.validate(anyString())).thenReturn(false);

		final var result = webTestClient.post()
			.uri(SEND_DIGITAL_MAIL_PATH)
			.contentType(APPLICATION_JSON)
			.body(fromValue(request))
			.exchange()
			.expectStatus()
			.isBadRequest()
			.expectBody(Problem.class)
			.returnResult()
			.getResponseBody();

		assertThat(result).isNotNull();

		verifyNoInteractions(mockDigitalMailService);
		verify(mockHtmlValidator).validate(anyString());
	}

	@Test
	void sendDigitalMailWithInvalidMunicipalityId() {

		final var request = generateDigitalMailRequestDtoWithHtmlBody();

		final var problem = webTestClient.post()
			.uri(SEND_DIGITAL_MAIL_PATH.replace(MUNICIPALITY_ID, "22-81"))
			.contentType(APPLICATION_JSON)
			.body(fromValue(request))
			.exchange()
			.expectStatus().isBadRequest()
			.expectHeader().contentType(APPLICATION_PROBLEM_JSON)
			.expectBody(ConstraintViolationProblem.class)
			.returnResult();

		assertThat(problem.getResponseBody()).isNotNull();
		assertThat(problem.getResponseBody().getTitle()).isEqualTo("Constraint Violation");
		assertThat(problem.getResponseBody().getStatus()).isEqualTo(BAD_REQUEST);
		assertThat(problem.getResponseBody().getViolations()).extracting(Violation::getField, Violation::getMessage)
			.containsExactlyInAnyOrder(tuple("sendDigitalMail.municipalityId", "not a valid municipality ID"));

		verifyNoInteractions(mockDigitalMailService, mockHtmlValidator);
	}

	@Test
	void sendDigitalMailWithInvalidOrganizationNumber() {

		final var request = generateDigitalMailRequestDtoWithHtmlBody();

		final var problem = webTestClient.post()
			.uri(SEND_DIGITAL_MAIL_PATH.replace(ORGANIZATION_NUMBER, "invalid"))
			.contentType(APPLICATION_JSON)
			.body(fromValue(request))
			.exchange()
			.expectStatus().isBadRequest()
			.expectHeader().contentType(APPLICATION_PROBLEM_JSON)
			.expectBody(ConstraintViolationProblem.class)
			.returnResult();

		assertThat(problem.getResponseBody()).isNotNull();
		assertThat(problem.getResponseBody().getTitle()).isEqualTo("Constraint Violation");
		assertThat(problem.getResponseBody().getStatus()).isEqualTo(BAD_REQUEST);
		assertThat(problem.getResponseBody().getViolations()).extracting(Violation::getField, Violation::getMessage)
			.containsExactlyInAnyOrder(
				tuple("sendDigitalMail.organizationNumber", "must match the regular expression ^([1235789][\\d][2-9]\\d{7})$"),
				tuple("sendDigitalMail.organizationNumber", "Sending organization is not registered as authorized sender"));

		verifyNoInteractions(mockDigitalMailService, mockHtmlValidator);
	}

	@Test
	void sendDigitalInvoiceWithInvalidMunicipalityId() {
		final var request = generateInvoiceRequest();

		final var problem = webTestClient.post()
			.uri(SEND_DIGITAL_INVOICE_PATH.replace(MUNICIPALITY_ID, "22-81"))
			.contentType(APPLICATION_JSON)
			.body(fromValue(request))
			.exchange()
			.expectStatus().isBadRequest()
			.expectHeader().contentType(APPLICATION_PROBLEM_JSON)
			.expectBody(ConstraintViolationProblem.class)
			.returnResult();

		assertThat(problem.getResponseBody()).isNotNull();
		assertThat(problem.getResponseBody().getTitle()).isEqualTo("Constraint Violation");
		assertThat(problem.getResponseBody().getStatus()).isEqualTo(BAD_REQUEST);
		assertThat(problem.getResponseBody().getViolations()).extracting(Violation::getField, Violation::getMessage)
			.containsExactlyInAnyOrder(tuple("sendDigitalInvoice.municipalityId", "not a valid municipality ID"));

		verifyNoInteractions(mockDigitalMailService, mockHtmlValidator);
	}

	@Test
	void hasAvailableMailboxesWithInvalidMunicipalityId() {
		final var problem = webTestClient.post()
			.uri(HAS_AVAILABLE_MAILBOXES_PATH.replace(MUNICIPALITY_ID, "invalid"))
			.bodyValue(List.of(UUID.randomUUID().toString()))
			.exchange()
			.expectStatus().isBadRequest()
			.expectHeader().contentType(APPLICATION_PROBLEM_JSON)
			.expectBody(ConstraintViolationProblem.class)
			.returnResult();

		assertThat(problem.getResponseBody()).isNotNull();
		assertThat(problem.getResponseBody().getTitle()).isEqualTo("Constraint Violation");
		assertThat(problem.getResponseBody().getStatus()).isEqualTo(BAD_REQUEST);
		assertThat(problem.getResponseBody().getViolations()).extracting(Violation::getField, Violation::getMessage)
			.containsExactlyInAnyOrder(tuple("hasAvailableMailboxes.municipalityId", "not a valid municipality ID"));

		verifyNoInteractions(mockDigitalMailService, mockHtmlValidator);
	}

	@Test
	void hasAvailableMailboxesWithFaultyOrganizationNumber() {
		final var problem = webTestClient.post()
			.uri(HAS_AVAILABLE_MAILBOXES_PATH.replace(ORGANIZATION_NUMBER, "invalid"))
			.bodyValue(List.of(UUID.randomUUID().toString()))
			.exchange()
			.expectStatus().isBadRequest()
			.expectHeader().contentType(APPLICATION_PROBLEM_JSON)
			.expectBody(ConstraintViolationProblem.class)
			.returnResult();

		assertThat(problem.getResponseBody()).isNotNull();
		assertThat(problem.getResponseBody().getTitle()).isEqualTo("Constraint Violation");
		assertThat(problem.getResponseBody().getStatus()).isEqualTo(BAD_REQUEST);
		assertThat(problem.getResponseBody().getViolations()).extracting(Violation::getField, Violation::getMessage)
			.containsExactlyInAnyOrder(
				tuple("hasAvailableMailboxes.organizationNumber", "must match the regular expression ^([1235789][\\d][2-9]\\d{7})$"),
				tuple("hasAvailableMailboxes.organizationNumber", "Sending organization is not registered as authorized sender"));

		verifyNoInteractions(mockDigitalMailService, mockHtmlValidator);
	}

	@ParameterizedTest
	@MethodSource("invalidPartyIdsProvider")
	void hasAvailableMailboxesWithInvalidPartyIds(final List<String> partyIds, String field, String message) {
		final var problem = webTestClient.post()
			.uri(HAS_AVAILABLE_MAILBOXES_PATH)
			.bodyValue(partyIds)
			.exchange()
			.expectStatus().isBadRequest()
			.expectHeader().contentType(APPLICATION_PROBLEM_JSON)
			.expectBody(ConstraintViolationProblem.class)
			.returnResult()
			.getResponseBody();

		assertThat(problem).isNotNull();
		assertThat(problem.getStatus()).isEqualTo(BAD_REQUEST);
		assertThat(problem.getTitle()).isEqualTo("Constraint Violation");
		assertThat(problem.getViolations()).extracting(Violation::getField, Violation::getMessage)
			.containsExactlyInAnyOrder(tuple(field, message));

		verifyNoInteractions(mockDigitalMailService, mockHtmlValidator);
	}

	private static Stream<Arguments> invalidPartyIdsProvider() {
		final var field = "hasAvailableMailboxes.partyIds[0].<list element>";
		final var message = "not a valid UUID";
		final var uuid = UUID.randomUUID().toString();
		return Stream.of(
			Arguments.of(List.of("not-a-uuid"), field, message),
			Arguments.of(List.of("not-a-uuid", uuid), field, message),
			Arguments.of(List.of(""), field, message),
			Arguments.of(List.of(), "hasAvailableMailboxes.partyIds", "must not be empty"),
			Arguments.of(List.of(uuid, uuid), "hasAvailableMailboxes.partyIds", "must only contain unique elements"));
	}
}
