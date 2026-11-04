package se.sundsvall.digitalmail.integration.kivra;

import generated.com.kivra.ContentUserV2;
import generated.com.kivra.PartsResponsive;
import generated.com.kivra.PaymentMultipleOptions;
import generated.com.kivra.PaymentMultipleOptionsOptionsInner;
import java.util.List;

import static generated.com.kivra.PaymentMultipleOptions.CurrencyEnum.SEK;

final class KivraMapper {

	private KivraMapper() {}

	static ContentUserV2 mapInvoiceToContent(final InvoiceDto invoiceDto) {
		return new ContentUserV2()
			.ssn(invoiceDto.getSsn())
			.subject(invoiceDto.getSubject())
			.type(invoiceDto.getType().getValue())
			.parts(invoiceDto.getFiles().stream()
				.map(file -> new PartsResponsive()
					.name(file.getFilename())
					.contentType(file.getContentType())
					.data(file.getBody()))
				.toList())
			.paymentMultipleOptions(
				new PaymentMultipleOptions()
					.payable(invoiceDto.isPayable())
					.currency(SEK)
					.method(PaymentMultipleOptions.MethodEnum.fromValue(invoiceDto.getAccountType().getValue()))
					.account(invoiceDto.getAccountNumber())
					.options(List.of(new PaymentMultipleOptionsOptionsInner()
						.dueDate(invoiceDto.getDueDate().toString())
						.reference(invoiceDto.getPaymentReference())
						.amount(invoiceDto.getAmount().toString())
						.type(PaymentMultipleOptionsOptionsInner.TypeEnum.fromValue(invoiceDto.getPaymentReferenceType().name())))));
	}
}
