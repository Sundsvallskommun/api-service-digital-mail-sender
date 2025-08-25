package se.sundsvall.digitalmail.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.groups.Tuple.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.zalando.problem.Status.INTERNAL_SERVER_ERROR;
import static org.zalando.problem.Status.NOT_FOUND;
import static se.sundsvall.digitalmail.TestObjectFactory.MUNICIPALITY_ID;
import static se.sundsvall.digitalmail.TestObjectFactory.ORGANIZATION_NUMBER;
import static se.sundsvall.digitalmail.TestObjectFactory.generateDigitalMailRequestDto;
import static se.sundsvall.digitalmail.TestObjectFactory.generateInvoiceDto;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.zalando.problem.Problem;
import org.zalando.problem.ThrowableProblem;
import se.sundsvall.digitalmail.api.model.DigitalMailResponse;
import se.sundsvall.digitalmail.api.model.Mailbox;
import se.sundsvall.digitalmail.integration.kivra.InvoiceDto;
import se.sundsvall.digitalmail.integration.kivra.KivraIntegration;
import se.sundsvall.digitalmail.integration.party.PartyIntegration;
import se.sundsvall.digitalmail.integration.skatteverket.DigitalMailDto;
import se.sundsvall.digitalmail.integration.skatteverket.MailboxDto;
import se.sundsvall.digitalmail.integration.skatteverket.sendmail.DigitalMailIntegration;

@ExtendWith(MockitoExtension.class)
class DigitalMailServiceTest {

	@Mock
	private PartyIntegration mockPartyIntegration;

	@Mock
	private DigitalMailIntegration mockDigitalMailIntegration;

	@Mock
	private KivraIntegration mockKivraIntegration;

	@Mock
	private AvailabilityService mockAvailabilityService;

	@InjectMocks
	private DigitalMailService service;

	@AfterEach
	void afterEach() {
		verifyNoMoreInteractions(mockPartyIntegration, mockDigitalMailIntegration, mockKivraIntegration, mockAvailabilityService);
	}

	@Test
	void testSendDigitalMail_shouldReturnResponse() {
		final var request = generateDigitalMailRequestDto();
		final var mailbox = new MailboxDto("recipientId", "serviceAddress", "kivra", true);

		when(mockPartyIntegration.getLegalId(anyString(), anyString())).thenReturn(Optional.of("personalNumber"));
		when(mockAvailabilityService.getRecipientMailboxesAndCheckAvailability(anyList(), eq(ORGANIZATION_NUMBER))).thenReturn(List.of(mailbox));
		when(mockDigitalMailIntegration.sendDigitalMail(any(DigitalMailDto.class), eq("serviceAddress"))).thenReturn(new DigitalMailResponse());

		final var pdfLength = request.getAttachments().getFirst().getBody().length();  // Save the length of the pdf before compression

		final var digitalMailResponse = service.sendDigitalMail(new DigitalMailDto(request), MUNICIPALITY_ID, ORGANIZATION_NUMBER);

		final var compressedPdfLength = request.getAttachments().getFirst().getBody().length(); // Save the length of the pdf after compression

		assertThat(compressedPdfLength).isLessThan(pdfLength);  // Check that the pdf has been compressed
		assertThat(digitalMailResponse).isNotNull();
		verify(mockPartyIntegration).getLegalId(anyString(), anyString());
		verify(mockAvailabilityService).getRecipientMailboxesAndCheckAvailability(anyList(), eq(ORGANIZATION_NUMBER));
		verify(mockDigitalMailIntegration).sendDigitalMail(any(DigitalMailDto.class), eq("serviceAddress"));
	}

	// Same thing will happen if any integration throws an exception so will only test one.
	@Test
	void testSendDigitalMailNoPersonalNumberFromPartyShouldThrowProblem() {
		final var request = generateDigitalMailRequestDto();

		when(mockPartyIntegration.getLegalId(anyString(), anyString())).thenReturn(Optional.empty());

		assertThatExceptionOfType(ThrowableProblem.class)
			.isThrownBy(() -> service.sendDigitalMail(request, MUNICIPALITY_ID, ORGANIZATION_NUMBER))
			.satisfies(thrownProblem -> {
				assertThat(thrownProblem.getStatus()).isEqualTo(NOT_FOUND);
				assertThat(thrownProblem.getMessage()).isEqualTo("Error while sending digital mail: No personal number found for partyId: " + request.getPartyId());
			});

		verify(mockPartyIntegration).getLegalId(anyString(), anyString());
		verify(mockAvailabilityService, never()).getRecipientMailboxesAndCheckAvailability(anyList(), eq(ORGANIZATION_NUMBER));
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
		when(mockPartyIntegration.getLegalId(municipalityId, invoiceDto.getPartyId())).thenReturn(Optional.of(legalId));
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
		when(mockPartyIntegration.getLegalId(municipalityId, invoiceDto.getPartyId())).thenReturn(Optional.of(legalId));
		when(mockKivraIntegration.verifyValidRecipient(legalId)).thenReturn(false);

		final var response = service.sendDigitalInvoice(invoiceDto, municipalityId);

		assertThat(response).isNotNull();

		verify(mockPartyIntegration).getLegalId(municipalityId, invoiceDto.getPartyId());
		verify(mockKivraIntegration).verifyValidRecipient(legalId);
		verifyNoMoreInteractions(mockKivraIntegration, mockPartyIntegration);
	}

