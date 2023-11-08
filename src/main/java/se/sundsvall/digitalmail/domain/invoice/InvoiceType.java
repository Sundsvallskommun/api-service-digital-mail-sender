package se.sundsvall.digitalmail.domain.invoice;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Invoice type", example = "INVOICE", defaultValue = "INVOICE")
public enum InvoiceType {
    INVOICE("invoice"),
    REMINDER("invoice.reminder");

    private final String value;

    InvoiceType(final String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
