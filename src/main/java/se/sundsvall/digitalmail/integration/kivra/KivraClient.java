package se.sundsvall.digitalmail.integration.kivra;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static se.sundsvall.digitalmail.integration.kivra.KivraIntegration.INTEGRATION_NAME;

import generated.com.kivra.ContentUserV2;
import generated.com.kivra.TenantV2;
import generated.com.kivra.UserMatchV2SSN;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(
	name = INTEGRATION_NAME,
	url = "${integration.kivra.api-url}",
	configuration = KivraIntegrationConfiguration.class)
@CircuitBreaker(name = INTEGRATION_NAME)
interface KivraClient {

	@PostMapping(value = "/content", consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
	ResponseEntity<ContentUserV2> postContent(@RequestBody ContentUserV2 content);

	@PostMapping(value = "/usermatch/ssn", consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
	ResponseEntity<UserMatchV2SSN> postUserMatchSSN(@RequestBody UserMatchV2SSN userMatchV2SSN);

	/**
	 * Method is used to verify that the certificate to Kivra is valid
	 *
	 * @return basic tenant information
	 */
	@GetMapping(produces = APPLICATION_JSON_VALUE)
	ResponseEntity<TenantV2> getTenantInformation();
}
