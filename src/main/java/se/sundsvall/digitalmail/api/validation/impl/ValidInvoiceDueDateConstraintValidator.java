package se.sundsvall.digitalmail.api.validation.impl;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.time.LocalDate;
import java.time.ZoneId;
import se.sundsvall.digitalmail.api.model.DigitalInvoiceRequest;
import se.sundsvall.digitalmail.api.validation.ValidInvoiceDueDate;

import static java.util.Objects.isNull;
import static se.sundsvall.digitalmail.domain.invoice.InvoiceType.INVOICE;

public class ValidInvoiceDueDateConstraintValidator implements ConstraintValidator<ValidInvoiceDueDate, DigitalInvoiceRequest> {

	private static final ZoneId ZONE = ZoneId.of("Europe/Stockholm");

	@Override
	public boolean isValid(final DigitalInvoiceRequest request, final ConstraintValidatorContext context) {
		if (isNull(request) || !INVOICE.name().equals(request.type()) || isNull(request.details()) || isNull(request.details().dueDate())) {
			return true;
		}

		if (request.details().dueDate().isAfter(LocalDate.now(ZONE))) {
			return true;
		}

		context.disableDefaultConstraintViolation();
		context.buildConstraintViolationWithTemplate(context.getDefaultConstraintMessageTemplate())
			.addPropertyNode("details")
			.addPropertyNode("dueDate")
			.addConstraintViolation();
		return false;
	}
}
