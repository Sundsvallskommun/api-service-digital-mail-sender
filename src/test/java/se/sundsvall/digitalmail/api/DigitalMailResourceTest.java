package se.sundsvall.digitalmail.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.web.reactive.function.BodyInserters.fromValue;
import static se.sundsvall.digitalmail.TestObjectFactory.generateDigitalMailRequestDto;
import static se.sundsvall.digitalmail.TestObjectFactory.generateInvoiceRequest;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;

import se.sundsvall.digitalmail.DigitalMail;
import se.sundsvall.digitalmail.api.model.DeliveryStatus;
import se.sundsvall.digitalmail.api.model.DigitalInvoiceResponse;
import se.sundsvall.digitalmail.api.model.DigitalMailResponse;
import se.sundsvall.digitalmail.api.model.validation.HtmlValidator;
import se.sundsvall.digitalmail.integration.kivra.InvoiceDto;
import se.sundsvall.digitalmail.integration.skatteverket.DigitalMailDto;
import se.sundsvall.digitalmail.service.DigitalMailService;

@ActiveProfiles("junit")
@SpringBootTest(classes = DigitalMail.class, webEnvironment = RANDOM_PORT)
class DigitalMailResourceTest {

	private static final String MUNICIPALITY_ID = "2281";

	private static final String SEND_DIGITAL_MAIL_PATH = "/" + MUNICIPALITY_ID + "/send-digital-mail";

	private static final String SEND_DIGITAL_INVOICE_PATH = "/" + MUNICIPALITY_ID + "/send-digital-invoice";

	private static final String HAS_AVAILABLE_MAILBOX_PATH = "/" + MUNICIPALITY_ID + "/has-available-mailbox/{partyId}";

	@MockBean
	private HtmlValidator mockHtmlValidator;

	@MockBean
	private DigitalMailService mockDigitalMailService;

	@Autowired
	private WebTestClient webTestClient;

	@Test
	void sendDigitalMail() {
		final var request = generateDigitalMailRequestDto();
		final var response = DigitalMailResponse.builder()
			.withDeliveryStatus(DeliveryStatus.builder()
				.withDelivered(true)
				.withPartyId(request.getPartyId())
				.withTransactionId("someTransactionId")
				.build())
			.build();

		when(mockDigitalMailService.sendDigitalMail(any(DigitalMailDto.class), any(String.class))).thenReturn(response);

		final var result = webTestClient.post()
			.uri(SEND_DIGITAL_MAIL_PATH)
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

		verify(mockDigitalMailService, times(1)).sendDigitalMail(any(DigitalMailDto.class), any(String.class));
		verify(mockHtmlValidator, never()).validate(any(String.class));
	}

	@Test
	void sendDigitalInvoice() {
		final var request = generateInvoiceRequest();
		final var response = new DigitalInvoiceResponse(request.partyId(), true);

		when(mockDigitalMailService.sendDigitalInvoice(any(InvoiceDto.class), any(String.class))).thenReturn(response);

		final var result = webTestClient.post()
			.uri(SEND_DIGITAL_INVOICE_PATH)
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

		verify(mockDigitalMailService, times(1)).sendDigitalInvoice(any(InvoiceDto.class), any(String.class));
	}

	@Test
	void hasAvailableMailbox() {
		when(mockDigitalMailService.verifyRecipientHasSomeAvailableMailbox(any(String.class), any(String.class))).thenReturn(true);

		webTestClient.post()
			.uri(HAS_AVAILABLE_MAILBOX_PATH, UUID.randomUUID().toString())
			.exchange()
			.expectStatus().isOk()
			.expectBody().isEmpty();

		verify(mockDigitalMailService, times(1)).verifyRecipientHasSomeAvailableMailbox(any(String.class), any(String.class));
	}

}
