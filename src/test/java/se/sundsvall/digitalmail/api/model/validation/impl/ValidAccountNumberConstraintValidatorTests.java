package se.sundsvall.digitalmail.api.model.validation.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import se.sundsvall.digitalmail.api.model.validation.ValidAccountNumber;

@ExtendWith(MockitoExtension.class)
class ValidAccountNumberConstraintValidatorTests {

    @Mock
    private ValidAccountNumber mockAnnotation;

    @InjectMocks
    private ValidAccountNumberConstraintValidator validator;

    @ParameterizedTest
    @ValueSource(strings = {"9911884", "991-1884", "5237-4055", "5989-2810", "670-6493", "5840-6646", "150-6047"})
    void validAccountNumber(final String value) {
        validator.initialize(mockAnnotation);

        assertThat(validator.isValid(value, null)).isTrue();

        verify(mockAnnotation).nullable();
    }

    @ParameterizedTest
    @ValueSource(strings = {"9911885", "991-1885", "12345x", "invalid"})
    void invalidAccountNumber(final String value) {
        validator.initialize(mockAnnotation);

        assertThat(validator.isValid(value, null)).isFalse();

        verify(mockAnnotation).nullable();
    }

    @Test
    void nullAccountNumberWhenNullableIsFalse() {
        validator.initialize(mockAnnotation);

        assertThat(validator.isValid(null, null)).isFalse();

        verify(mockAnnotation).nullable();
    }

    @Test
    void nullAccountNumberWhenNullableIsTrue() {
        when(mockAnnotation.nullable()).thenReturn(true);

        validator.initialize(mockAnnotation);

        assertThat(validator.isValid(null, null)).isTrue();

        verify(mockAnnotation).nullable();
    }
}
