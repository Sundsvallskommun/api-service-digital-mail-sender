package se.sundsvall.digitalmail.integration.skatteverket;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.ws.client.WebServiceClientException;
import org.springframework.ws.client.core.WebServiceTemplate;
import org.springframework.ws.client.support.interceptor.ClientInterceptorAdapter;
import org.springframework.ws.context.MessageContext;
import org.springframework.ws.soap.SoapMessage;
import org.zalando.logbook.Logbook;
import org.zalando.problem.Problem;
import org.zalando.problem.Status;

import se.sundsvall.dept44.configuration.webservicetemplate.WebServiceTemplateBuilder;

@Configuration
class SkatteverketClientConfig {
    
    private final Logbook logbook;
    private final SkatteverketProperties properties;

    SkatteverketClientConfig(final SkatteverketProperties properties, final Logbook logbook) {
        this.logbook = logbook;
        this.properties = properties;
    }
    
    //Separate the beans since we don't want the "reachable" one to get intercepted for the size-check.
    @Bean("skatteverketSendmailWebserviceTemplate")
    WebServiceTemplate notificationWebserviceTemplate() {
        final var builder = new WebServiceTemplateBuilder()
            .withConnectTimeout(Duration.ofMillis(properties.connectTimeout()))
            .withReadTimeout(Duration.ofMillis(properties.readTimeout()))
            .withLogbook(logbook)
            .withPackageToScan("se.gov.minameddelanden.schema");
    
        if (properties.shouldUseKeystore()) {
            loadKeyStore(builder);
        }
        
        //Since we need to set the url dynamically we won't set the base url here.
        return builder
            .withClientInterceptor(new SoapMessageSizeInterceptor(properties.messageMaxSize()))
            .build();
    }
    
    @Bean("skatteverketIsReachableWebserviceTemplate")
    WebServiceTemplate recipientWebserviceTemplate() {
        final var builder = new WebServiceTemplateBuilder()
            .withConnectTimeout(Duration.ofMillis(properties.connectTimeout()))
            .withReadTimeout(Duration.ofMillis(properties.readTimeout()))
            .withLogbook(logbook)
            .withPackageToScan("se.gov.minameddelanden.schema");
    
        if (properties.shouldUseKeystore()) {
            loadKeyStore(builder);
        }

        return builder
            .withBaseUrl(properties.recipientUrl())
            .build();
    }

    private void loadKeyStore(WebServiceTemplateBuilder builder) {
        builder
                .withKeyStoreData(Base64.getDecoder().decode(properties.keyStoreAsBase64().getBytes(StandardCharsets.UTF_8)))
                .withKeyStorePassword(properties.keyStorePassword());
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

        private void checkSizeOfMessage(ByteArrayOutputStream outputStream) {
            final var length = outputStream.toString(StandardCharsets.UTF_8).getBytes(StandardCharsets.UTF_8).length;

            if (length > maxSize) {
                throw Problem.builder()
                    .withTitle("Message is too big to be sent as a digital mail.")
                    .withStatus(Status.BAD_REQUEST)
                    .withDetail("Size is: " + length + " bytes. Max allowed is: " + maxSize + " bytes.")
                    .build();
            }
        }
    }

}
