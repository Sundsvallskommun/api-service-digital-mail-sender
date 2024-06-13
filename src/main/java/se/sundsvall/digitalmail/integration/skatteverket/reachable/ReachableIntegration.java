package se.sundsvall.digitalmail.integration.skatteverket.reachable;

import static org.zalando.problem.Status.INTERNAL_SERVER_ERROR;

import java.util.List;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.ws.client.core.WebServiceTemplate;
import org.zalando.problem.Problem;

import se.sundsvall.digitalmail.integration.skatteverket.MailboxDto;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import se.gov.minameddelanden.schema.recipient.v3.IsReachableResponse;

@Component
@CircuitBreaker(name = "reachableIntegration")
public class ReachableIntegration {
    
    private final WebServiceTemplate isReachableTemplate;
    private final RecipientIntegrationMapper mapper;
    
    ReachableIntegration(
            @Qualifier("skatteverketIsReachableWebserviceTemplate") final WebServiceTemplate isReachableTemplate,
            final RecipientIntegrationMapper mapper) {
        this.isReachableTemplate = isReachableTemplate;
        this.mapper = mapper;
    }
    
    /**
     * Fetches a mailbox and if a mailbox is reachable.
     * @param personalNumbers
     * @return
     */
    public List<MailboxDto> isReachable(final List<String> personalNumbers) {
        try {
            // Call Skatteverket to see which mailbox(es) (if any) the person has
            final var isReachableRequest = mapper.createIsReachableRequest(personalNumbers);

            final var isReachableResponse = (IsReachableResponse) isReachableTemplate.marshalSendAndReceive(isReachableRequest);

            return mapper.getMailboxSettings(isReachableResponse);
        } catch (Exception e) {
            throw Problem.builder()
                .withTitle("Error while getting digital mailbox from skatteverket")
                .withStatus(INTERNAL_SERVER_ERROR)
                .withDetail(e.getMessage())
                .build();
        }
    }
}
