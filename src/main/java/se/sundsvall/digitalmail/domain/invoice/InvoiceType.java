package se.sundsvall.digitalmail.domain.invoice;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Getter
@Schema(description = "Invoice type", examples = "INVOICE", defaultValue = "INVOICE")
public enum InvoiceType {
	INVOICE("invoice"),
	REMINDER("invoice.reminder");

	private final String value;

	InvoiceType(final String value) {
		this.value = value;
	}
}
