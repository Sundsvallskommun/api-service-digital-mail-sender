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

import se.sundsvall.digitalmail.api.model.validation.impl.ValidBodyInformationConstraintValidator;

/**
 * The annotated element must have contentType and body fields set and non-empty. Also, ensures that
 * the contentType can actually be handled.
 */
@Documented
@Target({FIELD, CONSTRUCTOR, PARAMETER})
@Retention(RUNTIME)
@Constraint(validatedBy = ValidBodyInformationConstraintValidator.class)
public @interface ValidBodyInformation {

    String message() default "contentType and body cannot be null or empty";

    String invalidContentTypeMessage() default "invalid contentType";

    boolean nullable() default false;

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
