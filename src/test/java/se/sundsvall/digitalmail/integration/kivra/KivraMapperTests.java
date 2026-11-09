package se.sundsvall.digitalmail.integration.kivra;

import generated.com.kivra.PaymentMultipleOptions;
import generated.com.kivra.PaymentMultipleOptionsOptionsInner;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import se.sundsvall.digitalmail.api.model.DigitalInvoiceRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static se.sundsvall.digitalmail.TestObjectFactory.generateInvoiceDto;
import static se.sundsvall.digitalmail.TestObjectFactory.generateInvoiceRequest;
import static se.sundsvall.digitalmail.integration.kivra.KivraMapper.DATE_FORMATTER;
import static se.sundsvall.digitalmail.integration.kivra.KivraMapper.formatAmount;
import static se.sundsvall.digitalmail.integration.kivra.KivraMapper.mapInvoiceToContent;

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

	@Test
	void mapInvoiceToContentTrimsFieldsAndFormatsAmount() {
		final var request = generateInvoiceRequest();
		final var requestWithSpaces = new DigitalInvoiceRequest(
			request.partyId(),
			request.type(),
			request.subject(),
			request.reference(),
			request.payable(),
			new DigitalInvoiceRequest.Details(10_000_000f, LocalDate.now(), "SE_OCR", " 12345 ", "BANKGIRO", " 991-1884 "),
			request.files());

		final var payment = mapInvoiceToContent(new InvoiceDto(requestWithSpaces)).getPaymentMultipleOptions();

		assertThat(payment.getAccount()).isEqualTo("991-1884");
		assertThat(payment.getOptions().getFirst().getReference()).isEqualTo("12345");
		assertThat(payment.getOptions().getFirst().getAmount()).isEqualTo("10000000.00");
	}

	@ParameterizedTest
	@CsvSource({
		"123.45, 123.45",
		"123.5, 123.50",
		"100, 100.00",
		"10000000, 10000000.00"
	})
	void formatAmountUsesTwoDecimalsWithoutExponent(final Float amount, final String expected) {
		assertThat(formatAmount(amount)).isEqualTo(expected);
	}
}