	@Test
	void sendDigitalInvoiceNoPersonalNumberFromPartyShouldThrowProblem() {
		final var invoiceDto = generateInvoiceDto();
		when(mockPartyIntegration.getLegalId(anyString(), anyString()))
			.thenReturn(Optional.empty());

		assertThatExceptionOfType(ThrowableProblem.class)
			.isThrownBy(() -> service.sendDigitalInvoice(invoiceDto, ""))
			.satisfies(thrownProblem -> {
				assertThat(thrownProblem.getStatus()).isEqualTo(NOT_FOUND);
				assertThat(thrownProblem.getMessage()).isEqualTo("Error while sending digital invoice: No personal number found for partyId: " + invoiceDto.getPartyId());
			});

		verify(mockPartyIntegration).getLegalId(anyString(), anyString());
		verify(mockKivraIntegration, never()).sendInvoice(any(InvoiceDto.class));
	}

	@Test
	void sendDigitalInvoice_kivraIntegrationThrowsProblem() {
		final var invoiceDto = generateInvoiceDto();
		when(mockPartyIntegration.getLegalId(anyString(), anyString())).thenReturn(Optional.of("somePersonalNumber"));
		when(mockKivraIntegration.verifyValidRecipient("somePersonalNumber")).thenReturn(true);
		when(mockKivraIntegration.sendInvoice(any(InvoiceDto.class))).thenThrow(Problem.builder().withStatus(INTERNAL_SERVER_ERROR).build());

		assertThatExceptionOfType(ThrowableProblem.class).isThrownBy(() -> service.sendDigitalInvoice(invoiceDto, ""));

		verify(mockPartyIntegration).getLegalId(anyString(), anyString());
		verify(mockKivraIntegration).verifyValidRecipient("somePersonalNumber");
		verify(mockKivraIntegration).sendInvoice(any(InvoiceDto.class));
	}

	@Test
	void getRecipientMailboxes() {
		var reachablePersonalNumber = "personalNumber";
		var unreachablePersonalNumber = "unreachablePersonalNumber";
		var reachableMailboxDto = new MailboxDto("personalNumber", "serviceAddress", "kivra", true);
		var unreachableMailboxDto = new MailboxDto("unreachablePersonalNumber", null, null, false);

		// Test that we find personal numbers for 2 out of 3 partyIds
		when(mockPartyIntegration.getLegalId(anyString(), anyString()))
			.thenReturn(Optional.of(reachablePersonalNumber))
			.thenReturn(Optional.of(unreachablePersonalNumber))
			.thenReturn(Optional.empty());

		// That would result in a list of 2 MailboxDto objects as we only check availability for personal numbers that were
		// found.
		// Return one reachable and one unreachable mailbox.
		when(mockAvailabilityService.getRecipientMailboxesAndCheckAvailability(anyList(), eq(ORGANIZATION_NUMBER)))
			.thenReturn(List.of(reachableMailboxDto, unreachableMailboxDto));

		final var mailboxes = service.getMailboxes(List.of("partyId1", "partyId2", "partyIdNotFound"), MUNICIPALITY_ID, ORGANIZATION_NUMBER);

		assertThat(mailboxes).extracting(Mailbox::getPartyId, Mailbox::getSupplier, Mailbox::isReachable)
			.containsExactlyInAnyOrder(
				tuple("partyId1", "kivra", true),
				tuple("partyId2", null, false),
				tuple("partyIdNotFound", null, false));

		verify(mockPartyIntegration, times(3)).getLegalId(eq(MUNICIPALITY_ID), anyString());
		verify(mockAvailabilityService).getRecipientMailboxesAndCheckAvailability(anyList(), eq(ORGANIZATION_NUMBER));
		verifyNoInteractions(mockKivraIntegration, mockDigitalMailIntegration);
	}
}
