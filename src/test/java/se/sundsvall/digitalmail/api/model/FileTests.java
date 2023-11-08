package se.sundsvall.digitalmail.api.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class FileTests {

    @Test
    void gettersAndSetters() {
        final var filename = "someFilename";
        final var body = "someBody";
        final var contentType = "someContentType";

        final var file = new File();
        file.setFilename(filename);
        file.setBody(body);
        file.setContentType(contentType);

        assertThat(file.getFilename()).isEqualTo(filename);
        assertThat(file.getBody()).isEqualTo(body);
        assertThat(file.getContentType()).isEqualTo(contentType);
    }
}
