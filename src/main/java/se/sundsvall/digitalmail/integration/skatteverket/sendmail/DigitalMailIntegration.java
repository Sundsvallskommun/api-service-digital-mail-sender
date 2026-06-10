package se.sundsvall.digitalmail.integration.skatteverket.sendmail;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.ws.client.core.WebServiceTemplate;
import se.gov.minameddelanden.schema.service.v3.DeliverSecureResponse;
import se.sundsvall.dept44.problem.Problem;
import se.sundsvall.dept44.problem.ThrowableProblem;
import se.sundsvall.digitalmail.api.model.DigitalMailResponse;
import se.sundsvall.digitalmail.integration.skatteverket.DigitalMailDto;

import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;

@Component
@Slf4j
@CircuitBreaker(name = "digitalMailIntegration")
public class DigitalMailIntegration {

	private final WebServiceTemplate distributeTemplate;
	private final DigitalMailMapper mapper;

	@Autowired
	DigitalMailIntegration(
		@Qualifier("skatteverketSendmailWebserviceTemplate") final WebServiceTemplate distributeTemplate,
		final DigitalMailMapper mapper) {
		this.distributeTemplate = distributeTemplate;
		this.mapper = mapper;
	}

	/**
	 * Send a digital mail
	 *
	 * @param  requestDto     The request to send
	 * @param  serviceAddress The address to send the mail to
	 * @return                The response from the service
	 */
	public DigitalMailResponse sendDigitalMail(final DigitalMailDto requestDto, final String serviceAddress) {
		log.debug("Trying to send secure digital mail.");

		try {
			log.info("Creating deliver secure request");
			final var deliverSecureRequest = mapper.createDeliverSecure(requestDto);

			log.info("Sending deliver secure request");
			final var deliverSecureResponse = (DeliverSecureResponse) distributeTemplate.marshalSendAndReceive(serviceAddress, deliverSecureRequest);

			log.info("Mapping deliver secure response");
			return mapper.createDigitalMailResponse(deliverSecureResponse, requestDto.getPartyId());
		} catch (Exception e) {
			// Might come from interceptor
			if (e instanceof ThrowableProblem) {
				log.error("Failed to send digital mail", e);
				throw e;
			}

			// Needed to get stacktraces
			log.error("Failed to send digital mail", e);
			final var cause = getProblemCause(e);

			throw Problem.builder()
				.withCause(cause)
				.withDetail(e.getMessage())
				.withStatus(INTERNAL_SERVER_ERROR)
				.withTitle("Couldn't send secure digital mail")
				.build();
		}
	}

	// If we get an error parsing XML we can't use ".getCause()", really special case..
	ThrowableProblem getProblemCause(final Exception e) {
		try {
			return (ThrowableProblem) e.getCause();
		} catch (Exception ex) {
			log.error("Couldn't get cause", e);
			return Problem.builder()
				.withDetail("Couldn't get cause").withStatus(INTERNAL_SERVER_ERROR)
				.build();
		}
	}
}
