package se.sundsvall.digitalmail.integration;

import jakarta.validation.constraints.NotBlank;

import org.springframework.boot.context.properties.bind.DefaultValue;

public record OAuth2(

    @NotBlank
    String tokenUrl,

    @NotBlank
    String clientId,

    @NotBlank
    String clientSecret,

    @DefaultValue("client_credentials")
    String authorizationGrantType) { }
