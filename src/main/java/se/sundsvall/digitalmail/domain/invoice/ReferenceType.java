package se.sundsvall.digitalmail.domain.invoice;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "The payment reference type", examples = "SE_OCR")
public enum ReferenceType {
	SE_OCR,
	TENANT_REF
}
