package se.sundsvall.digitalmail.api.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.TEXT_HTML_VALUE;
import static org.springframework.http.MediaType.TEXT_PLAIN_VALUE;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class BodyInformationTests {

    private static final String CONTENT_TYPE = "someContentType";
    private static final String BODY = "someBody";

    @Nested
    class PlainTextTests {

        @Test
        void gettersAndSetters() {
            final var bodyInformation = new BodyInformation.PlainText();
            bodyInformation.setBody(BODY);

            assertThat(bodyInformation.getContentType()).isEqualTo(TEXT_PLAIN_VALUE);
            assertThat(bodyInformation.getBody()).isEqualTo(BODY);
        }
    }

    @Nested
    class HtmlTests {

        @Test
        void gettersAndSetters() {
            final var bodyInformation = new BodyInformation.Html();
            bodyInformation.setBody(BODY);

            assertThat(bodyInformation.getContentType()).isEqualTo(TEXT_HTML_VALUE);
            assertThat(bodyInformation.getBody()).isEqualTo(BODY);
        }
    }

    @Nested
    class UnknownTests {

        @Test
        void gettersAndSetters() {
            final var bodyInformation = new BodyInformation.Unknown();
            bodyInformation.setContentType(CONTENT_TYPE);
            bodyInformation.setBody(BODY);

            assertThat(bodyInformation.getContentType()).isEqualTo(CONTENT_TYPE);
            assertThat(bodyInformation.getBody()).isEqualTo(BODY);
        }
    }
}
