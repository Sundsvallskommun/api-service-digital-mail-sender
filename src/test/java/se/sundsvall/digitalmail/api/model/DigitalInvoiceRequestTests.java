package se.sundsvall.digitalmail.api.model;

import static org.assertj.core.api.Assertions.assertThat;
import static se.sundsvall.digitalmail.domain.invoice.AccountType.BANKGIRO;
import static se.sundsvall.digitalmail.domain.invoice.InvoiceType.INVOICE;
import static se.sundsvall.digitalmail.domain.invoice.ReferenceType.SE_OCR;

import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class DigitalInvoiceRequestTests {

	@Test
	void creationAndGetters() {
		final var partyId = "somePartyId";
		final var type = INVOICE;
		final var subject = "someSubject";
		final var reference = "someReference";
		final var amount = 123.45f;
		final var dueDate = LocalDate.now();
		final var paymentReference = "somePaymentReference";
		final var account = "someAccount";
		final var details = new DigitalInvoiceRequest.Details(amount, dueDate, SE_OCR,
			paymentReference, BANKGIRO, account);
		final var contentType = "someContentType";
		final var body = "someBody";
		final var filename = "someFilename";
		final var file = new File(contentType, body, filename);

		final var request = new DigitalInvoiceRequest(partyId, type, subject,
			reference, true, details, List.of(file));

		assertThat(request.partyId()).isEqualTo(partyId);
		assertThat(request.type()).isEqualTo(type);
		assertThat(request.subject()).isEqualTo(subject);
		assertThat(request.reference()).isEqualTo(reference);
		assertThat(request.payable()).isTrue();
		assertThat(request.details()).isEqualTo(details);
		assertThat(request.files()).hasSize(1).satisfies(files -> {
			assertThat(file.getContentType()).isEqualTo(contentType);
			assertThat(file.getBody()).isEqualTo(body);
			assertThat(file.getFilename()).isEqualTo(filename);
		});
	}
}
