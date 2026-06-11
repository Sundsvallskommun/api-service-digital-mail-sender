package se.sundsvall.digitalmail.domain.invoice;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Getter
@Schema(description = "The receiving account type", examples = "BANKGIRO")
public enum AccountType {
	BANKGIRO("1"),
	PLUSGIRO("2");

	private final String value;

	AccountType(final String value) {
		this.value = value;
	}
}
