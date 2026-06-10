package se.sundsvall.digitalmail.integration.skatteverket.sendmail;

import jakarta.xml.bind.DatatypeConverter;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jose4j.base64url.Base64;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import se.gov.minameddelanden.common.X509CertificateWithPrivateKey;
import se.gov.minameddelanden.common.Xml;
import se.gov.minameddelanden.schema.message.Attachment;
import se.gov.minameddelanden.schema.message.MessageBody;
import se.gov.minameddelanden.schema.message.Seal;
import se.gov.minameddelanden.schema.message.v2.SecureDeliveryHeader;
import se.gov.minameddelanden.schema.message.v3.MessageHeader;
import se.gov.minameddelanden.schema.message.v3.ObjectFactory;
import se.gov.minameddelanden.schema.message.v3.SealedDelivery;
import se.gov.minameddelanden.schema.message.v3.SecureDelivery;
import se.gov.minameddelanden.schema.message.v3.SecureMessage;
import se.gov.minameddelanden.schema.message.v3.SignedDelivery;
import se.gov.minameddelanden.schema.message.v3.SupportInfo;
import se.gov.minameddelanden.schema.sender.Sender;
import se.gov.minameddelanden.schema.service.v3.DeliverSecure;
import se.gov.minameddelanden.schema.service.v3.DeliverSecureResponse;
import se.sundsvall.dept44.problem.Problem;
import se.sundsvall.dept44.problem.ThrowableProblem;
import se.sundsvall.dept44.requestid.RequestId;
import se.sundsvall.digitalmail.api.model.BodyInformation;
import se.sundsvall.digitalmail.api.model.DeliveryStatus;
import se.sundsvall.digitalmail.api.model.DigitalMailResponse;
import se.sundsvall.digitalmail.api.model.File;
import se.sundsvall.digitalmail.integration.skatteverket.DigitalMailDto;
import se.sundsvall.digitalmail.integration.skatteverket.SkatteverketProperties;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.List;

import static org.apache.commons.collections4.CollectionUtils.isEmpty;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static se.sundsvall.digitalmail.util.LegalIdUtil.prefixOrgNumber;

@Component
@Slf4j
class DigitalMailMapper {

	private static final ObjectFactory OBJECT_FACTORY = new ObjectFactory();
	private static final String NAMESPACE_URI = "http://minameddelanden.gov.se/schema/Message/v3";

	private final SkatteverketProperties properties;

	private final DocumentBuilder documentBuilder;

	private final X509CertificateWithPrivateKey certificate;

	// Since the marshaller is not thread safe, we need to create a new one for each thread.
	private final ThreadLocal<Marshaller> threadLocalMarshaller = ThreadLocal.withInitial(() -> {
		try {
			JAXBContext jaxbContext = JAXBContext.newInstance(SignedDelivery.class, SealedDelivery.class, DeliverSecure.class);
			return jaxbContext.createMarshaller();
		} catch (JAXBException e) {
			throw Problem.builder()
				.withTitle("Failed to create Marshaller")
				.withStatus(INTERNAL_SERVER_ERROR)
				.withCause((ThrowableProblem) e.getCause())
				.build();
		}
	});

	DigitalMailMapper(final SkatteverketProperties properties, final X509CertificateWithPrivateKey certificate) throws ParserConfigurationException {
		this.properties = properties;
		this.certificate = certificate;
		final var documentBuilderFactory = DocumentBuilderFactory.newInstance();
		documentBuilderFactory.setNamespaceAware(true);
		this.documentBuilder = documentBuilderFactory.newDocumentBuilder();
	}

	// Package-private so the marshaling failure path can be exercised in tests via a throwing marshaller.
	Marshaller getMarshaller() {
		return threadLocalMarshaller.get();
	}

	DigitalMailResponse createDigitalMailResponse(final DeliverSecureResponse deliveryResult, final String partyId) {
		final var digitalMailResponse = new DigitalMailResponse();
		digitalMailResponse.setDeliveryStatus(DeliveryStatus.builder()
			.withTransactionId(deliveryResult.getReturn().getTransId())
			.withDelivered(deliveryResult.getReturn().getStatuses().getFirst().isDelivered())   // Will always be only one, for now
			.withPartyId(partyId)
			.build());
		return digitalMailResponse;
	}

