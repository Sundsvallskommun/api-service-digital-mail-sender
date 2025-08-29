package se.sundsvall.digitalmail.api.model.validation.annotation.impl;

import static java.util.Optional.ofNullable;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.stereotype.Component;
import se.sundsvall.digitalmail.api.model.validation.annotation.ValidSender;
import se.sundsvall.digitalmail.integration.skatteverket.SkatteverketProperties;

@Component
public class ValidSenderContraintValidator implements ConstraintValidator<ValidSender, String> {

	private final SkatteverketProperties skatteverketProperties;

	public ValidSenderContraintValidator(SkatteverketProperties skatteverketProperties) {
		this.skatteverketProperties = skatteverketProperties;
	}

	@Override
	public boolean isValid(String organizationNumber, ConstraintValidatorContext context) {
		var validSenders = skatteverketProperties.supportedSenders();

		// Check if the map contains the organization number as a key
		return ofNullable(validSenders.get(organizationNumber)).isPresent();
	}
}
