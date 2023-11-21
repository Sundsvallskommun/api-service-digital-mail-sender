package se.sundsvall.digitalmail.api.model.validation.impl;

import static java.util.Objects.isNull;
import static org.apache.commons.lang3.StringUtils.isBlank;

import java.io.ByteArrayInputStream;
import java.util.Base64;
import java.util.List;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.ValidationException;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.hibernate.validator.internal.engine.constraintvalidation.ConstraintValidatorContextImpl;

import se.sundsvall.digitalmail.api.model.validation.ValidHtml;

import nu.validator.client.EmbeddedValidator;

public class ValidHtmlConstraintValidator implements ConstraintValidator<ValidHtml, String> {

    private static final EmbeddedValidator HTML_VALIDATOR = new EmbeddedValidator();
    private static final Gson GSON = new GsonBuilder().create();
    private static final Base64.Decoder BASE64_DECODER = Base64.getDecoder();

    private boolean nullable;

    @Override
    public void initialize(final ValidHtml annotation) {
        nullable = annotation.nullable();
    }

    @Override
    public boolean isValid(final String value, final ConstraintValidatorContext context) {
        if (isNull(value) && nullable) {
            return true;
        } else if (isNull(value)) {
            return false;
        }

        // Treat empty HTML as invalid
        if (isBlank(value.trim())) {
            return false;
        }

        try {
            // Decode the BASE64-encoded HTML
            var html = new String(BASE64_DECODER.decode(value.trim()));
            // Validate
            var validationResultAsString = HTML_VALIDATOR.validate(new ByteArrayInputStream(html.getBytes()));
            // Check
            var validationResult = GSON.fromJson(validationResultAsString, ValidationResult.class);
            if (validationResult.isValid()) {
                return true;
            }

            ((ConstraintValidatorContextImpl) context).addMessageParameter("errors", validationResult.errors());

            return false;
        } catch (Exception e) {
            throw new ValidationException("Unable to validate HTML", e);
        }
    }

    /*
     * Simple validation result record. Just counts the number of validation errors, ignoring what
     * the errors actually are.
     */
    record ValidationResult(List<Message> messages) {

        List<String> errors() {
            return messages.stream()
                .filter(message -> "error".equalsIgnoreCase(message.type()))
                .map(Message::message)
                .toList();
        }

        boolean isValid() {
            return errors().isEmpty();
        }

        record Message(String type, String message) { }
    }
}
