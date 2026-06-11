package se.sundsvall.digitalmail.integration.skatteverket.sendmail;

import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.w3c.dom.Node;
import se.gov.minameddelanden.schema.service.DeliveryResult;
import se.gov.minameddelanden.schema.service.DeliveryStatus;
import se.gov.minameddelanden.schema.service.v3.DeliverSecureResponse;
import se.sundsvall.dept44.problem.ThrowableProblem;
import se.sundsvall.digitalmail.SkatteverketTestKeystore;
import se.sundsvall.digitalmail.TestObjectFactory;
import se.sundsvall.digitalmail.api.model.BodyInformation;
import se.sundsvall.digitalmail.api.model.File;
import se.sundsvall.digitalmail.integration.skatteverket.SkatteverketProperties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.MediaType.TEXT_HTML_VALUE;
import static org.springframework.http.MediaType.TEXT_PLAIN_VALUE;
import static se.sundsvall.digitalmail.TestObjectFactory.ORGANIZATION_NUMBER;
import static se.sundsvall.digitalmail.TestObjectFactory.PREFIXED_ORGANIZATION_NUMBER;

@ExtendWith(MockitoExtension.class)
class DigitalMailMapperTest {

	@Mock
	private SkatteverketProperties mockProperties;

	private DigitalMailMapper mapper;

	@BeforeEach
	void setUp() throws Exception {
		mapper = new DigitalMailMapper(mockProperties, SkatteverketTestKeystore.signingCertificate());
	}

	@Test
	void testCreateDeliverSecure() {
		when(mockProperties.supportedSenders()).thenReturn(Map.of(ORGANIZATION_NUMBER, "Sundsvalls kommun"));

		final var sealedDelivery = mapper.createDeliverSecure(TestObjectFactory.generateDigitalMailRequestDto()).getDeliverSecure();
		final var seal = sealedDelivery.getSeal();
		final var signedDelivery = sealedDelivery.getSignedDelivery();

		assertThat(seal.getReceivedTime()).isNotNull();
		assertThat(seal.isSignaturesOK()).isTrue();

		assertThat(signedDelivery.getDelivery().getHeader().getCorrelationId()).isNull();
		assertThat(signedDelivery.getDelivery().getHeader().getRecipient()).isEqualTo("recipientId");
		assertThat(signedDelivery.getDelivery().getHeader().getSender().getName()).isEqualTo("Sundsvalls kommun");
		assertThat(signedDelivery.getDelivery().getHeader().getSender().getId()).isEqualTo(PREFIXED_ORGANIZATION_NUMBER);

		final var header = signedDelivery.getDelivery().getMessages().getFirst().getHeader();

		assertThat(header.getSubject()).isEqualTo("Some subject");
		assertThat(header.getLanguage()).isEqualTo("svSE");
		assertThat(header.getSupportinfo().getPhoneNumber()).isEqualTo("0701740605");
		assertThat(header.getSupportinfo().getEmailAdress()).isEqualTo("email@somewhere.com");
		assertThat(header.getSupportinfo().getURL()).isEqualTo("http://url.com");
		assertThat(header.getSupportinfo().getText()).isEqualTo("support text");

		final var body = signedDelivery.getDelivery().getMessages().getFirst().getBody();
		assertThat(body.getBody()).isNotNull();
		assertThat(body.getContentType()).isEqualTo("text/plain");
	}

	@Test
	void createSealedDeliveryWhenMarshallingFailsThrowsProblem() throws Exception {
		when(mockProperties.supportedSenders()).thenReturn(Map.of(ORGANIZATION_NUMBER, "Sundsvalls kommun"));

		final var throwingMarshaller = mock(Marshaller.class);
		doThrow(new JAXBException("Marshalling failed")).when(throwingMarshaller).marshal(any(), any(Node.class));

		final var spyMapper = spy(mapper);
		doReturn(throwingMarshaller).when(spyMapper).getMarshaller();

		assertThatExceptionOfType(ThrowableProblem.class)
			.isThrownBy(() -> spyMapper.createSealedDelivery(TestObjectFactory.generateDigitalMailRequestDto()))
			.satisfies(thrownProblem -> assertThat(thrownProblem.getStatus()).isEqualTo(INTERNAL_SERVER_ERROR));
	}

