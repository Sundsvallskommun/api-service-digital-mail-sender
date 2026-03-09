package se.sundsvall.digitalmail.integration.party;

import generated.se.sundsvall.party.PartyType;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.TEXT_PLAIN_VALUE;
import static se.sundsvall.digitalmail.integration.party.PartyConfig.INTEGRATION_NAME;

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

	/**
	 * Fetches legalIds for multiple partyIds in batch.
	 *
	 * @param  municipalityId municipalityId to fetch legalIds for.
	 * @param  partyIds       list of partyIds to fetch legalIds for.
	 * @return                map of partyId to legalId.
	 */
	@PostMapping(path = "/{municipalityId}/PRIVATE/legalIds", consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
	Map<String, String> getLegalIds(@PathVariable String municipalityId, @RequestBody List<String> partyIds);
}
