package se.sundsvall.digitalmail.integration.citizen;

import static se.sundsvall.digitalmail.integration.citizen.CitizenConfig.INTEGRATION_NAME;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(
    name = INTEGRATION_NAME,
    url = "${integration.citizen.api-url}",
    configuration = CitizenConfig.class
)
public interface CitizenClient {
    
    /**
     * Fetches legalId for a partyId.
     * @param partyId
     * @return personId or organizationId.
     */
    @Cacheable(value = "citizenCache")
    @GetMapping(path = "/{partyId}/personnumber", produces = MediaType.TEXT_PLAIN_VALUE)
    String getPersonNumber(@PathVariable("partyId") String partyId);
}
