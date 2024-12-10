package se.sundsvall.digitalmail.integration.kivra;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static se.sundsvall.digitalmail.integration.kivra.KivraIntegration.INTEGRATION_NAME;

import generated.com.kivra.ContentUserV2;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(
	name = INTEGRATION_NAME,
	url = "${integration.kivra.api-url}",
	configuration = KivraIntegrationConfiguration.class)
interface KivraClient {

	@PostMapping(value = "/content", consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
	ResponseEntity<ContentUserV2> postContent(@RequestBody ContentUserV2 content);
}
