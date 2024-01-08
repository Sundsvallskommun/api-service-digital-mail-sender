package se.sundsvall.digitalmail.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
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
import se.sundsvall.digitalmail.integration.citizen.CitizenClient;
import se.sundsvall.digitalmail.integration.kivra.InvoiceDto;
import se.sundsvall.digitalmail.integration.kivra.KivraIntegration;
import se.sundsvall.digitalmail.integration.skatteverket.DigitalMailDto;
import se.sundsvall.digitalmail.integration.skatteverket.MailboxDto;
import se.sundsvall.digitalmail.integration.skatteverket.sendmail.DigitalMailIntegration;

@ExtendWith(MockitoExtension.class)
class DigitalMailServiceTest {
    
    @Mock
    private CitizenClient mockCitizenClient;
    
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
        
        when(mockCitizenClient.getPersonNumber(anyString())).thenReturn("personalNumber");
        when(mockAvailabilityService.getRecipientMailboxesAndCheckAvailability(anyList())).thenReturn(List.of(mailbox));
        when(mockDigitalMailIntegration.sendDigitalMail(any(DigitalMailDto.class), eq("serviceAddress"))).thenReturn(new DigitalMailResponse());

        final var digitalMailResponse = service.sendDigitalMail(new DigitalMailDto(request));
        
        assertThat(digitalMailResponse).isNotNull();
        verify(mockCitizenClient, times(1)).getPersonNumber(anyString());
        verify(mockAvailabilityService, times(1)).getRecipientMailboxesAndCheckAvailability(anyList());
        verify(mockDigitalMailIntegration, times(1)).sendDigitalMail(any(DigitalMailDto.class), eq("serviceAddress"));
    }
    
    //Same thing will happen if any integration throws an exception so will only test one.
    @Test
    void testSendDigitalMail_citizenMappingThrowsException_shouildThrowProblem() {
        final var request = generateDigitalMailRequestDto();

        when(mockCitizenClient.getPersonNumber(anyString())).thenThrow(Problem.builder().withStatus(INTERNAL_SERVER_ERROR).build());
    
        assertThatExceptionOfType(ThrowableProblem.class).isThrownBy(() -> service.sendDigitalMail(request));
        
        verify(mockCitizenClient, times(1)).getPersonNumber(anyString());
        verify(mockAvailabilityService, times(0)).getRecipientMailboxesAndCheckAvailability(anyList());
        verify(mockDigitalMailIntegration, times(0)).sendDigitalMail(any(DigitalMailDto.class), eq("serviceAddress"));
    }

    @Test
    void sendDigitalInvoice() {
        when(mockCitizenClient.getPersonNumber(any(String.class))).thenReturn("somePersonalNumber");
        when(mockKivraIntegration.sendInvoice(any(InvoiceDto.class))).thenReturn(true);

        var response = service.sendDigitalInvoice(generateInvoiceDto());

        assertThat(response).isNotNull();

        verify(mockCitizenClient, times(1)).getPersonNumber(any(String.class));
        verify(mockKivraIntegration, times(1)).sendInvoice(any(InvoiceDto.class));
    }

    @Test
    void sendDigitalInvoice_citizenMappingThrowsProblem() {
        when(mockCitizenClient.getPersonNumber(any(String.class)))
            .thenThrow(Problem.builder().withStatus(INTERNAL_SERVER_ERROR).build());

        assertThatExceptionOfType(ThrowableProblem.class).isThrownBy(() -> service.sendDigitalInvoice(generateInvoiceDto()));

        verify(mockCitizenClient, times(1)).getPersonNumber(any(String.class));
        verify(mockKivraIntegration, never()).sendInvoice(any(InvoiceDto.class));
    }

    @Test
    void sendDigitalInvoice_kivraIntegrationThrowsProblem() {
        when(mockCitizenClient.getPersonNumber(any(String.class))).thenReturn("somePersonalNumber");
        when(mockKivraIntegration.sendInvoice(any(InvoiceDto.class))).thenThrow(Problem.builder().withStatus(INTERNAL_SERVER_ERROR).build());

        assertThatExceptionOfType(ThrowableProblem.class).isThrownBy(() -> service.sendDigitalInvoice(generateInvoiceDto()));

        verify(mockCitizenClient, times(1)).getPersonNumber(any(String.class));
        verify(mockKivraIntegration, times(1)).sendInvoice(any(InvoiceDto.class));
    }

    @Test
    void testVerifyRecipientHasSomeAvailableMailbox() {
        when(mockCitizenClient.getPersonNumber(anyString())).thenReturn("somePersonalNumber");
        when(mockAvailabilityService.getRecipientMailboxesAndCheckAvailability(anyList()))
            .thenReturn(List.of(new MailboxDto("recipientId", "serviceAddress", "kivra")));

        var result = service.verifyRecipientHasSomeAvailableMailbox("somePartyId");
        assertThat(result).isTrue();

        verify(mockCitizenClient, times(1)).getPersonNumber(anyString());
        verify(mockAvailabilityService, times(1)).getRecipientMailboxesAndCheckAvailability(anyList());
    }

    @Test
    void testVerifyRecipientHasSomeAvailableMailbox_whenNoMailboxesExist() {
        when(mockCitizenClient.getPersonNumber(anyString())).thenReturn("somePersonalNumber");
        when(mockAvailabilityService.getRecipientMailboxesAndCheckAvailability(anyList()))
            .thenThrow(Problem.valueOf(Status.NOT_FOUND));

        var result = service.verifyRecipientHasSomeAvailableMailbox("somePartyId");
        assertThat(result).isFalse();

        verify(mockCitizenClient, times(1)).getPersonNumber(anyString());
        verify(mockAvailabilityService, times(1)).getRecipientMailboxesAndCheckAvailability(anyList());
    }
}