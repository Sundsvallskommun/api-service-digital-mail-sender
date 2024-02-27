package se.sundsvall.digitalmail.integration.kivra;

import static org.assertj.core.api.Assertions.assertThat;
import static se.sundsvall.digitalmail.TestObjectFactory.generateInvoiceDto;
import static se.sundsvall.digitalmail.integration.kivra.KivraMapper.DATE_FORMATTER;
import static se.sundsvall.digitalmail.integration.kivra.KivraMapper.mapInvoiceToContent;

import org.junit.jupiter.api.Test;

import generated.com.kivra.PaymentMultipleOptions;
import generated.com.kivra.PaymentMultipleOptionsOptionsInner;

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
        assertThat(content.getParts()).hasSameSizeAs(invoiceDto.getFiles());
        assertThat(content.getPaymentMultipleOptions()).isInstanceOfSatisfying(PaymentMultipleOptions.class, payment -> {
                assertThat(payment.getPayable()).isTrue();
                assertThat(payment.getCurrency()).isEqualTo(PaymentMultipleOptions.CurrencyEnum.SEK);
                assertThat(payment.getOptions().getFirst().getDueDate()).isEqualTo(invoiceDto.getDueDate().format(DATE_FORMATTER));
                assertThat(payment.getOptions().getFirst().getAmount()).isEqualTo(invoiceDto.getAmount().toString());
                assertThat(payment.getOptions().getFirst().getType()).isEqualTo(PaymentMultipleOptionsOptionsInner.TypeEnum.fromValue(invoiceDto.getPaymentReferenceType().name()));
                assertThat(payment.getMethod()).isEqualTo(PaymentMultipleOptions.MethodEnum.fromValue(invoiceDto.getAccountType().getValue()));
                assertThat(payment.getAccount()).isEqualTo(invoiceDto.getAccountNumber());
                assertThat(payment.getOptions().getFirst().getReference()).isEqualTo(invoiceDto.getPaymentReference());
            });

    }
}
