package se.sundsvall.digitalmail.api.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import se.sundsvall.digitalmail.api.validation.impl.ValidPdfConstraintValidator;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Documented
@Target({
	FIELD, PARAMETER
})
@Retention(RUNTIME)
@Constraint(validatedBy = ValidPdfConstraintValidator.class)
public @interface ValidPdf {

	String message() default "not a valid PDF";

	Class<?>[] groups() default {};

	Class<? extends Payload>[] payload() default {};
}
