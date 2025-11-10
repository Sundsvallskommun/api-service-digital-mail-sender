package se.sundsvall.digitalmail.integration.messaging;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static se.sundsvall.digitalmail.integration.messaging.MessagingConfig.INTEGRATION_NAME;

import generated.se.sundsvall.messaging.EmailRequest;
import generated.se.sundsvall.messaging.MessageResult;
import generated.se.sundsvall.messaging.SlackRequest;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@CircuitBreaker(name = INTEGRATION_NAME)
@FeignClient(name = INTEGRATION_NAME, url = "${integration.messaging.api-url}", configuration = MessagingConfig.class)
public interface MessagingClient {

	/**
	 * Sends a single e-mail
	 *
	 * @param  request containing email information
	 * @return         response containing id and delivery results for the message that was sent
	 */
	@PostMapping(path = "/{municipalityId}/email", produces = APPLICATION_JSON_VALUE, consumes = APPLICATION_JSON_VALUE)
	MessageResult sendEmail(@PathVariable("municipalityId") String municipalityId, @RequestBody EmailRequest request);

	/**
	 * Sends a Slack message
	 *
	 * @param  slackRequest containing message information
	 * @return              response containing id and delivery results for the message that was sent
	 */
	@PostMapping(path = "/{municipalityId}/slack", produces = APPLICATION_JSON_VALUE, consumes = APPLICATION_JSON_VALUE)
	MessageResult sendSlackMessage(@PathVariable("municipalityId") String municipalityId, @RequestBody SlackRequest slackRequest);
}
