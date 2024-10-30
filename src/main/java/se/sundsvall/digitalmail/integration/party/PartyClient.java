package se.sundsvall.digitalmail.integration.party;

import static se.sundsvall.digitalmail.integration.party.PartyConfig.INTEGRATION_NAME;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(
	name = INTEGRATION_NAME,
	url = "${integration.party.api-url}",
	configuration = PartyConfig.class)
public interface PartyClient {

	/**
	 * Fetches legalId for a partyId.
	 *
	 * @param  municipalityId municipalityId to fetch legalId for.
	 * @param  partyId        partyId to fetch legalId for.
	 * @return                personId or organizationId.
	 */
	@Cacheable(value = "partyCache")
	@GetMapping(path = "/{municipalityId}/PRIVATE/{partyId}/legalId", produces = MediaType.TEXT_PLAIN_VALUE)
	String getLegalId(@PathVariable("municipalityId") String municipalityId, @PathVariable("partyId") String partyId);

}
