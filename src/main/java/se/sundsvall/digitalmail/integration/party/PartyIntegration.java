package se.sundsvall.digitalmail.integration.party;

import static java.util.Optional.ofNullable;

import generated.se.sundsvall.party.PartyType;
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
		var optionalLegalId = partyClient.getLegalId(municipalityId, PartyType.PRIVATE, partyId)
			.or(() -> partyClient.getLegalId(municipalityId, PartyType.ENTERPRISE, partyId));

		return optionalLegalId.flatMap(PartyIntegration::prefixOrgNr);

	}

	/**
	 * Prefix legal Id with "16" if it's an organization number, i.e. has length 10.
	 *
	 * @param  legalId the legal Id to prefix
	 * @return         organization number prefixed with "16". Otherwise return the legal Id as is.
	 */
	public static Optional<String> prefixOrgNr(final String legalId) {
		return ofNullable(legalId)
			.filter(string -> string.length() == 10)
			.map(string -> "16" + string)
			.or(() -> ofNullable(legalId));
	}
}