	@Test
	void testMd5Sum() {
		final var testString = "Some test string";
		final var expected = "C41E6CD1FEC10F345B366AA2839F6EF4";

		final var actual = mapper.createMd5Checksum(testString.getBytes(StandardCharsets.UTF_8));

		assertThat(actual).isEqualTo(expected);
	}

	@Test
	void testCreateBodyBytes_forTextPlain() {
		final var bodyContent = "Some body";
		final var bodyBytes = mapper.createBody(BodyInformation.builder()
			.withBody(bodyContent)
			.withContentType(TEXT_PLAIN_VALUE)
			.build());

		assertThat(new String(bodyBytes)).isEqualTo(bodyContent);
	}

	@Test
	void testCreateBodyBytes_forTextHtml() {
		final var bodyContent = "<html>stuff</html>";
		// Sent in data is base64-encoded
		final var encoded = Base64.getEncoder().encode(bodyContent.getBytes(StandardCharsets.UTF_8));

		final var bodyBytes = mapper.createBody(BodyInformation.builder()
			.withBody(new String(encoded))
			.withContentType(TEXT_HTML_VALUE)
			.build());

		// Check that the text we sent in is the same as the bytes generated.
		assertThat(new String(bodyBytes)).isEqualTo(bodyContent);
	}

	@Test
	void testCreateAttachments() {
		final var attachment = new File();
		attachment.setBody("Ym9keQ==");
		attachment.setFilename("filename.pdf");
		attachment.setContentType(MediaType.APPLICATION_PDF_VALUE);

		final var attachment2 = new File();
		attachment2.setBody("Ym9keTI=");
		attachment2.setFilename("filename2.pdf");
		attachment2.setContentType(MediaType.APPLICATION_PDF_VALUE);

		final var attachments = mapper.createAttachments(List.of(attachment, attachment2));

		assertThat(new String(attachments.getFirst().getBody(), StandardCharsets.UTF_8)).isEqualTo("body");
		assertThat(attachments.get(0).getContentType()).isEqualTo(MediaType.APPLICATION_PDF_VALUE);
		assertThat(attachments.get(0).getFilename()).isEqualTo("filename.pdf");

		assertThat(new String(attachments.get(1).getBody(), StandardCharsets.UTF_8)).isEqualTo("body2");
		assertThat(attachments.get(1).getContentType()).isEqualTo(MediaType.APPLICATION_PDF_VALUE);
		assertThat(attachments.get(1).getFilename()).isEqualTo("filename2.pdf");
	}

	@Test
	void testCreateDigitalMailResponse() {
		final var deliveryResult = new DeliveryResult();
		deliveryResult.setTransId("abc123");

		final var deliveryStatus = new DeliveryStatus();
		deliveryStatus.setDelivered(true);
		deliveryResult.getStatuses().add(deliveryStatus);

		final var deliverSecureResponse = new DeliverSecureResponse();
		deliverSecureResponse.setReturn(deliveryResult);

		final var response = mapper.createDigitalMailResponse(deliverSecureResponse, "partyId");

		assertThat(response.getDeliveryStatus().getPartyId()).isEqualTo("partyId");
		assertThat(response.getDeliveryStatus().getTransactionId()).isEqualTo("abc123");
	}

	@Test
	void testEmptyMessageBodyInformation_shouldGenerateEmptyBody() {
		final var digitalMailRequestDto = TestObjectFactory.generateDigitalMailRequestDto();
		digitalMailRequestDto.setBodyInformation(null);

		final var messageBody = mapper.createMessageBody(digitalMailRequestDto);

		assertThat(messageBody.getBody()).isEqualTo(new byte[0]);
		assertThat(messageBody.getContentType()).isEqualTo(TEXT_PLAIN_VALUE);
	}

	@Test
	void testEmptyBody_shouldGenerateEmptyBody() {
		final var digitalMailRequestDto = TestObjectFactory.generateDigitalMailRequestDto();
		digitalMailRequestDto.setBodyInformation(BodyInformation.builder()
			.withBody("")
			.build());

		final var messageBody = mapper.createMessageBody(digitalMailRequestDto);

		assertThat(messageBody.getBody()).isEqualTo(new byte[0]);
		assertThat(messageBody.getContentType()).isEqualTo(TEXT_PLAIN_VALUE);
	}
}
