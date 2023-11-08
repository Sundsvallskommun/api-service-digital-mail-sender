package se.sundsvall.digitalmail.integration.skatteverket.sendmail;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ws.client.core.WebServiceTemplate;
import org.zalando.problem.ThrowableProblem;

import se.sundsvall.digitalmail.api.model.DigitalMailRequest;
import se.sundsvall.digitalmail.api.model.DigitalMailResponse;
import se.sundsvall.digitalmail.integration.skatteverket.DigitalMailDto;

import se.gov.minameddelanden.schema.service.v3.DeliverSecure;
import se.gov.minameddelanden.schema.service.v3.DeliverSecureResponse;

@ExtendWith(MockitoExtension.class)
class SecureMailIntegrationTest {
    
    @Mock
    private WebServiceTemplate mockWebServiceTemplate;
    @Mock
    private DigitalMailMapper mockMapper;
    @InjectMocks
    private DigitalMailIntegration mailIntegration;

    @Test
    void testSuccessfulSentMail_shouldReturnDeliveryResult() {
        when(mockMapper.createDeliverSecure(any(DigitalMailDto.class))).thenReturn(new DeliverSecure());
        when(mockWebServiceTemplate.marshalSendAndReceive(eq("http://nowhere.com"), any(DeliverSecure.class))).thenReturn(new DeliverSecureResponse());
        when(mockMapper.createDigitalMailResponse(any(DeliverSecureResponse.class), any(String.class)))
            .thenReturn(new DigitalMailResponse());

        var digitalMailDto = new DigitalMailDto(DigitalMailRequest.builder().build());
        digitalMailDto.setPartyId("somePartyId");

        final var deliverSecureResponse = mailIntegration.sendDigitalMail(digitalMailDto, "http://nowhere.com");
        assertThat(deliverSecureResponse).isNotNull();
        
        verify(mockWebServiceTemplate, times(1)).marshalSendAndReceive(eq("http://nowhere.com"), any(DeliverSecure.class));
    }
    
    @Test
    void testExceptionFromIntegration_shouldThrowProblem() {
        when(mockMapper.createDeliverSecure(any(DigitalMailDto.class))).thenCallRealMethod();
        when(mockWebServiceTemplate.marshalSendAndReceive(eq("http://nowhere.com"), any(DeliverSecure.class))).thenThrow(new RuntimeException("error-message"));
    
        assertThatExceptionOfType(ThrowableProblem.class)
            .isThrownBy(() -> mailIntegration.sendDigitalMail(new DigitalMailDto(DigitalMailRequest.builder().build()), "http://nowhere.com"))
            .withMessage("Couldn't send secure digital mail: error-message");
    }
    
    @Test
    void testGetProblemCause_fakingXmlParsingError_shouldReturnProblem() {
        final var problemCause = mailIntegration.getProblemCause(null); //Couldn't find a better way to produce similar error...

        assertThat(problemCause.getMessage()).isEqualTo("Couldn't get cause");
    }
}