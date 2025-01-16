package se.sundsvall.digitalmail.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.zalando.problem.Status.INTERNAL_SERVER_ERROR;
import static se.sundsvall.digitalmail.TestObjectFactory.generateDigitalMailRequestDto;
import static se.sundsvall.digitalmail.TestObjectFactory.generateInvoiceDto;

import java.util.List;
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
import se.sundsvall.digitalmail.integration.party.PartyClient;
import se.sundsvall.digitalmail.integration.skatteverket.DigitalMailDto;
import se.sundsvall.digitalmail.integration.skatteverket.MailboxDto;
import se.sundsvall.digitalmail.integration.skatteverket.sendmail.DigitalMailIntegration;

@ExtendWith(MockitoExtension.class)
class DigitalMailServiceTest {

	@Mock
	private PartyClient mockPartyClient;

	@Mock
	private DigitalMailIntegration mockDigitalMailIntegration;

	@Mock
	private KivraIntegration mockKivraIntegration;

	@Mock
	private AvailabilityService mockAvailabilityService;

	@InjectMocks
	private DigitalMailService service;

	@Test
	void testSendDigitalMail_shouldReturnResponse() {
		final var request = generateDigitalMailRequestDto();
		final var mailbox = new MailboxDto("recipientId", "serviceAddress", "kivra");

		when(mockPartyClient.getLegalId(anyString(), anyString())).thenReturn("personalNumber");
		when(mockAvailabilityService.getRecipientMailboxesAndCheckAvailability(anyList())).thenReturn(List.of(mailbox));
		when(mockDigitalMailIntegration.sendDigitalMail(any(DigitalMailDto.class), eq("serviceAddress"))).thenReturn(new DigitalMailResponse());

		final var pdfLength = request.getAttachments().getFirst().getBody().length();  // Save the length of the pdf before compression

		final var digitalMailResponse = service.sendDigitalMail(new DigitalMailDto(request), "");

		final var compressedPdfLength = request.getAttachments().getFirst().getBody().length(); // Save the length of the pdf after compression

		assertThat(compressedPdfLength).isLessThan(pdfLength);  // Check that the pdf has been compressed
		assertThat(digitalMailResponse).isNotNull();
		verify(mockPartyClient).getLegalId(anyString(), anyString());
		verify(mockAvailabilityService).getRecipientMailboxesAndCheckAvailability(anyList());
		verify(mockDigitalMailIntegration).sendDigitalMail(any(DigitalMailDto.class), eq("serviceAddress"));
	}

	// Same thing will happen if any integration throws an exception so will only test one.
	@Test
	void testSendDigitalMail_partyThrowsExceptionShouldThrowProblem() {
		final var request = generateDigitalMailRequestDto();

		when(mockPartyClient.getLegalId(anyString(), anyString())).thenThrow(Problem.builder().withStatus(INTERNAL_SERVER_ERROR).build());

		assertThatExceptionOfType(ThrowableProblem.class).isThrownBy(() -> service.sendDigitalMail(request, ""));

		verify(mockPartyClient).getLegalId(anyString(), anyString());
		verify(mockAvailabilityService, never()).getRecipientMailboxesAndCheckAvailability(anyList());
		verify(mockDigitalMailIntegration, never()).sendDigitalMail(any(DigitalMailDto.class), eq("serviceAddress"));
	}

	/**
	 * Test scenario where the recipient has a valid mailbox and the invoice is sent successfully.
	 */
	@Test
	void sendDigitalInvoice_1() {
		final var municipalityId = "2281";
		final var legalId = "somePersonalNumber";
		final var invoiceDto = generateInvoiceDto();
		when(mockPartyClient.getLegalId(municipalityId, invoiceDto.getPartyId())).thenReturn(legalId);
		when(mockKivraIntegration.verifyValidRecipient(legalId)).thenReturn(true);
		when(mockKivraIntegration.sendInvoice(invoiceDto)).thenReturn(true);

		final var response = service.sendDigitalInvoice(invoiceDto, municipalityId);

		assertThat(response).isNotNull();

		verify(mockPartyClient).getLegalId(municipalityId, invoiceDto.getPartyId());
		verify(mockKivraIntegration).verifyValidRecipient(legalId);
		verify(mockKivraIntegration).sendInvoice(invoiceDto);
		verifyNoMoreInteractions(mockKivraIntegration, mockPartyClient);
	}

