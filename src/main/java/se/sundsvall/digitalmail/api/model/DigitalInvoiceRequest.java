package se.sundsvall.digitalmail.api.model;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import java.time.LocalDate;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import se.sundsvall.dept44.common.validators.annotation.ValidUuid;
import se.sundsvall.digitalmail.api.model.validation.annotation.ValidAccountNumber;
import se.sundsvall.digitalmail.domain.invoice.AccountType;
import se.sundsvall.digitalmail.domain.invoice.InvoiceType;
import se.sundsvall.digitalmail.domain.invoice.ReferenceType;

import io.swagger.v3.oas.annotations.media.Schema;

public record DigitalInvoiceRequest(

	@ValidUuid @Schema(description = "partyId for the person or organization the invoice should be sent to", example = "6a5c3d04-412d-11ec-973a-0242ac130003", requiredMode = REQUIRED) String partyId,

	@NotNull @Schema(description = "Invoice type", requiredMode = REQUIRED) InvoiceType type,

	@NotBlank @Schema(description = "The invoice subject", example = "Faktura från Sundsvalls kommun", requiredMode = REQUIRED) String subject,

	@Schema(description = "Invoice reference", example = "Faktura #12345") String reference,

	@Schema(description = "Whether the invoice is payable", defaultValue = "true") Boolean payable,

	@NotNull @Valid @Schema(requiredMode = REQUIRED) Details details,

	@NotEmpty List<@Valid File> files) {

	@Schema(description = "Invoice details")
	public record Details(

		@NotNull @Positive @Schema(description = "The invoice amount", example = "123.45", requiredMode = REQUIRED) Float amount,

		@NotNull @Schema(description = "The invoice due date", example = "2023-10-09", requiredMode = REQUIRED) LocalDate dueDate,

		@NotNull @Schema(requiredMode = REQUIRED) ReferenceType paymentReferenceType,

		@NotBlank @Schema(description = "The payment reference number", maxLength = 25, example = "426523791", requiredMode = REQUIRED) String paymentReference,

		@NotNull @Schema(requiredMode = REQUIRED) AccountType accountType,

		@ValidAccountNumber @Schema(description = "The receiving account (a valid BANKGIRO or PLUSGIRO number)", example = "12345", requiredMode = REQUIRED) String accountNumber) {}
}
