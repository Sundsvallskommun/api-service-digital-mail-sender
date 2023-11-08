package se.sundsvall.digitalmail.integration;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class OAuth2Tests {

    @Test
    void testCreation() {
        var oAuth2 = new OAuth2("someTokenUrl", "someClientId", "someClientSecret", "someAuthorizationGrantType");

        assertThat(oAuth2.tokenUrl()).isEqualTo("someTokenUrl");
        assertThat(oAuth2.clientId()).isEqualTo("someClientId");
        assertThat(oAuth2.clientSecret()).isEqualTo("someClientSecret");
        assertThat(oAuth2.authorizationGrantType()).isEqualTo("someAuthorizationGrantType");
    }
}
