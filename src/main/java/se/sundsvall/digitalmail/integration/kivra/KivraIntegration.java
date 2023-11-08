package se.sundsvall.digitalmail.integration.kivra;

import static se.sundsvall.digitalmail.integration.kivra.KivraMapper.mapInvoiceToContent;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@EnableConfigurationProperties(KivraIntegrationProperties.class)
public class KivraIntegration {

    static final String INTEGRATION_NAME = "Kivra";

    private final KivraClient client;

    KivraIntegration(final KivraClient client) {
        this.client = client;
    }

    public boolean sendInvoice(final InvoiceDto invoiceDto) {
        final var content = mapInvoiceToContent(invoiceDto);
        final var response = client.postContent(content);

        return response.getStatusCode().is2xxSuccessful();
    }
}
