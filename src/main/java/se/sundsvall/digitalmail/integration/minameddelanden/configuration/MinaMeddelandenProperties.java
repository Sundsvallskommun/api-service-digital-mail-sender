package se.sundsvall.digitalmail.integration.minameddelanden.configuration;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "integration.minameddelanden")
public record MinaMeddelandenProperties(

	@NotEmpty List<@NotBlank String> supportedSuppliers,
	@NotBlank String recipientUrl,
	@DefaultValue("2097152") long messageMaxSize,
	@DefaultValue("5000") long connectTimeout,
	@DefaultValue("120000") long readTimeout,
	@NotEmpty List<Sender> senders) {

	public record Sender(
		String name,
		String id,
		String keyStoreAsBase64,
		String keyStorePassword,
		String alias) {
	}
}
