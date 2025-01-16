package se.sundsvall.digitalmail.integration.kivra;

import static se.sundsvall.digitalmail.integration.kivra.KivraMapper.mapInvoiceToContent;

import generated.com.kivra.UserMatchV2SSN;
import java.util.Optional;
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

	/**
	 * Verify that the recipient has a mailbox in Kivra and that the recipient haven't opted out from receiving mail from
	 * the sender.
	 *
	 * @return true if the recipient has a mailbox in Kivra and haven't opted out from receiving mail from the sender, false
	 *         otherwise.
	 */
	public boolean verifyValidRecipient(final String legalId) {
		var userMatchV2SSN = new UserMatchV2SSN().addListItem(legalId);
		var response = client.postUserMatchSSN(userMatchV2SSN);

		return Optional.ofNullable(response.getBody())
			.map(body -> body.getList().contains(legalId))
			.orElse(false);
	}
}
