package se.sundsvall.digitalmail.integration;

import org.springframework.boot.context.properties.bind.DefaultValue;

import jakarta.validation.constraints.NotBlank;

public record OAuth2(

	@NotBlank String tokenUrl,

	@NotBlank String clientId,

	@NotBlank String clientSecret,

	@DefaultValue("client_credentials") String authorizationGrantType) {}
