package se.sundsvall.digitalmail.api.validation.impl;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import se.sundsvall.digitalmail.api.healthcheck.SenderHealthIndicator;
import se.sundsvall.digitalmail.api.validation.ValidSender;
import se.sundsvall.digitalmail.integration.skatteverket.SkatteverketProperties;

import static org.springframework.util.CollectionUtils.isEmpty;

@Component
@RequiredArgsConstructor
public class ValidSenderConstraintValidator implements ConstraintValidator<ValidSender, String> {

	private final SkatteverketProperties skatteverketProperties;
	private final SenderHealthIndicator senderHealthIndicator;

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
