package se.sundsvall.digitalmail.integration.kivra;

import static se.sundsvall.digitalmail.integration.kivra.KivraMapper.mapInvoiceToContent;

import generated.com.kivra.UserMatchV2SSN;
import java.util.Optional;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@EnableConfigurationProperties(KivraProperties.class)
public class KivraIntegration {

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
		final var userMatchV2SSN = new UserMatchV2SSN().addListItem(legalId);
		final var response = client.postUserMatchSSN(userMatchV2SSN);

		return Optional.ofNullable(response.getBody())
			.map(body -> body.getList().contains(legalId))
			.orElse(false);
	}

	/**
	 * Method reads basic tenant information and will throw ClientAuthorizationException if certificate is not valid
	 * when communicating with Kivra https address.
	 */
	public void healthCheck() {
		client.getTenantInformation();
	}
}
