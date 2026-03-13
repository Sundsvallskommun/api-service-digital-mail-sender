package se.sundsvall.digitalmail.integration.party;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;
import se.sundsvall.digitalmail.integration.OAuth2;

@Validated
@ConfigurationProperties(prefix = "integration.party")
public record PartyProperties(
	@NotBlank String apiUrl,
	@NotNull @Valid OAuth2 oauth2,
	@DefaultValue("PT5S") Duration connectTimeout,
	@DefaultValue("PT15S") Duration readTimeout,
	@DefaultValue("1000") int maxPartyIdsPerCall) {}
