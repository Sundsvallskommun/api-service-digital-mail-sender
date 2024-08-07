package apptest;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;

import se.sundsvall.dept44.test.AbstractAppTest;
import se.sundsvall.dept44.test.annotation.wiremock.WireMockAppTestSuite;
import se.sundsvall.digitalmail.DigitalMail;

@WireMockAppTestSuite(files = "classpath:/DigitalInvoiceIT/", classes = DigitalMail.class)
class DigitalInvoiceIT extends AbstractAppTest {

	private static final String SERVICE_PATH = "/2281/send-digital-invoice";

	@Test
	void test1_successfulRequest() {
		setupCall()
			.withServicePath(SERVICE_PATH)
			.withRequest("request.json")
			.withHttpMethod(HttpMethod.POST)
			.withExpectedResponseStatus(HttpStatus.OK)
			.withExpectedResponse("expected.json")
			.sendRequestAndVerifyResponse()
			.verifyAllStubs();
	}

}
