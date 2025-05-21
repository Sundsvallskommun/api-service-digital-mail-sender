package se.sundsvall.digitalmail.integration.messaging;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;
import se.sundsvall.digitalmail.integration.OAuth2;

@Validated
@ConfigurationProperties("integration.messaging")
public record MessagingProperties(
	@NotBlank String apiUrl,
	@DefaultValue("PT5S") Duration connectTimeout,
	@DefaultValue("PT15S") Duration readTimeout,
	@NotNull @Valid OAuth2 oauth2) {
}
