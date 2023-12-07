package se.sundsvall.digitalmail.api.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.TEXT_PLAIN_VALUE;

import java.util.List;

import org.junit.jupiter.api.Test;

class DigitalMailRequestTests {

    @Test
    void gettersAndSetters() {
        final var partyId = "somePartyId";
        final var municipalityId = "someMunicipalityId";
        final var headerSubject = "someSubject";

        final var digitalMailRequest = new DigitalMailRequest();
        digitalMailRequest.setPartyId(partyId);
        digitalMailRequest.setMunicipalityId(municipalityId);
        digitalMailRequest.setHeaderSubject(headerSubject);
        digitalMailRequest.setSupportInfo(new SupportInfo());
        digitalMailRequest.setBodyInformation(BodyInformation.builder().withContentType(TEXT_PLAIN_VALUE).build());
        digitalMailRequest.setAttachments(List.of(new File(), new File()));

        assertThat(digitalMailRequest.getPartyId()).isEqualTo(partyId);
        assertThat(digitalMailRequest.getMunicipalityId()).isEqualTo(municipalityId);
        assertThat(digitalMailRequest.getHeaderSubject()).isEqualTo(headerSubject);
        assertThat(digitalMailRequest.getSupportInfo()).isNotNull();
        assertThat(digitalMailRequest.getBodyInformation()).isNotNull();
        assertThat(digitalMailRequest.getAttachments()).isNotNull().hasSize(2);
    }
}
