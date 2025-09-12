package apptest;

import static java.lang.String.format;

import apptest.extension.ResponseBodyTransformer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import se.sundsvall.dept44.test.AbstractAppTest;
import se.sundsvall.dept44.test.annotation.wiremock.WireMockAppTestSuite;
import se.sundsvall.digitalmail.Application;

@WireMockAppTestSuite(files = "classpath:/DigitalMailIT/", classes = Application.class)
class DigitalMailIT extends AbstractAppTest {

	private static final String SEND_DIGITAL_MAIL_SERVICE_PATH = "/2281/2120002411/send-digital-mail";
	private static final String MAILBOXES_SERVICE_PATH = "/2281/2120002411/mailboxes";
	
	private static final String REQUEST = "request.json"; 
	private static final String EXPECTED = "expected.json";

	private final String replacementUrl;

	DigitalMailIT(@Value("${wiremock.server.port}") final int wiremockPort) {
		replacementUrl = format("http://localhost:%d/deliversecure", wiremockPort);
	}

	@Test
	void test1_sendDigitalMailToPrivatePerson() {
		setupCall()
			.withExtensions(new ResponseBodyTransformer()
				.withModifier(body -> body.replace("https://mm.kivra.com/service/v3", replacementUrl)))
			.withServicePath(SEND_DIGITAL_MAIL_SERVICE_PATH)
			.withRequest(REQUEST)
			.withHttpMethod(HttpMethod.POST)
			.withExpectedResponseStatus(HttpStatus.OK)
			.withExpectedResponse(EXPECTED)
			.sendRequestAndVerifyResponse()
			.verifyAllStubs();
	}

	/**
	 * Test to verify that we get a 200 OK with 4 mailboxes
	 * 200001102391 / 84f9c52b-8545-486a-9510-9743237bd547  - Should have a mailbox and allow digital mail
	 * 201001212388 / f10545c1-d05d-41f2-b2bc-9dd22241ebc6  - Should have a mailbox but does NOT allow digital mail from the organization
	 * 201301172399 / 831f24d4-2836-4645-a502-035368b43683  - Doesn't have a mailbox
	 * - / 172524d4-ac0a-4fc6-8553-8286e04835dc             - No legal Id found for partyId
	 */
	@Test
	void test2_hasAvailableMailboxes() {
		setupCall()
			.withServicePath(MAILBOXES_SERVICE_PATH)
			.withRequest(REQUEST)
			.withHttpMethod(HttpMethod.POST)
			.withExpectedResponseStatus(HttpStatus.OK)
			.withExpectedResponse(EXPECTED)
			.sendRequestAndVerifyResponse()
			.verifyAllStubs();
	}

	@Test
	void test3_sendDigitalMailToOrganization() {
		setupCall()
			.withExtensions(new ResponseBodyTransformer()
				.withModifier(body -> body.replace("https://mm.kivra.com/service/v3", replacementUrl)))
			.withServicePath(SEND_DIGITAL_MAIL_SERVICE_PATH)
			.withRequest(REQUEST)
			.withHttpMethod(HttpMethod.POST)
			.withExpectedResponseStatus(HttpStatus.OK)
			.withExpectedResponse(EXPECTED)
			.sendRequestAndVerifyResponse()
			.verifyAllStubs();
	}
}