	/**
	 * Test scenario where the recipient does not have mailbox and the invoice is never sent.
	 */
	@Test
	void sendDigitalInvoice_2() {
		final var municipalityId = "2281";
		final var legalId = "somePersonalNumber";
		final var invoiceDto = generateInvoiceDto();
		when(mockPartyClient.getLegalId(municipalityId, invoiceDto.getPartyId())).thenReturn(legalId);
		when(mockKivraIntegration.verifyValidRecipient(legalId)).thenReturn(false);

		final var response = service.sendDigitalInvoice(invoiceDto, municipalityId);

		assertThat(response).isNotNull();

		verify(mockPartyClient).getLegalId(municipalityId, invoiceDto.getPartyId());
		verify(mockKivraIntegration).verifyValidRecipient(legalId);
		verifyNoMoreInteractions(mockKivraIntegration, mockPartyClient);
	}

	@Test
	void sendDigitalInvoice_partyThrowsProblem() {
		final var invoiceDto = generateInvoiceDto();
		when(mockPartyClient.getLegalId(any(String.class), any(String.class)))
			.thenThrow(Problem.builder().withStatus(INTERNAL_SERVER_ERROR).build());

		assertThatExceptionOfType(ThrowableProblem.class).isThrownBy(() -> service.sendDigitalInvoice(invoiceDto, ""));

		verify(mockPartyClient).getLegalId(any(String.class), any(String.class));
		verify(mockKivraIntegration, never()).sendInvoice(any(InvoiceDto.class));
	}

	@Test
	void sendDigitalInvoice_kivraIntegrationThrowsProblem() {
		final var invoiceDto = generateInvoiceDto();
		when(mockPartyClient.getLegalId(any(String.class), any(String.class))).thenReturn("somePersonalNumber");
		when(mockKivraIntegration.verifyValidRecipient("somePersonalNumber")).thenReturn(true);
		when(mockKivraIntegration.sendInvoice(any(InvoiceDto.class))).thenThrow(Problem.builder().withStatus(INTERNAL_SERVER_ERROR).build());

		assertThatExceptionOfType(ThrowableProblem.class).isThrownBy(() -> service.sendDigitalInvoice(invoiceDto, ""));

		verify(mockPartyClient).getLegalId(any(String.class), any(String.class));
		verify(mockKivraIntegration).verifyValidRecipient("somePersonalNumber");
		verify(mockKivraIntegration).sendInvoice(any(InvoiceDto.class));
	}

	@Test
	void testVerifyRecipientHasSomeAvailableMailbox() {
		when(mockPartyClient.getLegalId(anyString(), anyString())).thenReturn("somePersonalNumber");
		when(mockAvailabilityService.getRecipientMailboxesAndCheckAvailability(anyList()))
			.thenReturn(List.of(new MailboxDto("recipientId", "serviceAddress", "kivra")));

		final var result = service.verifyRecipientHasSomeAvailableMailbox("somePartyId", "");
		assertThat(result).isTrue();

		verify(mockPartyClient).getLegalId(anyString(), anyString());
		verify(mockAvailabilityService).getRecipientMailboxesAndCheckAvailability(anyList());
	}

	@Test
	void testVerifyRecipientHasSomeAvailableMailbox_whenNoMailboxesExist() {
		when(mockPartyClient.getLegalId(anyString(), anyString())).thenReturn("somePersonalNumber");
		when(mockAvailabilityService.getRecipientMailboxesAndCheckAvailability(anyList()))
			.thenThrow(Problem.valueOf(Status.NOT_FOUND));

		final var result = service.verifyRecipientHasSomeAvailableMailbox("somePartyId", "");
		assertThat(result).isFalse();

		verify(mockPartyClient).getLegalId(anyString(), anyString());
		verify(mockAvailabilityService).getRecipientMailboxesAndCheckAvailability(anyList());
	}

}
