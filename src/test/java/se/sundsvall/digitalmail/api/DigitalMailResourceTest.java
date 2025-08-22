package se.sundsvall.digitalmail.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.web.reactive.function.BodyInserters.fromValue;
import static se.sundsvall.digitalmail.TestObjectFactory.generateDigitalMailRequestDto;
import static se.sundsvall.digitalmail.TestObjectFactory.generateInvoiceRequest;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import se.sundsvall.digitalmail.Application;
import se.sundsvall.digitalmail.api.model.DeliveryStatus;
import se.sundsvall.digitalmail.api.model.DigitalInvoiceResponse;
import se.sundsvall.digitalmail.api.model.DigitalMailResponse;
import se.sundsvall.digitalmail.api.model.Mailbox;
import se.sundsvall.digitalmail.api.model.validation.HtmlValidator;
import se.sundsvall.digitalmail.integration.kivra.InvoiceDto;
import se.sundsvall.digitalmail.integration.skatteverket.DigitalMailDto;
import se.sundsvall.digitalmail.service.DigitalMailService;

@ActiveProfiles("junit")
@SpringBootTest(classes = Application.class, webEnvironment = RANDOM_PORT)
class DigitalMailResourceTest {

	private static final String MUNICIPALITY_ID = "2281";

	private static final String ORGANIZATION_NUMBER = "2120001472";

	private static final String SEND_DIGITAL_MAIL_PATH = "/" + MUNICIPALITY_ID + "/{organizationNumber}/send-digital-mail";

	private static final String SEND_DIGITAL_INVOICE_PATH = "/" + MUNICIPALITY_ID + "/send-digital-invoice";

	private static final String HAS_AVAILABLE_MAILBOXES_PATH = "/" + MUNICIPALITY_ID + "/{organizationNumber}/mailboxes";

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
	void sendDigitalMail() {
		final var request = generateDigitalMailRequestDto();
		final var response = DigitalMailResponse.builder()
			.withDeliveryStatus(DeliveryStatus.builder()
				.withDelivered(true)
				.withPartyId(request.getPartyId())
				.withTransactionId("someTransactionId")
				.build())
			.build();

		when(mockDigitalMailService.sendDigitalMail(any(DigitalMailDto.class), anyString(), anyString())).thenReturn(response);

		final var result = webTestClient.post()
			.uri(SEND_DIGITAL_MAIL_PATH, ORGANIZATION_NUMBER)
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

		verify(mockDigitalMailService).sendDigitalMail(any(DigitalMailDto.class), anyString(), anyString());
		verifyNoInteractions(mockHtmlValidator);
	}

	@Test
	void sendDigitalInvoice() {
		final var request = generateInvoiceRequest();
		final var response = new DigitalInvoiceResponse(request.partyId(), true);

		when(mockDigitalMailService.sendDigitalInvoice(any(InvoiceDto.class), anyString())).thenReturn(response);

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

		verify(mockDigitalMailService, times(1)).sendDigitalInvoice(any(InvoiceDto.class), anyString());
		verifyNoInteractions(mockHtmlValidator);
	}

	@Test
	void hasAvailableMailboxes() {
		final var partyId = UUID.randomUUID().toString();
		final var supplier = "Kivra";
		final var mailbox = Mailbox.builder().withReachable(true).withSupplier(supplier).withPartyId(partyId).build();

		when(mockDigitalMailService.getRecipientsMailboxes(List.of(partyId), MUNICIPALITY_ID, ORGANIZATION_NUMBER)).thenReturn(List.of(mailbox));

		var response = webTestClient.post()
			.uri(HAS_AVAILABLE_MAILBOXES_PATH, ORGANIZATION_NUMBER)
			.bodyValue(List.of(partyId))
			.exchange()
			.expectStatus().isOk()
			.expectBodyList(Mailbox.class)
			.returnResult()
			.getResponseBody();

		assertThat(response)
			.isNotNull()
			.extracting(Mailbox::getPartyId, Mailbox::isReachable, Mailbox::getSupplier)
			.containsExactly(
				tuple(partyId, true, supplier));

		verify(mockDigitalMailService, times(1)).getRecipientsMailboxes(List.of(partyId), MUNICIPALITY_ID, ORGANIZATION_NUMBER);
		verifyNoInteractions(mockHtmlValidator);
	}
}
