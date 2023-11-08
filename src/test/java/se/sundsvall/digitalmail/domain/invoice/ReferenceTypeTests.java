package se.sundsvall.digitalmail.domain.invoice;

import static org.assertj.core.api.Assertions.assertThat;
import static se.sundsvall.digitalmail.domain.invoice.ReferenceType.SE_OCR;
import static se.sundsvall.digitalmail.domain.invoice.ReferenceType.TENANT_REF;

import org.junit.jupiter.api.Test;

class ReferenceTypeTests {

    @Test
    void values() {
        assertThat(ReferenceType.values()).containsExactly(SE_OCR, TENANT_REF);
    }
}
