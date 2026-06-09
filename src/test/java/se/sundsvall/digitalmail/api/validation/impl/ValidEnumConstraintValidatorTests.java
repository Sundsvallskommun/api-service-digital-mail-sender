package se.sundsvall.digitalmail.api.validation.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.digitalmail.api.validation.ValidEnum;
import se.sundsvall.digitalmail.domain.invoice.InvoiceType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;

@ExtendWith(MockitoExtension.class)
class ValidEnumConstraintValidatorTests {

	@Mock
	private ValidEnum mockAnnotation;

	@Test
	void validValues() {
		final var validator = validatorFor(InvoiceType.class);

		assertThat(validator.isValid("INVOICE", null)).isTrue();
		assertThat(validator.isValid("REMINDER", null)).isTrue();
	}

	@ParameterizedTest
	@ValueSource(strings = {
		"invoice", "reminder", "FOO", "", "   "
	})
	void invalidValues(final String value) {
		final var validator = validatorFor(InvoiceType.class);

		assertThat(validator.isValid(value, null)).isFalse();
	}

	@Test
	void nullIsValidAndDeferredToNotNull() {
		final var validator = validatorFor(InvoiceType.class);

		assertThat(validator.isValid(null, null)).isTrue();
	}

	private ValidEnumConstraintValidator validatorFor(final Class<? extends Enum<?>> enumClass) {
		doReturn(enumClass).when(mockAnnotation).value();
		final var validator = new ValidEnumConstraintValidator();
		validator.initialize(mockAnnotation);
		return validator;
	}
}
