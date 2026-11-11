package se.sundsvall.digitalmail.integration.kivra;

import generated.com.kivra.ContentUserV2;
import generated.com.kivra.PartsResponsive;
import generated.com.kivra.PaymentMultipleOptions;
import generated.com.kivra.PaymentMultipleOptionsOptionsInner;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static generated.com.kivra.PaymentMultipleOptions.CurrencyEnum.SEK;
import static org.apache.commons.lang3.StringUtils.trim;

final class KivraMapper {

	static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

	private KivraMapper() {}

	static ContentUserV2 mapInvoiceToContent(final InvoiceDto invoiceDto) {
		return new ContentUserV2()
			.ssn(invoiceDto.getSsn())
			.subject(invoiceDto.getSubject())
			.generatedAt(DATE_FORMATTER.format(invoiceDto.getGeneratedAt()))
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
					.account(trim(invoiceDto.getAccountNumber()))
					.options(List.of(new PaymentMultipleOptionsOptionsInner()
						.dueDate(invoiceDto.getDueDate().toString())
						.reference(trim(invoiceDto.getPaymentReference()))
						.amount(formatAmount(invoiceDto.getAmount()))
						.type(PaymentMultipleOptionsOptionsInner.TypeEnum.fromValue(invoiceDto.getPaymentReferenceType().name())))));
	}

	/**
	 * Formats the amount with two decimals and without exponent notation
	 */
	static String formatAmount(final Float amount) {
		return new BigDecimal(amount.toString())
			.setScale(2, RoundingMode.HALF_UP)
			.toPlainString();
	}
}
