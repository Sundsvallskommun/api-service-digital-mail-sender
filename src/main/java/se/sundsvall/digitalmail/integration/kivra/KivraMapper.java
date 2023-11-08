package se.sundsvall.digitalmail.integration.kivra;

import static generated.com.kivra.Payment.CurrencyEnum.SEK;

import java.time.format.DateTimeFormatter;

import generated.com.kivra.ContentUser;
import generated.com.kivra.ContentUserContext;
import generated.com.kivra.ContentUserContextInvoice;
import generated.com.kivra.File;
import generated.com.kivra.Payment;

final class KivraMapper {

    static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private KivraMapper() { }

    static ContentUser mapInvoiceToContent(final InvoiceDto invoiceDto) {
        return new ContentUser()
            .ssn(invoiceDto.getSsn())
            .subject(invoiceDto.getSubject())
            .generatedAt(DATE_FORMATTER.format(invoiceDto.getGeneratedAt()))
            .type(invoiceDto.getType().getValue())
            .files(invoiceDto.getFiles().stream()
                .map(file -> new File()
                    .name(file.getFilename())
                    .contentType(file.getContentType())
                    .data(file.getBody())
                )
                .toList())
            .context(new ContentUserContext()
                .invoice(new ContentUserContextInvoice()
                    .invoiceReference(invoiceDto.getReference())
                    .paymentOrPaymentMultipleOptions(new Payment()
                        .payable(true)
                        .currency(SEK)
                        .dueDate(DATE_FORMATTER.format(invoiceDto.getDueDate()))
                        .totalOwed(invoiceDto.getAmount())
                        .type(Payment.TypeEnum.fromValue(invoiceDto.getPaymentReferenceType().name()))
                        .method(Payment.MethodEnum.fromValue(invoiceDto.getAccountType().getValue()))
                        .account(invoiceDto.getAccountNumber())
                        .reference(invoiceDto.getPaymentReference()))));
    }
}
