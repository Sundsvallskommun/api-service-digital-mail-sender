package se.sundsvall.digitalmail.integration.skatteverket.sendmail;

import static org.zalando.problem.Status.INTERNAL_SERVER_ERROR;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.ws.client.core.WebServiceTemplate;
import org.zalando.problem.Problem;
import org.zalando.problem.ThrowableProblem;

import se.sundsvall.digitalmail.api.model.DigitalMailResponse;
import se.sundsvall.digitalmail.integration.skatteverket.DigitalMailDto;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import se.gov.minameddelanden.schema.service.v3.DeliverSecureResponse;

@Component
@CircuitBreaker(name = "digitalMailIntegration")
public class DigitalMailIntegration {
    
    private static final Logger LOG = LoggerFactory.getLogger(DigitalMailIntegration.class);
    
    private final WebServiceTemplate distributeTemplate;
    private final DigitalMailMapper mapper;
    
    @Autowired
    DigitalMailIntegration(
            @Qualifier("skatteverket-sendmail-webservice-template") final WebServiceTemplate distributeTemplate,
            final DigitalMailMapper mapper) {
        this.distributeTemplate = distributeTemplate;
        this.mapper = mapper;
    }
    
    /**
     * Send a digital mail
     *
     * @param requestDto
     * @param serviceAddress
     * @return
     */
    public DigitalMailResponse sendDigitalMail(final DigitalMailDto requestDto, final String serviceAddress) {
        LOG.debug("Trying to send secure digital mail.");
    
        try {
            final var deliverSecureRequest = mapper.createDeliverSecure(requestDto);

            final var deliverSecureResponse = (DeliverSecureResponse) distributeTemplate.marshalSendAndReceive(serviceAddress, deliverSecureRequest);

            return mapper.createDigitalMailResponse(deliverSecureResponse, requestDto.getPartyId());
        } catch (Exception e) {
            // Might come from interceptor
            if (e instanceof ThrowableProblem) {
                throw e;
            }
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
            LOG.error("Couldn't get cause", e);
            return Problem.builder()
                    .withDetail("Couldn't get cause")
                    .build();
        }
    }
}
