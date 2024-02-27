package se.sundsvall.digitalmail.integration.kivra;


import static generated.com.kivra.PaymentMultipleOptions.CurrencyEnum.SEK;

import java.time.format.DateTimeFormatter;
import java.util.List;

import generated.com.kivra.ContentUser;
import generated.com.kivra.ContentUserContext;
import generated.com.kivra.ContentUserContextInvoice;
import generated.com.kivra.File;
import generated.com.kivra.PaymentMultipleOptions;
import generated.com.kivra.PaymentMultipleOptionsOptionsInner;

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
              .paymentOrPaymentMultipleOptions(
                new PaymentMultipleOptions()
                .payable(true)
                .currency(SEK)
                .method(PaymentMultipleOptions.MethodEnum.fromValue(invoiceDto.getAccountType().getValue()))
                .account(invoiceDto.getAccountNumber())
                .options(List.of(new PaymentMultipleOptionsOptionsInner()
                  .dueDate(invoiceDto.getDueDate().toString())
                  .reference(invoiceDto.getPaymentReference())
                  .amount(invoiceDto.getAmount())
                  .type(PaymentMultipleOptionsOptionsInner.TypeEnum.fromValue(invoiceDto.getPaymentReferenceType().name())))))));


    }
}
