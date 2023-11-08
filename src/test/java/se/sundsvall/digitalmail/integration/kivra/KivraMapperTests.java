package se.sundsvall.digitalmail.integration.kivra;

import static org.assertj.core.api.Assertions.assertThat;
import static se.sundsvall.digitalmail.TestObjectFactory.generateInvoiceDto;
import static se.sundsvall.digitalmail.integration.kivra.KivraMapper.DATE_FORMATTER;
import static se.sundsvall.digitalmail.integration.kivra.KivraMapper.mapInvoiceToContent;

import org.junit.jupiter.api.Test;

import generated.com.kivra.Payment;

class KivraMapperTests {

    @Test
    void testMapInvoiceToContent() {
        final var ssn = "someSsn";
        final var invoiceDto = generateInvoiceDto();
        invoiceDto.setSsn(ssn);

        final var content = mapInvoiceToContent(invoiceDto);

        assertThat(content.getSsn()).isEqualTo(ssn);
        assertThat(content.getSubject()).isEqualTo(invoiceDto.getSubject());
        assertThat(content.getGeneratedAt()).isEqualTo(invoiceDto.getGeneratedAt().format(DATE_FORMATTER));
        assertThat(content.getType()).isEqualTo(invoiceDto.getType().getValue());
        assertThat(content.getFiles()).hasSameSizeAs(invoiceDto.getFiles());
        assertThat(content.getContext()).isNotNull();
        assertThat(content.getContext().getInvoice()).isNotNull().satisfies(invoice -> {
            assertThat(invoice.getInvoiceReference()).isEqualTo(invoiceDto.getReference());
            assertThat(invoice.getPaymentOrPaymentMultipleOptions()).isInstanceOfSatisfying(Payment.class, payment -> {
                assertThat(payment.getPayable()).isTrue();
                assertThat(payment.getCurrency()).isEqualTo(Payment.CurrencyEnum.SEK);
                assertThat(payment.getDueDate()).isEqualTo(invoiceDto.getDueDate().format(DATE_FORMATTER));
                assertThat(payment.getTotalOwed()).isEqualTo(invoiceDto.getAmount());
                assertThat(payment.getType()).isEqualTo(Payment.TypeEnum.fromValue(invoiceDto.getPaymentReferenceType().name()));
                assertThat(payment.getMethod()).isEqualTo(Payment.MethodEnum.fromValue(invoiceDto.getAccountType().getValue()));
                assertThat(payment.getAccount()).isEqualTo(invoiceDto.getAccountNumber());
                assertThat(payment.getReference()).isEqualTo(invoiceDto.getPaymentReference());
            });
        });
    }
}
