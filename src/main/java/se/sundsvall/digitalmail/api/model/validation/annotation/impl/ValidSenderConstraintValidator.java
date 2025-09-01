package se.sundsvall.digitalmail.api.model.validation.annotation.impl;

import static org.springframework.util.CollectionUtils.isEmpty;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.stereotype.Component;
import se.sundsvall.digitalmail.api.healthcheck.SenderHealthIndicator;
import se.sundsvall.digitalmail.api.model.validation.annotation.ValidSender;
import se.sundsvall.digitalmail.integration.skatteverket.SkatteverketProperties;

@Component
public class ValidSenderConstraintValidator implements ConstraintValidator<ValidSender, String> {

	private final SkatteverketProperties skatteverketProperties;
	private final SenderHealthIndicator senderHealthIndicator;

	public ValidSenderConstraintValidator(SkatteverketProperties skatteverketProperties, SenderHealthIndicator senderHealthIndicator) {
		this.skatteverketProperties = skatteverketProperties;
		this.senderHealthIndicator = senderHealthIndicator;
	}

	@Override
	public boolean isValid(final String organizationNumber, final ConstraintValidatorContext context) {
		final var validSenders = skatteverketProperties.supportedSenders();

		if (isEmpty(validSenders)) {
			senderHealthIndicator.setUnhealthy();
			return false;
		} else {
			senderHealthIndicator.setHealthy();
		}

		// Check if the map contains the organization number as a key
		return validSenders.containsKey(organizationNumber);
	}
}
