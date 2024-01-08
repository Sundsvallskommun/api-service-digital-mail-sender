package apptest;

import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.OK;

import org.junit.jupiter.api.Test;

import se.sundsvall.dept44.test.AbstractAppTest;
import se.sundsvall.dept44.test.annotation.wiremock.WireMockAppTestSuite;
import se.sundsvall.digitalmail.DigitalMail;

@WireMockAppTestSuite(files = "classpath:/AvailabilityIT/", classes = DigitalMail.class)
class AvailabilityIT extends AbstractAppTest {

    private static final String SERVICE_PATH = "/has-available-mailbox/6a5c3d04-412d-11ec-973a-0242ac130003";

    @Test
    void test1_successfulRequest() {
        setupCall()
            .withServicePath(SERVICE_PATH)
            .withHttpMethod(POST)
            .withExpectedResponseStatus(OK)
            .sendRequestAndVerifyResponse()
            .verifyAllStubs();
    }

    @Test
    void test2_noMatchInParty() {
        setupCall()
            .withServicePath(SERVICE_PATH)
            .withHttpMethod(POST)
            .withExpectedResponseStatus(NOT_FOUND)
            .sendRequestAndVerifyResponse()
            .verifyAllStubs();
    }

    @Test
    void test3_recipient_senderIsNotAccepted() {
        setupCall()
            .withServicePath(SERVICE_PATH)
            .withHttpMethod(POST)
            .withExpectedResponseStatus(NOT_FOUND)
            .sendRequestAndVerifyResponse()
            .verifyAllStubs();
    }

    @Test
    void test4_recipient_serviceSupplierIsNull() {
        setupCall()
            .withServicePath(SERVICE_PATH)
            .withHttpMethod(POST)
            .withExpectedResponseStatus(NOT_FOUND)
            .sendRequestAndVerifyResponse()
            .verifyAllStubs();
    }

    @Test
    void test5_recipient_pendingIsTrue() {
        setupCall()
            .withServicePath(SERVICE_PATH)
            .withHttpMethod(POST)
            .withExpectedResponseStatus(NOT_FOUND)
            .sendRequestAndVerifyResponse()
            .verifyAllStubs();
    }

    @Test
    void test6_blankServiceSupplierServiceAddress() {
        setupCall()
            .withServicePath(SERVICE_PATH)
            .withHttpMethod(POST)
            .withExpectedResponseStatus(NOT_FOUND)
            .sendRequestAndVerifyResponse()
            .verifyAllStubs();
    }

    @Test
    void test7_recipient_unsupportedSupplier() {
        setupCall()
            .withServicePath(SERVICE_PATH)
            .withHttpMethod(POST)
            .withExpectedResponseStatus(NOT_FOUND)
            .sendRequestAndVerifyResponse()
            .verifyAllStubs();
    }
}
