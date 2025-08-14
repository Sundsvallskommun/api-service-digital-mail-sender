package se.sundsvall.digitalmail.integration.minameddelanden.configuration;

import static org.zalando.problem.Status.BAD_REQUEST;
import static org.zalando.problem.Status.NOT_FOUND;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.ws.client.WebServiceClientException;
import org.springframework.ws.client.core.WebServiceTemplate;
import org.springframework.ws.client.support.interceptor.ClientInterceptorAdapter;
import org.springframework.ws.context.MessageContext;
import org.springframework.ws.soap.SoapMessage;
import org.zalando.logbook.Logbook;
import org.zalando.problem.Problem;
import se.sundsvall.dept44.configuration.webservicetemplate.WebServiceTemplateBuilder;

@Component
public class MinaMeddelandenClientFactory {

	private final Map<String, WebServiceTemplate> isReachableWebServiceTemplates = new HashMap<>();
	private final Map<String, WebServiceTemplate> sendMailWebServiceTemplates = new HashMap<>();
	private final MinaMeddelandenProperties properties;
	private final Logbook logbook;

	public MinaMeddelandenClientFactory(final MinaMeddelandenProperties properties, final Logbook logbook) {
		this.properties = properties;
		this.logbook = logbook;

		init();
	}

	private void init() {
		properties.senders().forEach(sender -> {
			isReachableWebServiceTemplates.put(sender.name(), createIsReachableWebServiceTemplate(sender));
			sendMailWebServiceTemplates.put(sender.name(), createSendMailWebServiceTemplate(sender));
		});
	}

	public WebServiceTemplate getIsReachableWebServiceTemplate(final String senderName) {
		return Optional.ofNullable(isReachableWebServiceTemplates.get(senderName))
			.orElseThrow(() -> Problem.valueOf(NOT_FOUND, "No sender configuration found for: " + senderName));
	}

	public WebServiceTemplate getSendMailWebServiceTemplate(final String senderName) {
		return Optional.ofNullable(sendMailWebServiceTemplates.get(senderName))
			.orElseThrow(() -> Problem.valueOf(NOT_FOUND, "No sender configuration found for: " + senderName));
	}

	public WebServiceTemplate createIsReachableWebServiceTemplate(final MinaMeddelandenProperties.Sender sender) {
		return new WebServiceTemplateBuilder()
			.withConnectTimeout(Duration.ofSeconds(properties.connectTimeout()))
			.withReadTimeout(Duration.ofSeconds(properties.readTimeout()))
			.withBaseUrl(properties.recipientUrl())
			.withLogbook(logbook)
			.withPackageToScan("se.gov.minameddelanden.schema")
			.withKeyStoreData(Base64.getDecoder().decode(sender.keyStoreAsBase64().getBytes(StandardCharsets.UTF_8)))
			.withKeyStorePassword(sender.keyStorePassword())
			.build();
	}

	/**
	 * The url is set dynamically in the request, so no baseUrl is set.
	 *
	 * @param  sender the sender properties to use for the web service template
	 * @return        a WebServiceTemplate configured for sending digital mail
	 */
	public WebServiceTemplate createSendMailWebServiceTemplate(final MinaMeddelandenProperties.Sender sender) {
		return new WebServiceTemplateBuilder()
			.withConnectTimeout(Duration.ofSeconds(properties.connectTimeout()))
			.withReadTimeout(Duration.ofSeconds(properties.readTimeout()))
			.withClientInterceptor(new SoapMessageSizeInterceptor(properties.messageMaxSize()))
			.withLogbook(logbook)
			.withPackageToScan("se.gov.minameddelanden.schema")
			.withKeyStoreData(Base64.getDecoder().decode(sender.keyStoreAsBase64().getBytes(StandardCharsets.UTF_8)))
			.withKeyStorePassword(sender.keyStorePassword())
			.build();
	}

	static class SoapMessageSizeInterceptor extends ClientInterceptorAdapter {

		private static final Logger LOG = LoggerFactory.getLogger(SoapMessageSizeInterceptor.class);

		private final long maxSize;

		public SoapMessageSizeInterceptor(final long maxSize) {
			this.maxSize = maxSize;

			LOG.info("Max size of SOAP messages is set to {} bytes", maxSize);
		}

		@Override
		public boolean handleRequest(final MessageContext messageContext) throws WebServiceClientException {
			LOG.debug("Checking size of SOAP message");

			final var soapMessage = (SoapMessage) messageContext.getRequest();

			try (var outputStream = new ByteArrayOutputStream()) {
				soapMessage.writeTo(outputStream);
				checkSizeOfMessage(outputStream);
			} catch (IOException e) {
				LOG.warn("Couldn't calculate size of SOAP message, sending it anyway");
			}

			return true;
		}

		void checkSizeOfMessage(final ByteArrayOutputStream outputStream) {
			final var length = outputStream.toString(StandardCharsets.UTF_8).getBytes(StandardCharsets.UTF_8).length;

			if (length > maxSize) {
				throw Problem.builder()
					.withTitle("Message is too big to be sent as a digital mail.")
					.withStatus(BAD_REQUEST)
					.withDetail("Size is: " + length + " bytes. Max allowed is: " + maxSize + " bytes.")
					.build();
			}
		}
	}

}
