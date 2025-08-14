package se.sundsvall.digitalmail.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.zalando.problem.Status.INTERNAL_SERVER_ERROR;
import static se.sundsvall.digitalmail.Constants.DEFAULT_SENDER_NAME;
import static se.sundsvall.digitalmail.TestObjectFactory.generateDigitalMailRequestDto;
import static se.sundsvall.digitalmail.TestObjectFactory.generateInvoiceDto;
import static se.sundsvall.digitalmail.TestObjectFactory.generateSenderProperties;

import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.zalando.problem.Problem;
import org.zalando.problem.Status;
import org.zalando.problem.ThrowableProblem;
import se.sundsvall.digitalmail.api.model.DigitalMailResponse;
import se.sundsvall.digitalmail.integration.kivra.InvoiceDto;
import se.sundsvall.digitalmail.integration.kivra.KivraIntegration;
import se.sundsvall.digitalmail.integration.minameddelanden.DigitalMailDto;
import se.sundsvall.digitalmail.integration.minameddelanden.MailboxDto;
import se.sundsvall.digitalmail.integration.minameddelanden.configuration.MinaMeddelandenProperties;
import se.sundsvall.digitalmail.integration.minameddelanden.reachable.ReachableIntegration;
import se.sundsvall.digitalmail.integration.minameddelanden.sendmail.DigitalMailIntegration;
import se.sundsvall.digitalmail.integration.party.PartyIntegration;

@ExtendWith(MockitoExtension.class)
class DigitalMailServiceTest {

	@Mock
	private PartyIntegration mockPartyIntegration;

	@Mock
	private DigitalMailIntegration mockDigitalMailIntegration;

	@Mock
	private KivraIntegration mockKivraIntegration;

	@Mock
	private ReachableIntegration mockReachableIntegration;

	@Mock
	private MinaMeddelandenProperties mockProperties;

	@Mock
	private MinaMeddelandenProperties.Sender mockSenderProperties;

	@InjectMocks
	private DigitalMailService service;

	@AfterEach
	void afterEach() {
		verifyNoMoreInteractions(mockPartyIntegration, mockDigitalMailIntegration, mockKivraIntegration, mockReachableIntegration);
	}

	@Test
	void testSendDigitalMail_shouldReturnResponse() {
		final var request = generateDigitalMailRequestDto();
		final var mailbox = new MailboxDto("recipientId", "serviceAddress", "kivra");
		final var personalNumber = "personalNumber";

		when(mockProperties.senders()).thenReturn(List.of(mockSenderProperties));
		when(mockSenderProperties.name()).thenReturn(DEFAULT_SENDER_NAME);
		when(mockPartyIntegration.getLegalId(anyString(), anyString())).thenReturn(personalNumber);
		when(mockReachableIntegration.isReachable(mockSenderProperties, personalNumber)).thenReturn(List.of(mailbox));
		when(mockDigitalMailIntegration.sendDigitalMail(eq(mockSenderProperties), any(DigitalMailDto.class), eq("serviceAddress"))).thenReturn(new DigitalMailResponse());

		final var pdfLength = request.getAttachments().getFirst().getBody().length();  // Save the length of the pdf before compression

		final var digitalMailResponse = service.sendDigitalMail(new DigitalMailDto(request), "");

		final var compressedPdfLength = request.getAttachments().getFirst().getBody().length(); // Save the length of the pdf after compression

		assertThat(compressedPdfLength).isLessThan(pdfLength);  // Check that the pdf has been compressed
		assertThat(digitalMailResponse).isNotNull();
		verify(mockPartyIntegration).getLegalId(anyString(), anyString());
		verify(mockReachableIntegration).isReachable(eq(mockSenderProperties), anyString());
		verify(mockDigitalMailIntegration).sendDigitalMail(eq(mockSenderProperties), any(DigitalMailDto.class), eq("serviceAddress"));
	}

	// Same thing will happen if any integration throws an exception so will only test one.
	@Test
	void testSendDigitalMail_partyThrowsExceptionShouldThrowProblem() {
		final var request = generateDigitalMailRequestDto();
		final var senderProperties = generateSenderProperties();

		when(mockProperties.senders()).thenReturn(List.of(mockSenderProperties));
		when(mockSenderProperties.name()).thenReturn(DEFAULT_SENDER_NAME);
		when(mockPartyIntegration.getLegalId(anyString(), anyString())).thenThrow(Problem.builder().withStatus(INTERNAL_SERVER_ERROR).build());

		assertThatExceptionOfType(ThrowableProblem.class).isThrownBy(() -> service.sendDigitalMail(request, ""));

		verify(mockPartyIntegration).getLegalId(anyString(), anyString());
		verify(mockReachableIntegration, never()).isReachable(eq(senderProperties), anyString());
		verify(mockDigitalMailIntegration, never()).sendDigitalMail(eq(senderProperties), any(DigitalMailDto.class), eq("serviceAddress"));
	}

