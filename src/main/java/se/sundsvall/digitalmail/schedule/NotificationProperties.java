package se.sundsvall.digitalmail.schedule;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("scheduler.certificate-health.notification")
public record NotificationProperties(
	@Valid Mail mail,
	@Valid Slack slack) {

	public record Mail(List<@NotNull @Email String> recipients, @NotBlank String subject, @NotNull Sender sender) {
	}

	public record Sender(String name, @NotNull @Email String emailAddress) {
	}

	public record Slack(@NotBlank String message, @NotBlank String channel, @NotBlank String token) {
	}

}
