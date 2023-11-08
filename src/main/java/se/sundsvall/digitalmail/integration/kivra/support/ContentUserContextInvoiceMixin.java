package se.sundsvall.digitalmail.integration.kivra.support;

import com.fasterxml.jackson.annotation.JsonProperty;

import generated.com.kivra.ContentUserContextInvoicePaymentOrPaymentMultipleOptions;
/**
 * Simple Jackson mixin to rename the "payment_or_payment_multiple_options" field to "payment", as
 * that is what is expected by the Kivra API
 */
public interface ContentUserContextInvoiceMixin {

    @JsonProperty("payment")
    ContentUserContextInvoicePaymentOrPaymentMultipleOptions getPaymentOrPaymentMultipleOptions();
}
