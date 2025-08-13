package se.sundsvall.digitalmail.integration.minameddelanden.sendmail;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static se.sundsvall.digitalmail.TestObjectFactory.generateSenderProperties;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ws.client.core.WebServiceTemplate;
import org.zalando.problem.ThrowableProblem;
import se.gov.minameddelanden.schema.service.v3.DeliverSecure;
import se.gov.minameddelanden.schema.service.v3.DeliverSecureResponse;
import se.sundsvall.digitalmail.api.model.DigitalMailRequest;
import se.sundsvall.digitalmail.api.model.DigitalMailResponse;
import se.sundsvall.digitalmail.integration.minameddelanden.DigitalMailDto;
import se.sundsvall.digitalmail.integration.minameddelanden.configuration.MinaMeddelandenClientFactory;

@ExtendWith(MockitoExtension.class)
class DigitalMailIntegrationTest {

	@Mock
	private WebServiceTemplate mockWebServiceTemplate;

	@Mock
	private MinaMeddelandenClientFactory mockClientFactory;

	@Mock
	private DigitalMailMapper mockMapper;

	@InjectMocks
	private DigitalMailIntegration mailIntegration;

	@Test
	void testSuccessfulSentMail_shouldReturnDeliveryResult() {
		var senderProperties = generateSenderProperties();

		when(mockClientFactory.getSendMailWebServiceTemplate(senderProperties.name())).thenReturn(mockWebServiceTemplate);
		when(mockMapper.createDeliverSecure(eq(senderProperties), any(DigitalMailDto.class))).thenReturn(new DeliverSecure());
		when(mockWebServiceTemplate.marshalSendAndReceive(eq("https://nowhere.com"), any(DeliverSecure.class))).thenReturn(new DeliverSecureResponse());
		when(mockMapper.createDigitalMailResponse(any(DeliverSecureResponse.class), any(String.class)))
			.thenReturn(new DigitalMailResponse());

		final var digitalMailDto = new DigitalMailDto(DigitalMailRequest.builder().withPartyId("somePartyId").build());

		final var deliverSecureResponse = mailIntegration.sendDigitalMail(senderProperties, digitalMailDto, "https://nowhere.com");
		assertThat(deliverSecureResponse).isNotNull();

		verify(mockWebServiceTemplate, times(1)).marshalSendAndReceive(eq("https://nowhere.com"), any(DeliverSecure.class));
	}

	@Test
	void testExceptionFromIntegration_shouldThrowProblem() {

		final var digitalMailDto = new DigitalMailDto(DigitalMailRequest.builder().build());
		var senderProperties = generateSenderProperties();
		when(mockMapper.createDeliverSecure(eq(senderProperties), any(DigitalMailDto.class))).thenReturn(new DeliverSecure());
		when(mockClientFactory.getSendMailWebServiceTemplate(senderProperties.name())).thenReturn(mockWebServiceTemplate);
		when(mockWebServiceTemplate.marshalSendAndReceive(eq("https://nowhere.com"), any(DeliverSecure.class))).thenThrow(new RuntimeException("error-message"));

		assertThatExceptionOfType(ThrowableProblem.class)
			.isThrownBy(() -> mailIntegration.sendDigitalMail(senderProperties, digitalMailDto, "https://nowhere.com"))
			.withMessage("Couldn't send secure digital mail: error-message");
	}

	@Test
	void testGetProblemCause_fakingXmlParsingError_shouldReturnProblem() {
		final var problemCause = mailIntegration.getProblemCause(null); // Couldn't find a better way to produce similar error...

		assertThat(problemCause.getMessage()).isEqualTo("Couldn't get cause");
	}
}
