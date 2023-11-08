package se.sundsvall.digitalmail.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON;
import static org.springframework.web.reactive.function.BodyInserters.fromValue;
import static org.zalando.problem.Status.BAD_REQUEST;
import static se.sundsvall.digitalmail.TestObjectFactory.generateDigitalMailRequestDto;
import static se.sundsvall.digitalmail.TestObjectFactory.generateInvoiceRequest;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.zalando.problem.violations.ConstraintViolationProblem;
import org.zalando.problem.violations.Violation;

import se.sundsvall.digitalmail.DigitalMail;
import se.sundsvall.digitalmail.api.model.DeliveryStatus;
import se.sundsvall.digitalmail.api.model.DigitalInvoiceResponse;
import se.sundsvall.digitalmail.api.model.DigitalMailResponse;
import se.sundsvall.digitalmail.integration.kivra.InvoiceDto;
import se.sundsvall.digitalmail.integration.skatteverket.DigitalMailDto;
import se.sundsvall.digitalmail.service.DigitalMailService;

@ActiveProfiles("junit")
@SpringBootTest(classes = DigitalMail.class, webEnvironment = RANDOM_PORT)
class DigitalMailResourceTests {

    @MockBean
    private DigitalMailService mockDigitalMailService;

    @Autowired
    private WebTestClient webTestClient;

    @Test
    void sendDigitalMail() {
        var request = generateDigitalMailRequestDto();
        var response = DigitalMailResponse.builder()
            .withDeliveryStatus(DeliveryStatus.builder()
                .withDelivered(true)
                .withPartyId(request.getPartyId())
                .withTransactionId("someTransactionId")
                .build())
            .build();

        when(mockDigitalMailService.sendDigitalMail(any(DigitalMailDto.class))).thenReturn(response);

        var result = webTestClient.post()
            .uri("/send-digital-mail")
            .contentType(APPLICATION_JSON)
            .body(fromValue(request))
            .exchange()
            .expectStatus()
            .isOk()
            .expectBody(DigitalMailResponse.class)
            .returnResult()
            .getResponseBody();

        assertThat(result).isNotNull();
        assertThat(result.getDeliveryStatus()).isNotNull().satisfies(resultDeliverystatus -> {
            assertThat(resultDeliverystatus.isDelivered()).isEqualTo(response.getDeliveryStatus().isDelivered());
            assertThat(resultDeliverystatus.getPartyId()).isEqualTo(response.getDeliveryStatus().getPartyId());
            assertThat(resultDeliverystatus.getTransactionId()).isEqualTo(response.getDeliveryStatus().getTransactionId());
        });

        verify(mockDigitalMailService, times(1)).sendDigitalMail(any(DigitalMailDto.class));
    }

    @Test
    void sendDigitalInvoice() {
        var request = generateInvoiceRequest();
        var response = new DigitalInvoiceResponse(request.partyId(), true);

        when(mockDigitalMailService.sendDigitalInvoice(any(InvoiceDto.class))).thenReturn(response);

        var result = webTestClient.post()
            .uri("/send-digital-invoice")
            .contentType(APPLICATION_JSON)
            .body(fromValue(request))
            .exchange()
            .expectStatus()
            .isOk()
            .expectBody(DigitalInvoiceResponse.class)
            .returnResult()
            .getResponseBody();

        assertThat(result).isNotNull();
        assertThat(result.partyId()).isEqualTo(response.partyId());
        assertThat(result.sent()).isEqualTo(response.sent());

        verify(mockDigitalMailService, times(1)).sendDigitalInvoice(any(InvoiceDto.class));
    }

    @Test
    void hasAvailableMailbox() {
        when(mockDigitalMailService.verifyRecipientHasSomeAvailableMailbox(any(String.class))).thenReturn(true);

        webTestClient.post()
            .uri("/has-available-mailbox/{partyId}", UUID.randomUUID().toString())
            .exchange()
            .expectStatus().isOk()
            .expectBody().isEmpty();

        verify(mockDigitalMailService, times(1)).verifyRecipientHasSomeAvailableMailbox(any(String.class));
    }

    @Test
    void hasAvailableMailboxWhenRecipientHasNone() {
        when(mockDigitalMailService.verifyRecipientHasSomeAvailableMailbox(any(String.class))).thenReturn(false);

        webTestClient.post()
            .uri("/has-available-mailbox/{partyId}", UUID.randomUUID().toString())
            .exchange()
            .expectStatus().isNotFound()
            .expectBody().isEmpty();

        verify(mockDigitalMailService, times(1)).verifyRecipientHasSomeAvailableMailbox(any(String.class));
    }

    @Test
    void hasAvailableMailboxWithInvalidPartyId() {
        var problem = webTestClient.post()
            .uri("/has-available-mailbox/{partyId}", "not-a-uuid")
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
}