package apptest;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import se.sundsvall.dept44.test.AbstractAppTest;
import se.sundsvall.dept44.test.annotation.wiremock.WireMockAppTestSuite;
import se.sundsvall.digitalmail.Application;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.OK;

@WireMockAppTestSuite(files = "classpath:/DigitalMailIT/", classes = Application.class)
class DigitalMailIT extends AbstractAppTest {

	private static final String SEND_DIGITAL_MAIL_SERVICE_PATH = "/2281/2120002411/send-digital-mail";
	private static final String MAILBOXES_SERVICE_PATH = "/2281/2120002411/mailboxes";

	private static final String REQUEST = "request.json";
	private static final String EXPECTED = "expected.json";

	@Test
	void test1_sendDigitalMailToPrivatePerson() {
		setupCall()
			.withServicePath(SEND_DIGITAL_MAIL_SERVICE_PATH)
			.withRequest(REQUEST)
			.withHttpMethod(HttpMethod.POST)
			.withExpectedResponseStatus(OK)
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
			.withExpectedResponseStatus(OK)
			.withExpectedResponse(EXPECTED)
			.sendRequestAndVerifyResponse()
			.verifyAllStubs();
	}

	@Test
	void test3_sendDigitalMailToOrganization() {
		setupCall()
			.withServicePath(SEND_DIGITAL_MAIL_SERVICE_PATH)
			.withRequest(REQUEST)
			.withHttpMethod(HttpMethod.POST)
			.withExpectedResponseStatus(OK)
			.withExpectedResponse(EXPECTED)
			.sendRequestAndVerifyResponse()
			.verifyAllStubs();
	}

	@Test
	void test4_sendDigitalMailNoAvailableMailboxes() {
		setupCall()
			.withServicePath(SEND_DIGITAL_MAIL_SERVICE_PATH)
			.withRequest(REQUEST)
			.withHttpMethod(HttpMethod.POST)
			.withExpectedResponseStatus(NOT_FOUND)
			.withExpectedResponse(EXPECTED)
			.sendRequestAndVerifyResponse()
			.verifyAllStubs();
	}
}