	/**
	 * @param  dto to map to a request
	 * @return     A DeliverSecure-object containing the SealedDelivery to be sent to Skatteverket
	 */
	DeliverSecure createDeliverSecure(final DigitalMailDto dto) {
		final var sealedDelivery = createSealedDelivery(dto);
		final var deliverSecure = new DeliverSecure();
		deliverSecure.setDeliverSecure(sealedDelivery);
		return deliverSecure;
	}

	/**
	 * The sealed delivery to be inserted into the SealedDelivery-object
	 *
	 * @param  dto to be translated into a {@link SealedDelivery}
	 * @return     A Sealed delivery signed by not the sender but us as a mediator.
	 */
	SealedDelivery createSealedDelivery(final DigitalMailDto dto) {
		log.info("Creating sealed delivery");
		try {
			// Get the correct certificate
			// Create the signedDeliveryDocument, inner one.
			var signedDelivery = createSignedDelivery(dto);

			final var signedDeliveryElement = new JAXBElement<>(new QName(NAMESPACE_URI, "SignedDelivery"), SignedDelivery.class, signedDelivery);
			final var signedDeliveryDocument = documentBuilder.newDocument();

			getMarshaller().marshal(signedDeliveryElement, signedDeliveryDocument);

			var xml = Xml.fromDOM(signedDeliveryDocument);
			var signedXml = xml.sign(certificate);
			signedDelivery = signedXml.toJaxbObject(SignedDelivery.class);

			final var seal = new Seal();
			seal.setSignaturesOK(true);
			seal.setReceivedTime(createTimestamp());

			var sealedDelivery = OBJECT_FACTORY.createSealedDelivery();
			sealedDelivery.setSeal(seal);
			sealedDelivery.setSignedDelivery(signedDelivery);

			final var sealedDeliveryElement = new JAXBElement<>(new QName(NAMESPACE_URI, "SealedDelivery"), SealedDelivery.class, sealedDelivery);
			final var sealedDeliveryDocument = documentBuilder.newDocument();

			getMarshaller().marshal(sealedDeliveryElement, sealedDeliveryDocument);

			xml = Xml.fromDOM(sealedDeliveryDocument);
			signedXml = xml.sign(certificate);
			sealedDelivery = signedXml.toJaxbObject(SealedDelivery.class);

			return sealedDelivery;
		} catch (final JAXBException | DatatypeConfigurationException e) {
			// Needed to see stacktrace
			log.error("Failed to create sealed delivery", e);
			throw Problem.builder()
				.withTitle("Couldn't create a SealedDelivery object to send")
				.withStatus(INTERNAL_SERVER_ERROR)
				.withCause((ThrowableProblem) e.getCause())
				.build();
		} finally {
			// Always clean up
			threadLocalMarshaller.remove();
		}
	}

	/**
	 * The object to be digitally signed
	 *
	 * @param  dto to be translated into a {@link SignedDelivery}
	 * @return     A Signed delivery should be signed by the sender, which in this case is also us.
	 */
	SignedDelivery createSignedDelivery(final DigitalMailDto dto) {
		final var signedDelivery = OBJECT_FACTORY.createSignedDelivery();
		signedDelivery.setDelivery(createSecureDelivery(dto));
		return signedDelivery;
	}

	XMLGregorianCalendar createTimestamp() throws DatatypeConfigurationException {
		final var now = new GregorianCalendar();
		return DatatypeFactory.newInstance().newXMLGregorianCalendar(now);
	}

	SecureDelivery createSecureDelivery(final DigitalMailDto dto) {
		final var secureDelivery = new SecureDelivery();
		secureDelivery.setHeader(createSecureDeliveryHeader(dto));
		secureDelivery.getMessages().add(createSecureMessage(dto));
		return secureDelivery;
	}

	SecureMessage createSecureMessage(final DigitalMailDto dto) {
		final var secureMessage = new SecureMessage();
		secureMessage.setHeader(createMessageHeader(dto));
		secureMessage.setBody(createMessageBody(dto));
		if (isNotEmpty(dto.getAttachments())) {
			secureMessage.getAttachments().addAll(createAttachments(dto.getAttachments()));
		}
		return secureMessage;
	}

