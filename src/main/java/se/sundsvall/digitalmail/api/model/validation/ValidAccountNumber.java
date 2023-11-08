package se.sundsvall.digitalmail.api.model.validation;

import static java.lang.annotation.ElementType.CONSTRUCTOR;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import se.sundsvall.digitalmail.api.model.validation.impl.ValidAccountNumberConstraintValidator;

/**
 * The annotated element must be a valid BANKGIRO or PLUSGIRO number.
 */
@Documented
@Target({FIELD, CONSTRUCTOR, PARAMETER})
@Retention(RUNTIME)
@Constraint(validatedBy = ValidAccountNumberConstraintValidator.class)
public @interface ValidAccountNumber {

    String message() default "not a valid account number";

    boolean nullable() default false;

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
