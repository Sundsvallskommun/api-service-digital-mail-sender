package se.sundsvall.digitalmail.integration.skatteverket.sendmail;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.MediaType.TEXT_HTML_VALUE;
import static org.springframework.http.MediaType.TEXT_PLAIN_VALUE;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.util.Base64;
import java.util.List;

import javax.xml.crypto.dsig.XMLSignature;
import javax.xml.crypto.dsig.XMLSignatureFactory;
import javax.xml.crypto.dsig.dom.DOMValidateContext;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilderFactory;

import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.zalando.problem.ThrowableProblem;

import se.sundsvall.digitalmail.DigitalMail;
import se.sundsvall.digitalmail.TestObjectFactory;
import se.sundsvall.digitalmail.api.model.BodyInformation;
import se.sundsvall.digitalmail.api.model.File;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBElement;
import se.gov.minameddelanden.common.sign.X509KeySelector;
import se.gov.minameddelanden.schema.message.v3.SealedDelivery;
import se.gov.minameddelanden.schema.message.v3.SignedDelivery;
import se.gov.minameddelanden.schema.service.DeliveryResult;
import se.gov.minameddelanden.schema.service.DeliveryStatus;
import se.gov.minameddelanden.schema.service.v3.DeliverSecure;
import se.gov.minameddelanden.schema.service.v3.DeliverSecureResponse;

@ActiveProfiles("junit")
@SpringBootTest(webEnvironment = RANDOM_PORT, classes = DigitalMail.class)
@ExtendWith(SoftAssertionsExtension.class)
class DigitalMailMapperTest {

	@Autowired
	private DigitalMailMapper mapper;

	@Value("${integration.skatteverket.key-store-as-base64}")
	private String keyStoreAsBase64;

	@Test
	void testCreateDeliverSecure(final SoftAssertions softly) {
		final var sealedDelivery = mapper.createDeliverSecure(TestObjectFactory.generateDigitalMailRequestDto()).getDeliverSecure();
		final var seal = sealedDelivery.getSeal();
		final var signedDelivery = sealedDelivery.getSignedDelivery();

		softly.assertThat(seal.getReceivedTime()).isNotNull();
		softly.assertThat(seal.isSignaturesOK()).isTrue();

		softly.assertThat(signedDelivery.getDelivery().getHeader().getCorrelationId()).isNull();
		softly.assertThat(signedDelivery.getDelivery().getHeader().getRecipient()).isEqualTo("recipientId");
		softly.assertThat(signedDelivery.getDelivery().getHeader().getSender().getName()).isEqualTo("Sundsvalls Kommun");
		softly.assertThat(signedDelivery.getDelivery().getHeader().getSender().getId()).isEqualTo("162120002411");

		final var header = signedDelivery.getDelivery().getMessage().getFirst().getHeader();

		softly.assertThat(header.getSubject()).isEqualTo("Some subject");
		softly.assertThat(header.getLanguage()).isEqualTo("svSE");
		softly.assertThat(header.getSupportinfo().getPhoneNumber()).isEqualTo("0701234567");
		softly.assertThat(header.getSupportinfo().getEmailAdress()).isEqualTo("email@somewhere.com");
		softly.assertThat(header.getSupportinfo().getURL()).isEqualTo("http://url.com");
		softly.assertThat(header.getSupportinfo().getText()).isEqualTo("support text");

		final var body = signedDelivery.getDelivery().getMessage().get(0).getBody();
		softly.assertThat(body.getBody()).isNotNull();
		softly.assertThat(body.getContentType()).isEqualTo("text/plain");
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
	void testGetAliasFromKeystore() throws Exception {
		final var keyStore = getKeyStore();
		final var alias = mapper.getAliasFromKeystore(keyStore, DigitalMailMapper.SKATTEVERKET_CERT_NAME);

		assertThat(alias).isEqualTo("skatteverket");
	}

	@Test
	void testGetAliasFromKeystore_shouldThrowException_whenNotFound() throws Exception {
		final var keyStore = getKeyStore();

		assertThatExceptionOfType(ThrowableProblem.class)
			.isThrownBy(() -> mapper.getAliasFromKeystore(keyStore, "notFound"))
			.withMessage("Couldn't find certificate for notFound");
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

		assertThat(new String(attachments.get(0).getBody(), StandardCharsets.UTF_8)).isEqualTo("body");
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
		deliveryResult.getStatus().add(deliveryStatus);

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

	@Disabled("Implement validation of created message, so it works..")
	@Test
	void testCreateDeliverSecure_andValidateSignature() throws Exception {
		final var dto = TestObjectFactory.generateDigitalMailRequestDto();
		final var deliverSecure = mapper.createDeliverSecure(dto);
		final var sealedDelivery = deliverSecure.getDeliverSecure();

		final var documentFactory = DocumentBuilderFactory.newInstance();
		documentFactory.setNamespaceAware(true);
		final var documentBuilder = documentFactory.newDocumentBuilder();
		final var document = documentBuilder.newDocument();

		final var sealedDeliveryJAXBElement = new JAXBElement<>(new QName("http://minameddelanden.gov.se/schema/Message/v3", "sealedDelivery"), SealedDelivery.class, sealedDelivery);
		final var jaxbContext = JAXBContext.newInstance(SignedDelivery.class, SealedDelivery.class, DeliverSecure.class);
		final var marshaller = jaxbContext.createMarshaller();

		marshaller.marshal(sealedDeliveryJAXBElement, document);

		final var nodeList = document.getElementsByTagNameNS(XMLSignature.XMLNS, "Signature");
		if (nodeList.getLength() == 0) {
			throw new Exception("Cannot find Signature element");
		}

		final var signatureFactory = XMLSignatureFactory.getInstance("DOM");
		final var validateContext = new DOMValidateContext(new X509KeySelector(), nodeList.item(0));
		final var xmlSignature = signatureFactory.unmarshalXMLSignature(validateContext);

		final var iterator = xmlSignature.getSignedInfo().getReferences().iterator();

		for (var j = 0; iterator.hasNext(); j++) {
			var refValid = iterator.next().validate(validateContext);

			System.out.println("ref[" + j + "] validity status: " + refValid);
		}

		final var validated = xmlSignature.validate(validateContext);

		System.out.println(validated);

		assertThat(validated).isTrue();
	}

	private KeyStore getKeyStore() throws Exception {
		KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
		keyStore.load(new ByteArrayInputStream(Base64.getDecoder().decode(keyStoreAsBase64.getBytes())), "changeit".toCharArray());
		return keyStore;
	}
}