	/**
	 * Test scenario where the recipient has a valid mailbox and the invoice is sent successfully.
	 */
	@Test
	void sendDigitalInvoice_1() {
		final var municipalityId = "2281";
		final var legalId = "somePersonalNumber";
		final var invoiceDto = generateInvoiceDto();
		when(mockPartyIntegration.getLegalId(municipalityId, invoiceDto.getPartyId())).thenReturn(legalId);
		when(mockKivraIntegration.verifyValidRecipient(legalId)).thenReturn(true);
		when(mockKivraIntegration.sendInvoice(invoiceDto)).thenReturn(true);

		final var response = service.sendDigitalInvoice(invoiceDto, municipalityId);

		assertThat(response).isNotNull();

		verify(mockPartyIntegration).getLegalId(municipalityId, invoiceDto.getPartyId());
		verify(mockKivraIntegration).verifyValidRecipient(legalId);
		verify(mockKivraIntegration).sendInvoice(invoiceDto);
		verifyNoMoreInteractions(mockKivraIntegration, mockPartyIntegration);
	}

	/**
	 * Test scenario where the recipient does not have mailbox and the invoice is never sent.
	 */
	@Test
	void sendDigitalInvoice_2() {
		final var municipalityId = "2281";
		final var legalId = "somePersonalNumber";
		final var invoiceDto = generateInvoiceDto();
		when(mockPartyIntegration.getLegalId(municipalityId, invoiceDto.getPartyId())).thenReturn(legalId);
		when(mockKivraIntegration.verifyValidRecipient(legalId)).thenReturn(false);

		final var response = service.sendDigitalInvoice(invoiceDto, municipalityId);

		assertThat(response).isNotNull();

		verify(mockPartyIntegration).getLegalId(municipalityId, invoiceDto.getPartyId());
		verify(mockKivraIntegration).verifyValidRecipient(legalId);
		verifyNoMoreInteractions(mockKivraIntegration, mockPartyIntegration);
	}

	@Test
	void sendDigitalInvoice_partyThrowsProblem() {
		final var invoiceDto = generateInvoiceDto();
		when(mockPartyIntegration.getLegalId(any(String.class), any(String.class)))
			.thenThrow(Problem.builder().withStatus(INTERNAL_SERVER_ERROR).build());

		assertThatExceptionOfType(ThrowableProblem.class).isThrownBy(() -> service.sendDigitalInvoice(invoiceDto, ""));

		verify(mockPartyIntegration).getLegalId(any(String.class), any(String.class));
		verify(mockKivraIntegration, never()).sendInvoice(any(InvoiceDto.class));
	}

	@Test
	void sendDigitalInvoice_kivraIntegrationThrowsProblem() {
		final var invoiceDto = generateInvoiceDto();
		when(mockPartyIntegration.getLegalId(any(String.class), any(String.class))).thenReturn("somePersonalNumber");
		when(mockKivraIntegration.verifyValidRecipient("somePersonalNumber")).thenReturn(true);
		when(mockKivraIntegration.sendInvoice(any(InvoiceDto.class))).thenThrow(Problem.builder().withStatus(INTERNAL_SERVER_ERROR).build());

		assertThatExceptionOfType(ThrowableProblem.class).isThrownBy(() -> service.sendDigitalInvoice(invoiceDto, ""));

		verify(mockPartyIntegration).getLegalId(any(String.class), any(String.class));
		verify(mockKivraIntegration).verifyValidRecipient("somePersonalNumber");
		verify(mockKivraIntegration).sendInvoice(any(InvoiceDto.class));
	}

	@Test
	void testVerifyRecipientHasSomeAvailableMailbox() {
		var legalId = "legalId";
		var partyId = "somePartyId";
		var municipalityId = "2281";

		when(mockProperties.senders()).thenReturn(List.of(mockSenderProperties));
		when(mockSenderProperties.name()).thenReturn(DEFAULT_SENDER_NAME);
		when(mockPartyIntegration.getLegalId(municipalityId, partyId)).thenReturn(legalId);
		when(mockReachableIntegration.isReachable(mockSenderProperties, legalId))
			.thenReturn(List.of(new MailboxDto("recipientId", "serviceAddress", "kivra")));

		final var result = service.verifyRecipientHasSomeAvailableMailbox(partyId, municipalityId);
		assertThat(result).isTrue();

		verify(mockPartyIntegration).getLegalId(anyString(), anyString());
		verify(mockReachableIntegration).isReachable(mockSenderProperties, legalId);
	}

	@Test
	void testVerifyRecipientHasSomeAvailableMailbox_whenNoMailboxesExist() {
		var legalId = "legalId";
		var partyId = "somePartyId";
		var municipalityId = "2281";

		when(mockProperties.senders()).thenReturn(List.of(mockSenderProperties));
		when(mockSenderProperties.name()).thenReturn(DEFAULT_SENDER_NAME);
		when(mockPartyIntegration.getLegalId(municipalityId, partyId)).thenReturn(legalId);
		when(mockReachableIntegration.isReachable(mockSenderProperties, legalId))
			.thenThrow(Problem.valueOf(Status.NOT_FOUND));

		final var result = service.verifyRecipientHasSomeAvailableMailbox(partyId, municipalityId);
		assertThat(result).isFalse();

		verify(mockPartyIntegration).getLegalId(municipalityId, partyId);
		verify(mockReachableIntegration).isReachable(mockSenderProperties, legalId);
	}

}
