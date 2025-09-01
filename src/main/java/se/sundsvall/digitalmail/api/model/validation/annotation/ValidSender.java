package se.sundsvall.digitalmail.api.model.validation.annotation;

import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import se.sundsvall.digitalmail.api.model.validation.annotation.impl.ValidSenderConstraintValidator;

@Documented
@Target({
	PARAMETER
})
@Retention(RUNTIME)
@Constraint(validatedBy = ValidSenderConstraintValidator.class)
public @interface ValidSender {

	String message() default "Sending organization is not registered as authorized sender";

	boolean nullable() default false;

	Class<?>[] groups() default {};

	Class<? extends Payload>[] payload() default {};
}
