package se.sundsvall.digitalmail.integration.citizenmapping;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(
    name = "CitizenMappingClient",
    url = "${integration.citizenmapping.api-url}",
    configuration = CitizenMappingConfig.class
)
public interface CitizenMappingClient {
    
    /**
     * Fetches legalId for a partyId.
     * @param partyId
     * @return personId or organizationId.
     */
    @Cacheable(value = "citizenMappingCache")
    @GetMapping(path = "/{partyId}/personalnumber", produces = MediaType.TEXT_PLAIN_VALUE)
    String getCitizenMapping(@PathVariable("partyId") String partyId);
}
