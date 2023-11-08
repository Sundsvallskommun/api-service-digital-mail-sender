package se.sundsvall.digitalmail.domain.invoice;

import static org.assertj.core.api.Assertions.assertThat;
import static se.sundsvall.digitalmail.domain.invoice.AccountType.BANKGIRO;
import static se.sundsvall.digitalmail.domain.invoice.AccountType.PLUSGIRO;

import org.junit.jupiter.api.Test;

class AccountTypeTests {

    @Test
    void values() {
        assertThat(AccountType.values()).containsExactly(BANKGIRO, PLUSGIRO);

        assertThat(BANKGIRO.getValue()).isEqualTo("1");
        assertThat(PLUSGIRO.getValue()).isEqualTo("2");
    }
}
