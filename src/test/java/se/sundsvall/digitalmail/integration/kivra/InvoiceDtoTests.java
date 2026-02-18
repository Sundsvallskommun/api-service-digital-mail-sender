package se.sundsvall.digitalmail.integration.kivra;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static se.sundsvall.digitalmail.TestObjectFactory.generateInvoiceRequest;

class InvoiceDtoTests {

	@Test
	void creationAndGetters() {
		final var invoiceRequest = generateInvoiceRequest();
		final var invoiceDto = new InvoiceDto(invoiceRequest);

		assertThat(invoiceDto).hasNoNullFieldsOrPropertiesExcept("ssn");
	}

	@Test
	void ssnSetterAndGetter() {
		final var ssn = "someSsn";
		final var invoiceRequest = generateInvoiceRequest();
		final var invoiceDto = new InvoiceDto(invoiceRequest);

		assertThat(invoiceDto.getSsn()).isNull();
		invoiceDto.setSsn(ssn);
		assertThat(invoiceDto.getSsn()).isEqualTo(ssn);
	}
}
