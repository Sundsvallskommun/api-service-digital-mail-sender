package se.sundsvall.digitalmail.api.validation.impl;

import jakarta.validation.ConstraintValidatorContext;
import java.time.LocalDate;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.digitalmail.api.model.DigitalInvoiceRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.verify;
import static se.sundsvall.digitalmail.TestObjectFactory.generateInvoiceRequest;

@ExtendWith(MockitoExtension.class)
class ValidInvoiceDueDateConstraintValidatorTests {

	private static final ZoneId ZONE = ZoneId.of("Europe/Stockholm");

	@Mock(answer = RETURNS_DEEP_STUBS)
	private ConstraintValidatorContext contextMock;

	private final ValidInvoiceDueDateConstraintValidator validator = new ValidInvoiceDueDateConstraintValidator();

	@Test
	void invoiceWithFutureDueDatesIsValid() {
		assertThat(validator.isValid(request("INVOICE", LocalDate.now(ZONE).plusDays(1)), contextMock)).isTrue();
	}

	@Test
	void invoiceWithTodayIsInvalid() {
		assertThat(validator.isValid(request("INVOICE", LocalDate.now(ZONE).minusDays(0)), contextMock)).isFalse();

		verify(contextMock).disableDefaultConstraintViolation();
	}

	@Test
	void invoiceWithPastDueDateIsInvalid() {
		assertThat(validator.isValid(request("INVOICE", LocalDate.now(ZONE).minusDays(1)), contextMock)).isFalse();

		verify(contextMock).disableDefaultConstraintViolation();
	}

	@Test
	void reminderWithPastDueDateIsValid() {
		assertThat(validator.isValid(request("REMINDER", LocalDate.now(ZONE).minusDays(1)), contextMock)).isTrue();
	}

	@Test
	void missingDueDateGetTakenCareOfByNotNull() {
		assertThat(validator.isValid(request("INVOICE", null), contextMock)).isTrue();
	}

	private static DigitalInvoiceRequest request(final String type, final LocalDate dueDate) {
		final var invoiceRequest = generateInvoiceRequest();
		return new DigitalInvoiceRequest(invoiceRequest.partyId(), type, invoiceRequest.subject(), invoiceRequest.reference(), invoiceRequest.payable(),
			new DigitalInvoiceRequest.Details(123.45f, dueDate, "SE_OCR", "426523791", "BANKGIRO", "991-1884"),
			invoiceRequest.files());
	}
}
