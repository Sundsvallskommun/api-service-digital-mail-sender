package se.sundsvall.digitalmail.api.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class BodyInformationTests {

    private static final String CONTENT_TYPE = "someContentType";
    private static final String BODY = "someBody";

    @Test
    void gettersAndSetters() {
        final var bodyInformation = new BodyInformation();
        bodyInformation.setContentType(CONTENT_TYPE);
        bodyInformation.setBody(BODY);

        assertThat(bodyInformation.getContentType()).isEqualTo(CONTENT_TYPE);
        assertThat(bodyInformation.getBody()).isEqualTo(BODY);
    }

}
