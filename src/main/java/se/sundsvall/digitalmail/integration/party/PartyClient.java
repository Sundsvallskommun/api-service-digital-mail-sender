package se.sundsvall.digitalmail.integration.party;

import static org.springframework.http.MediaType.TEXT_PLAIN_VALUE;
import static se.sundsvall.digitalmail.integration.party.PartyConfig.INTEGRATION_NAME;

import generated.se.sundsvall.party.PartyType;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import java.util.Optional;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(
	name = INTEGRATION_NAME,
	url = "${integration.party.api-url}",
	configuration = PartyConfig.class,
	dismiss404 = true)
@CircuitBreaker(name = INTEGRATION_NAME)
public interface PartyClient {

	/**
	 * Fetches legalId for a partyId.
	 *
	 * @param  municipalityId municipalityId to fetch legalId for.
	 * @param  partyType      partyType to fetch legalId for (PRIVATE or ENTERPRISE).
	 * @param  partyId        partyId to fetch legalId for.
	 * @return                personId or organizationId.
	 */
	@GetMapping(path = "/{municipalityId}/{partyType}/{partyId}/legalId", produces = TEXT_PLAIN_VALUE)
	Optional<String> getLegalId(@PathVariable String municipalityId, @PathVariable PartyType partyType, @PathVariable String partyId);
}
