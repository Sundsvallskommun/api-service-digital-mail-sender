package se.sundsvall.digitalmail.integration.kivra;

import static java.util.Optional.ofNullable;

import java.time.LocalDate;
import java.util.List;

import se.sundsvall.digitalmail.api.model.DigitalInvoiceRequest;
import se.sundsvall.digitalmail.api.model.File;
import se.sundsvall.digitalmail.domain.invoice.AccountType;
import se.sundsvall.digitalmail.domain.invoice.InvoiceType;
import se.sundsvall.digitalmail.domain.invoice.ReferenceType;

import lombok.Getter;

@Getter
public class InvoiceDto {

	private final LocalDate generatedAt = LocalDate.now();
	private final String partyId;
	private final InvoiceType type;
	private final String subject;
	private final String reference;
	private final boolean payable;
	private final Float amount;
	private final LocalDate dueDate;
	private final ReferenceType paymentReferenceType;
	private final String paymentReference;
	private final AccountType accountType;
	private final String accountNumber;
	private final List<File> files;

	private String ssn;

	public InvoiceDto(final DigitalInvoiceRequest invoiceRequest) {
		partyId = invoiceRequest.partyId();
		type = invoiceRequest.type();
		subject = invoiceRequest.subject();
		reference = invoiceRequest.reference();
		payable = ofNullable(invoiceRequest.payable()).orElse(true);
		amount = invoiceRequest.details().amount();
		dueDate = invoiceRequest.details().dueDate();
		paymentReferenceType = invoiceRequest.details().paymentReferenceType();
		paymentReference = invoiceRequest.details().paymentReference();
		accountType = invoiceRequest.details().accountType();
		accountNumber = invoiceRequest.details().accountNumber();
		files = invoiceRequest.files();
	}

	public void setSsn(final String ssn) {
		this.ssn = ssn;
	}
}
