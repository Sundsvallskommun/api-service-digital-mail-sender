package se.sundsvall.digitalmail.api.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import se.sundsvall.digitalmail.api.validation.impl.ValidEnumConstraintValidator;

import static java.lang.annotation.ElementType.CONSTRUCTOR;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * The annotated String must match the name of one of the constants of the given enum (case-sensitive),
 * mirroring Jackson's default enum binding. {@code null} is considered valid here — combine with
 * {@code @NotNull} to require a value.
 */
@Documented
@Target({
	FIELD, CONSTRUCTOR, PARAMETER
})
@Retention(RUNTIME)
@Constraint(validatedBy = ValidEnumConstraintValidator.class)
public @interface ValidEnum {

	Class<? extends Enum<?>> value();

	String message() default "not a valid value, must be one of the allowed values";

	Class<?>[] groups() default {};

	Class<? extends Payload>[] payload() default {};
}
