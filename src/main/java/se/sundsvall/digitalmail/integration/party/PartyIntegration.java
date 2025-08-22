package se.sundsvall.digitalmail.integration.party;

import java.util.Optional;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

@Component
public class PartyIntegration {

	private final PartyClient partyClient;

	public PartyIntegration(final PartyClient partyClient) {
		this.partyClient = partyClient;
	}

	@Cacheable(value = "partyCache")
	public Optional<String> getLegalId(final String municipalityId, final String partyId) {
		return partyClient.getLegalId(municipalityId, partyId);
	}

}
