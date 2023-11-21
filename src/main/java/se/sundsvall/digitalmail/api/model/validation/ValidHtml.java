package se.sundsvall.digitalmail.api.model.validation;

import static java.lang.annotation.ElementType.CONSTRUCTOR;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import se.sundsvall.digitalmail.api.model.validation.impl.ValidHtmlConstraintValidator;

/**
 * The annotated element must be valid HTML.
 */
@Documented
@Target({FIELD, CONSTRUCTOR, METHOD, PARAMETER})
@Retention(RUNTIME)
@Constraint(validatedBy = ValidHtmlConstraintValidator.class)
public @interface ValidHtml {

    String message() default "invalid HTML: {errors}";

    boolean nullable() default false;

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
