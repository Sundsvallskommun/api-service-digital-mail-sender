package se.sundsvall.digitalmail.api.model.validation.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.hibernate.validator.internal.engine.constraintvalidation.ConstraintValidatorContextImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import se.sundsvall.dept44.test.annotation.resource.Load;
import se.sundsvall.dept44.test.extension.ResourceLoaderExtension;
import se.sundsvall.digitalmail.api.model.validation.ValidHtml;

@ExtendWith({MockitoExtension.class, ResourceLoaderExtension.class})
class ValidHtmlConstraintValidatorTests {

    @Mock
    private ValidHtml mockAnnotation;

    @Mock
    private ConstraintValidatorContextImpl mockConstraintValidatorContext;

    @InjectMocks
    private ValidHtmlConstraintValidator validator;

    @Test
    void validValue(@Load("valid-html.base64") final String html) {
        validator.initialize(mockAnnotation);

        assertThat(validator.isValid(html, null)).isTrue();

        verify(mockAnnotation).nullable();
    }

    @Test
    void invalidValue(@Load("invalid-html.base64") final String html) {
        validator.initialize(mockAnnotation);

        assertThat(validator.isValid(html, mockConstraintValidatorContext)).isFalse();

        verify(mockAnnotation).nullable();
        verify(mockConstraintValidatorContext).addMessageParameter(any(String.class), any());
    }

    @ParameterizedTest(name = "[{index}] value=\"{0}\"")
    @ValueSource(strings = {"", "   "})
    void emptyHtml(final String value) {
        validator.initialize(mockAnnotation);

        assertThat(validator.isValid(value, null)).isFalse();

        verify(mockAnnotation).nullable();
    }

    @Test
    void nullValueWhenNullableIsFalse() {
        validator.initialize(mockAnnotation);

        assertThat(validator.isValid(null, null)).isFalse();

        verify(mockAnnotation).nullable();
    }

    @Test
    void nullValueWhenNullableIsTrue() {
        when(mockAnnotation.nullable()).thenReturn(true);

        validator.initialize(mockAnnotation);

        assertThat(validator.isValid(null, null)).isTrue();

        verify(mockAnnotation).nullable();
    }
}
