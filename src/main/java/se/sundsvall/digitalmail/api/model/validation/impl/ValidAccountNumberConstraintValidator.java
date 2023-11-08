package se.sundsvall.digitalmail.api.model.validation.impl;

import static java.util.Objects.isNull;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import se.sundsvall.digitalmail.api.model.validation.ValidAccountNumber;

public class ValidAccountNumberConstraintValidator implements ConstraintValidator<ValidAccountNumber, String> {

    private boolean nullable;

    @Override
    public void initialize(final ValidAccountNumber annotation) {
        nullable = annotation.nullable();
    }

    @Override
    public boolean isValid(final String value, final ConstraintValidatorContext context) {
        if (isNull(value) && nullable) {
            return true;
        } else if (isNull(value)) {
            return false;
        }

        // Remove everything but digits from the value
        var cleanedValue = value.trim().replaceAll("\\D", "");
        // An empty cleaned value cannot be valid
        if (cleanedValue.isEmpty()) {
            return false;
        }

        var sum = 0;
        var alternate = false;

        // Start from the right, moving left
        for (var i = cleanedValue.length() - 1; i >= 0; --i) {
            // Get the current digit
            int digit = Character.getNumericValue(cleanedValue.charAt(i));
            // Double every other digit
            digit = alternate ? (digit * 2) : digit;
            // Subtract 9 if the value is greater than 9 (the same as summing the digits)
            digit = (digit > 9) ? (digit - 9) : digit;
            // Add the digit to the sum
            sum += digit;
            // Flip the alternate flag
            alternate = !alternate;
        }

        return (sum % 10) == 0;
    }
}
