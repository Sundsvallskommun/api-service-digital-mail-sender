package se.sundsvall.digitalmail.api.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import se.sundsvall.digitalmail.api.validation.impl.ValidInvoiceDueDateConstraintValidator;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Documented
@Target(TYPE)
@Retention(RUNTIME)
@Constraint(validatedBy = ValidInvoiceDueDateConstraintValidator.class)
public @interface ValidInvoiceDueDate {

	String message() default "Due date must be a future date for an invoice";

	Class<?>[] groups() default {};

	Class<? extends Payload>[] payload() default {};
}
