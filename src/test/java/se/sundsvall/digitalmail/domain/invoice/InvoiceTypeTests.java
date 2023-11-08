package se.sundsvall.digitalmail.domain.invoice;

import static org.assertj.core.api.Assertions.assertThat;
import static se.sundsvall.digitalmail.domain.invoice.InvoiceType.INVOICE;
import static se.sundsvall.digitalmail.domain.invoice.InvoiceType.REMINDER;

import org.junit.jupiter.api.Test;

class InvoiceTypeTests {

    @Test
    void values() {
        assertThat(InvoiceType.values()).containsExactly(INVOICE, REMINDER);

        assertThat(INVOICE.getValue()).isEqualTo("invoice");
        assertThat(REMINDER.getValue()).isEqualTo("invoice.reminder");
    }
}
