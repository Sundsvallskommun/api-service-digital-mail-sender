package se.sundsvall.digitalmail.api.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class SupportInfoTests {

    @Test
    void gettersAndSetters() {
        final var supportText = "someText";
        final var contactInformationEmail = "someEmailAddress";
        final var contactInformationUrl = "someUrl";
        final var contactInformationPhoneNumber = "somePhoneNumber";

        final var supportInfo = new SupportInfo();
        supportInfo.setSupportText(supportText);
        supportInfo.setContactInformationEmail(contactInformationEmail);
        supportInfo.setContactInformationUrl(contactInformationUrl);
        supportInfo.setContactInformationPhoneNumber(contactInformationPhoneNumber);

        assertThat(supportInfo.getSupportText()).isEqualTo(supportText);
        assertThat(supportInfo.getContactInformationEmail()).isEqualTo(contactInformationEmail);
        assertThat(supportInfo.getContactInformationUrl()).isEqualTo(contactInformationUrl);
        assertThat(supportInfo.getContactInformationPhoneNumber()).isEqualTo(contactInformationPhoneNumber);
    }
}
