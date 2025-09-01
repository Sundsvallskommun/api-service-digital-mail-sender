package se.sundsvall.digitalmail.integration.skatteverket;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "integration.skatteverket")
public record SkatteverketProperties(

	@NotEmpty List<@NotBlank String> supportedSuppliers,

	@DefaultValue("true") boolean shouldUseKeystore,

	@NotBlank String recipientUrl,

	String notificationUrl,

	@NotBlank String keyStorePassword,

	String keyStoreAsBase64,

	@DefaultValue("2097152") long messageMaxSize,

	@DefaultValue("5000") long connectTimeout,

	@DefaultValue("120000") long readTimeout,

	Map<String, String> supportedSenders) {}
