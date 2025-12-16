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
	void testSendDigitalMailForLegalId_shouldReturnResponse() {
		final var request = generateDigitalMailRequestDto();
		final var mailbox = new MailboxDto(null, "recipientId", "serviceAddress", "kivra", true);

		when(mockPartyIntegration.getLegalId(anyString(), anyString())).thenReturn(Optional.of("legalId"));
		when(mockAvailabilityService.getRecipientMailboxesAndCheckAvailability(anyList(), eq(ORGANIZATION_NUMBER))).thenReturn(List.of(mailbox));
		when(mockDigitalMailIntegration.sendDigitalMail(any(DigitalMailDto.class), eq("serviceAddress"))).thenReturn(new DigitalMailResponse());

		final var pdfLength = request.getAttachments().getFirst().getBody().length();  // Save the length of the pdf before compression

		final var digitalMailResponse = service.sendDigitalMail(request, MUNICIPALITY_ID);

		final var compressedPdfLength = request.getAttachments().getFirst().getBody().length(); // Save the length of the pdf after compression

		assertThat(compressedPdfLength).isLessThan(pdfLength);  // Check that the pdf has been compressed
		assertThat(digitalMailResponse).isNotNull();
		verify(mockPartyIntegration).getLegalId(anyString(), anyString());
		verify(mockAvailabilityService).getRecipientMailboxesAndCheckAvailability(anyList(), eq(ORGANIZATION_NUMBER));
		verify(mockDigitalMailIntegration).sendDigitalMail(any(DigitalMailDto.class), eq("serviceAddress"));
	}

	// Same thing will happen if any integration throws an exception so will only test one.
	@Test
	void testSendDigitalMailNoLegalIdFromPartyShouldThrowProblem() {
		final var request = generateDigitalMailRequestDto();

		when(mockPartyIntegration.getLegalId(anyString(), anyString())).thenReturn(Optional.empty());

		assertThatExceptionOfType(ThrowableProblem.class)
			.isThrownBy(() -> service.sendDigitalMail(request, MUNICIPALITY_ID))
			.satisfies(thrownProblem -> {
				assertThat(thrownProblem.getStatus()).isEqualTo(NOT_FOUND);
				assertThat(thrownProblem.getMessage()).isEqualTo("Error while sending digital mail: No legal Id found for partyId: " + request.getPartyId());
			});

		verify(mockPartyIntegration).getLegalId(anyString(), anyString());
		verify(mockAvailabilityService, never()).getRecipientMailboxesAndCheckAvailability(anyList(), eq(ORGANIZATION_NUMBER));
		verify(mockDigitalMailIntegration, never()).sendDigitalMail(any(DigitalMailDto.class), eq("serviceAddress"));
	}

	@Test
	void testSendDigitalMailNoValidMailboxShouldthrowProblem() {
		final var request = generateDigitalMailRequestDto();

		when(mockPartyIntegration.getLegalId(anyString(), anyString())).thenReturn(Optional.of("legalId"));
		when(mockAvailabilityService.getRecipientMailboxesAndCheckAvailability(anyList(), eq(ORGANIZATION_NUMBER))).thenReturn(List.of());

		assertThatExceptionOfType(ThrowableProblem.class)
			.isThrownBy(() -> service.sendDigitalMail(request, MUNICIPALITY_ID))
			.satisfies(thrownProblem -> {
				assertThat(thrownProblem.getStatus()).isEqualTo(NOT_FOUND);
				assertThat(thrownProblem.getMessage()).isEqualTo("Couldn't find any mailboxes: No mailbox could be found for any of the given partyIds or the recipients doesn't allow the sender.");
			});

		verify(mockPartyIntegration).getLegalId(anyString(), anyString());
		verify(mockAvailabilityService).getRecipientMailboxesAndCheckAvailability(anyList(), eq(ORGANIZATION_NUMBER));
		verify(mockDigitalMailIntegration, never()).sendDigitalMail(any(DigitalMailDto.class), eq("serviceAddress"));
	}

	/**
	 * Test scenario where the recipient has a valid mailbox and the invoice is sent successfully.
	 */
	@Test
	void sendDigitalInvoice_1() {
		final var municipalityId = "2281";
		final var legalId = "someLegalId";
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
		final var legalId = "somelegalId";
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
	void sendDigitalInvoiceNoLegalIdFromPartyShouldThrowProblem() {
		final var invoiceDto = generateInvoiceDto();
		when(mockPartyIntegration.getLegalId(anyString(), anyString()))
			.thenReturn(Optional.empty());

		assertThatExceptionOfType(ThrowableProblem.class)
			.isThrownBy(() -> service.sendDigitalInvoice(invoiceDto, ""))
			.satisfies(thrownProblem -> {
				assertThat(thrownProblem.getStatus()).isEqualTo(NOT_FOUND);
				assertThat(thrownProblem.getMessage()).isEqualTo("Error while sending digital invoice: No legal Id found for partyId: " + invoiceDto.getPartyId());
			});

		verify(mockPartyIntegration).getLegalId(anyString(), anyString());
		verify(mockKivraIntegration, never()).sendInvoice(any(InvoiceDto.class));
	}

	@Test
	void sendDigitalInvoice_kivraIntegrationThrowsProblem() {
		final var invoiceDto = generateInvoiceDto();
		when(mockPartyIntegration.getLegalId(anyString(), anyString())).thenReturn(Optional.of("someLegalId"));
		when(mockKivraIntegration.verifyValidRecipient("someLegalId")).thenReturn(true);
		when(mockKivraIntegration.sendInvoice(any(InvoiceDto.class))).thenThrow(Problem.builder().withStatus(INTERNAL_SERVER_ERROR).build());

		assertThatExceptionOfType(ThrowableProblem.class).isThrownBy(() -> service.sendDigitalInvoice(invoiceDto, ""));

		verify(mockPartyIntegration).getLegalId(anyString(), anyString());
		verify(mockKivraIntegration).verifyValidRecipient("someLegalId");
		verify(mockKivraIntegration).sendInvoice(any(InvoiceDto.class));
	}

	@Test
	void testGetMailboxes() {
		final var reachableLegalId = "LegalId";
		final var unreachableLegalId = "unreachableLegalId";
		final var reachableMailboxDto = new MailboxDto(null, "LegalId", "serviceAddress", "kivra", true);
		final var unreachableMailboxDto = new MailboxDto("Sender not accepted by recipient", "unreachableLegalId", null, null, false);

		// Test that we find legal Ids for 2 out of 3 partyIds
		when(mockPartyIntegration.getLegalId(anyString(), anyString()))
			.thenReturn(Optional.of(reachableLegalId))
			.thenReturn(Optional.of(unreachableLegalId))
			.thenReturn(Optional.empty());

		// That would result in a list of 2 MailboxDto objects as we only check availability for legal Ids that were
		// found.
		// Return one reachable and one unreachable mailbox.
		when(mockAvailabilityService.getRecipientMailboxesAndCheckAvailability(anyList(), eq(ORGANIZATION_NUMBER)))
			.thenReturn(List.of(reachableMailboxDto, unreachableMailboxDto));

		final var mailboxes = service.getMailboxes(List.of("partyId1", "partyId2", "partyIdNotFound"), MUNICIPALITY_ID, ORGANIZATION_NUMBER);

		assertThat(mailboxes).extracting(Mailbox::getPartyId, Mailbox::getSupplier, Mailbox::isReachable, Mailbox::getReason)
			.containsExactlyInAnyOrder(
				tuple("partyId1", "kivra", true, null),
				tuple("partyId2", null, false, "Sender not accepted by recipient"),
				tuple("partyIdNotFound", null, false, "No legal Id found for partyId: partyIdNotFound"));

		verify(mockPartyIntegration, times(3)).getLegalId(eq(MUNICIPALITY_ID), anyString());
		verify(mockAvailabilityService).getRecipientMailboxesAndCheckAvailability(anyList(), eq(ORGANIZATION_NUMBER));
		verifyNoInteractions(mockKivraIntegration, mockDigitalMailIntegration);
	}

	@Test
	void testGetMailboxesWhenNoLegalIdFound() {
		// Test that we find no legal Id for any of the partyIds
		when(mockPartyIntegration.getLegalId(anyString(), anyString()))
			.thenReturn(Optional.empty())
			.thenReturn(Optional.empty());

		final var mailboxes = service.getMailboxes(List.of("partyId1", "partyId2"), MUNICIPALITY_ID, ORGANIZATION_NUMBER);

		assertThat(mailboxes).extracting(Mailbox::getPartyId, Mailbox::getSupplier, Mailbox::isReachable, Mailbox::getReason)
			.containsExactlyInAnyOrder(
				tuple("partyId1", null, false, "No legal Id found for partyId: partyId1"),
				tuple("partyId2", null, false, "No legal Id found for partyId: partyId2"));

		verify(mockPartyIntegration, times(2)).getLegalId(eq(MUNICIPALITY_ID), anyString());
		verifyNoInteractions(mockAvailabilityService, mockKivraIntegration, mockDigitalMailIntegration);
	}

	@Test
	void getGetMailboxesWhenEmptyResponseFromAvailabilityService() {
		when(mockPartyIntegration.getLegalId(anyString(), anyString()))
			.thenReturn(Optional.of("LegalId"))
			.thenReturn(Optional.of("anotherLegalId"));
		when(mockAvailabilityService.getRecipientMailboxesAndCheckAvailability(anyList(), eq(ORGANIZATION_NUMBER))).thenReturn(List.of());

		final var mailboxes = service.getMailboxes(List.of("partyId1", "partyId2"), MUNICIPALITY_ID, ORGANIZATION_NUMBER);

		assertThat(mailboxes).extracting(Mailbox::getPartyId, Mailbox::getSupplier, Mailbox::isReachable)
			.containsExactly(
				tuple("partyId1", null, false), tuple("partyId2", null, false));

		verify(mockPartyIntegration, times(2)).getLegalId(eq(MUNICIPALITY_ID), anyString());
		verify(mockAvailabilityService).getRecipientMailboxesAndCheckAvailability(anyList(), eq(ORGANIZATION_NUMBER));
		verifyNoInteractions(mockKivraIntegration, mockDigitalMailIntegration);
	}
}
