package se.sundsvall.digitalmail.api.model.validation.impl;

import static java.util.Objects.isNull;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import se.sundsvall.digitalmail.api.model.BodyInformation;
import se.sundsvall.digitalmail.api.model.validation.ValidBodyInformation;

public class ValidBodyInformationConstraintValidator implements ConstraintValidator<ValidBodyInformation, BodyInformation> {

    private boolean nullable;
    private String invalidContentTypeMessage;

    @Override
    public void initialize(final ValidBodyInformation annotation) {
        nullable = annotation.nullable();
        invalidContentTypeMessage = annotation.invalidContentTypeMessage();
    }

    @Override
    public boolean isValid(final BodyInformation value, final ConstraintValidatorContext context) {
        if (isNull(value) && nullable) {
            return true;
        } else if (isNull(value)) {
            return false;
        }

        // If Jackson is unable to resolve the content type to an accepted one, treat the body
        // information as invalid
        if (value instanceof BodyInformation.Unknown) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(invalidContentTypeMessage).addConstraintViolation();

            return false;
        }

        return isNotBlank(value.getBody()) && isNotBlank(value.getContentType());
    }
}
