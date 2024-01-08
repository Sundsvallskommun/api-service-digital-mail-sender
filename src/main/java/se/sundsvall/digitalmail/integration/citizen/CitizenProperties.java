package se.sundsvall.digitalmail.integration.citizen;

import java.time.Duration;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

import se.sundsvall.digitalmail.integration.OAuth2;

@Validated
@ConfigurationProperties(prefix = "integration.citizen")
record CitizenProperties(

    @NotBlank
    String apiUrl,

    @NotNull
    @Valid
    OAuth2 oauth2,

    @DefaultValue("PT5S")
    Duration connectTimeout,
    @DefaultValue("PT15S")
    Duration readTimeout) { }
