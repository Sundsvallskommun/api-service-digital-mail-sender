package se.sundsvall.digitalmail.api.model.validation.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.validation.ConstraintValidatorContext;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import se.sundsvall.digitalmail.api.model.BodyInformation;
import se.sundsvall.digitalmail.api.model.validation.ValidBodyInformation;

@ExtendWith(MockitoExtension.class)
class ValidBodyInformationConstraintValidatorTests {

    @Mock
    private ValidBodyInformation mockAnnotation;

    @InjectMocks
    private ValidBodyInformationConstraintValidator validator;

    @AfterEach
    void verifyAnnotationInvocations() {
        verify(mockAnnotation).nullable();
        verify(mockAnnotation).invalidContentTypeMessage();
    }

    @Test
    void valueInstanceOfUnknownType() {
        final var mockConstraintViolationBuilder = mock(ConstraintValidatorContext.ConstraintViolationBuilder.class);
        final var mockContext = mock(ConstraintValidatorContext.class);
        when(mockContext.buildConstraintViolationWithTemplate(any(String.class))).thenReturn(mockConstraintViolationBuilder);

        when(mockAnnotation.invalidContentTypeMessage()).thenReturn("someInvalidContentTypeMessage");

        final var unknownBodyInformation = BodyInformation.Unknown.builder().build();

        validator.initialize(mockAnnotation);

        assertThat(validator.isValid(unknownBodyInformation, mockContext)).isFalse();

        verify(mockContext).disableDefaultConstraintViolation();
        verify(mockContext).buildConstraintViolationWithTemplate(any(String.class));
        verify(mockConstraintViolationBuilder).addConstraintViolation();
    }

    @Test
    void valueWithNullBody() {
        final var bodyInformation = BodyInformation.PlainText.builder()
            .withContentType("someContentType")
            .build();

        validator.initialize(mockAnnotation);

        assertThat(validator.isValid(bodyInformation, null)).isFalse();
    }


    @Test
    void valueWithBlankBody() {
        final var bodyInformation = BodyInformation.PlainText.builder()
            .withContentType("someContentType")
            .withBody("")
            .build();

        validator.initialize(mockAnnotation);

        assertThat(validator.isValid(bodyInformation, null)).isFalse();
    }

    @Test
    void nullValueWhenNullableIsFalse() {
        validator.initialize(mockAnnotation);

        assertThat(validator.isValid(null, null)).isFalse();
    }

    @Test
    void nullValueWhenNullableIsTrue() {
        when(mockAnnotation.nullable()).thenReturn(true);

        validator.initialize(mockAnnotation);

        assertThat(validator.isValid(null, null)).isTrue();
    }
}
