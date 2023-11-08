package se.sundsvall.digitalmail.domain.invoice;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "The receiving account type", example = "BANKGIRO")
public enum AccountType {
    BANKGIRO("1"),
    PLUSGIRO("2");

    private final String value;

    AccountType(final String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