	List<Attachment> createAttachments(final List<File> attachments) {
		if (isEmpty(attachments)) {
			return Collections.emptyList();
		}

		return attachments.stream()
			.map(attachment -> {
				// We need to decode the base64-encoded string before we convert it to a byte array.
				final var attachmentBytes = Base64.decode(
					attachment.getBody());

				final var mailAttachment = new Attachment();
				mailAttachment.setBody(attachmentBytes);
				mailAttachment.setContentType(attachment.getContentType());
				mailAttachment.setFilename(attachment.getFilename());
				mailAttachment.setChecksum(createMd5Checksum(attachmentBytes));
				return mailAttachment;
			})
			.toList();
	}

	String createMd5Checksum(final byte[] attachmentBodyBytes) {
		try {
			final var md5 = MessageDigest.getInstance("MD5");
			md5.update(attachmentBodyBytes);
			final var digest = md5.digest();
			return DatatypeConverter.printHexBinary(digest);
		} catch (final Exception e) {
			log.error("Couldn't create MD5-checksum for attachment", e);
			throw Problem.builder()
				.withTitle("Couldn't create MD5-checksum for attachment")
				.withStatus(INTERNAL_SERVER_ERROR)

				.build();
		}
	}

	/**
	 * Creates the &lt;v3:header&gt;-element
	 *
	 * @param  dto to be translated into a {@link MessageHeader}
	 * @return     A {@link MessageHeader}
	 */
	MessageHeader createMessageHeader(final DigitalMailDto dto) {
		final var messageHeader = new MessageHeader();
		messageHeader.setSubject(dto.getHeaderSubject());
		messageHeader.setSupportinfo(createSupportInfo(dto));
		messageHeader.setLanguage("svSE");
		messageHeader.setId(RequestId.get());
		return messageHeader;
	}

	/**
	 * Creates the Supportinfo-element
	 *
	 * @param  dto to be translated into a {@link SupportInfo}
	 * @return     A {@link SupportInfo} containing the support information
	 */

	SupportInfo createSupportInfo(final DigitalMailDto dto) {
		final var supportInfo = new SupportInfo();
		supportInfo.setText(dto.getSupportInfo().getSupportText());
		supportInfo.setURL(dto.getSupportInfo().getContactInformationUrl());
		supportInfo.setPhoneNumber(dto.getSupportInfo().getContactInformationPhoneNumber());
		supportInfo.setEmailAdress(dto.getSupportInfo().getContactInformationEmail());
		return supportInfo;
	}

	/**
	 * Creates the body-element Will create an empty body if no bodyInformation object or body is present.
	 *
	 * @param  dto to be translated into a {@link MessageBody}
	 * @return     A {@link MessageBody}
	 */
	MessageBody createMessageBody(final DigitalMailDto dto) {
		final var messageBody = new MessageBody();

		// Make sure we actually have a body to send
		if (dto.getBodyInformation() != null && StringUtils.isNotBlank(dto.getBodyInformation().getBody())) {
			messageBody.setBody(createBody(dto.getBodyInformation()));
			messageBody.setContentType(dto.getBodyInformation().getContentType());
		} else {
			// Create an "empty" body.
			messageBody.setBody(new byte[0]);
			messageBody.setContentType(MediaType.TEXT_PLAIN_VALUE);
		}

		return messageBody;
	}

	byte[] createBody(final BodyInformation bodyInformation) {
		if (bodyInformation.getContentType().equals(MediaType.TEXT_PLAIN_VALUE)) {
			// If it's regular text, encode it.
			return bodyInformation.getBody().getBytes(StandardCharsets.UTF_8);
		} else {
			// If it's text/html, we need to first decode the content and then "encode" it.
			return Base64.decode(bodyInformation.getBody());
		}
	}

	SecureDeliveryHeader createSecureDeliveryHeader(final DigitalMailDto dto) {
		final var secureDeliveryHeader = new SecureDeliveryHeader();
		secureDeliveryHeader.setSender(createSender(dto.getOrganizationNumber()));
		secureDeliveryHeader.setRecipient(dto.getRecipientId());
		return secureDeliveryHeader;
	}

	Sender createSender(final String organizationNumber) {
		final var sender = new Sender();
		sender.setId(prefixOrgNumber(organizationNumber));

		// We've already verified the sender is valid, if we're here it will be present
		sender.setName(properties.supportedSenders().get(organizationNumber));
		return sender;
	}

}
