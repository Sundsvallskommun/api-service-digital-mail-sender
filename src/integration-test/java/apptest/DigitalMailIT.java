package apptest;

import static java.lang.String.format;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;

import se.sundsvall.dept44.test.AbstractAppTest;
import se.sundsvall.dept44.test.annotation.wiremock.WireMockAppTestSuite;
import se.sundsvall.digitalmail.DigitalMail;

import apptest.extension.ResponseBodyTransformer;

@WireMockAppTestSuite(files = "classpath:/DigitalMailIT/", classes = DigitalMail.class)
class DigitalMailIT extends AbstractAppTest {
    
    private static final String SERVICE_PATH = "/send-digital-mail";

    private final String replacementUrl;

    DigitalMailIT(@Value("${wiremock.server.port}") final int wiremockPort) {
        replacementUrl = format("http://localhost:%d/deliversecure", wiremockPort);
    }

    @Test
    void test1_successfulRequest() {
        setupCall()
            .withExtensions(new ResponseBodyTransformer()
                .withModifier(body -> body.replace("https://mm.kivra.com/service/v3", replacementUrl)))
            .withServicePath(SERVICE_PATH)
            .withRequest("request.json")
            .withHttpMethod(HttpMethod.POST)
            .withExpectedResponseStatus(HttpStatus.OK)
            .withExpectedResponse("expected.json")
            .sendRequestAndVerifyResponse()
            .verifyAllStubs();
    }
}
