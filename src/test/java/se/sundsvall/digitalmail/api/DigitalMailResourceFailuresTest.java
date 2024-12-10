package se.sundsvall.digitalmail.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON;
import static org.springframework.web.reactive.function.BodyInserters.fromValue;
import static org.zalando.problem.Status.BAD_REQUEST;
import static se.sundsvall.digitalmail.TestObjectFactory.generateDigitalMailRequestDtoWithHtmlBody;
import static se.sundsvall.digitalmail.TestObjectFactory.generateInvoiceRequest;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.zalando.problem.Problem;
import org.zalando.problem.violations.ConstraintViolationProblem;
import org.zalando.problem.violations.Violation;
import se.sundsvall.digitalmail.DigitalMail;
import se.sundsvall.digitalmail.api.model.validation.HtmlValidator;
import se.sundsvall.digitalmail.integration.kivra.InvoiceDto;
import se.sundsvall.digitalmail.service.DigitalMailService;

@ActiveProfiles("junit")
@SpringBootTest(classes = DigitalMail.class, webEnvironment = RANDOM_PORT)
class DigitalMailResourceFailuresTest {

	private static final String MUNICIPALITY_ID = "2281";

	private static final String SEND_DIGITAL_MAIL_PATH = "/" + MUNICIPALITY_ID + "/send-digital-mail";

	private static final String SEND_DIGITAL_INVOICE_PATH = "/" + MUNICIPALITY_ID + "/send-digital-invoice";

	private static final String HAS_AVAILABLE_MAILBOX_PATH = "/" + MUNICIPALITY_ID + "/has-available-mailbox/{partyId}";

	@MockitoBean
	private HtmlValidator mockHtmlValidator;

	@MockitoBean
	private DigitalMailService mockDigitalMailService;

	@Autowired
	private WebTestClient webTestClient;

	@Test
	void sendDigitalMailWithInvalidHtmlThrowsProblem() {
		final var request = generateDigitalMailRequestDtoWithHtmlBody();

		when(mockHtmlValidator.validate(any(String.class))).thenReturn(false);

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

		verify(mockDigitalMailService, never()).sendDigitalInvoice(any(InvoiceDto.class), any(String.class));
		verify(mockHtmlValidator, times(1)).validate(any(String.class));
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
	}

	@Test
	void hasAvailableMailboxWhenRecipientHasNone() {
		when(mockDigitalMailService.verifyRecipientHasSomeAvailableMailbox(any(String.class), any(String.class))).thenReturn(false);

		webTestClient.post()
			.uri(HAS_AVAILABLE_MAILBOX_PATH, UUID.randomUUID().toString())
			.exchange()
			.expectStatus().isNotFound()
			.expectBody().isEmpty();

		verify(mockDigitalMailService, times(1)).verifyRecipientHasSomeAvailableMailbox(any(String.class), any(String.class));
	}

	@Test
	void hasAvailableMailboxWithInvalidPartyId() {
		final var problem = webTestClient.post()
			.uri(HAS_AVAILABLE_MAILBOX_PATH, "not-a-uuid")
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
			.containsExactlyInAnyOrder(tuple("hasAvailableMailbox.partyId", "not a valid UUID"));
	}

	@Test
	void setHasAvailableMailboxWithInvalidMunicipalityId() {
		final var problem = webTestClient.post()
			.uri(HAS_AVAILABLE_MAILBOX_PATH.replace(MUNICIPALITY_ID, "22-81"), UUID.randomUUID().toString())
			.exchange()
			.expectStatus().isBadRequest()
			.expectHeader().contentType(APPLICATION_PROBLEM_JSON)
			.expectBody(ConstraintViolationProblem.class)
			.returnResult();

		assertThat(problem.getResponseBody()).isNotNull();
		assertThat(problem.getResponseBody().getTitle()).isEqualTo("Constraint Violation");
		assertThat(problem.getResponseBody().getStatus()).isEqualTo(BAD_REQUEST);
		assertThat(problem.getResponseBody().getViolations()).extracting(Violation::getField, Violation::getMessage)
			.containsExactlyInAnyOrder(tuple("hasAvailableMailbox.municipalityId", "not a valid municipality ID"));
	}

}
