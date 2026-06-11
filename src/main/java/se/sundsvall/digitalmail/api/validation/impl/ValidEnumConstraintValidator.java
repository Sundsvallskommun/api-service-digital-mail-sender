package se.sundsvall.digitalmail.api.validation.impl;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.Arrays;
import se.sundsvall.digitalmail.api.validation.ValidEnum;

import static java.util.Objects.isNull;

public class ValidEnumConstraintValidator implements ConstraintValidator<ValidEnum, String> {

	private Class<? extends Enum<?>> enumClass;

	@Override
	public void initialize(final ValidEnum annotation) {
		this.enumClass = annotation.value();
	}

	@Override
	public boolean isValid(final String value, final ConstraintValidatorContext context) {
		// Null is considered valid; let @NotNull handle required-ness.
		if (isNull(value)) {
			return true;
		}

		return Arrays.stream(enumClass.getEnumConstants())
			.anyMatch(constant -> constant.name().equals(value));
	}
}
