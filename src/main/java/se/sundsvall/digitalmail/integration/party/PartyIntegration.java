package se.sundsvall.digitalmail.integration.party;

import generated.se.sundsvall.party.PartyType;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import static java.util.Optional.ofNullable;

@Component
public class PartyIntegration {

	private final PartyClient partyClient;

	public PartyIntegration(final PartyClient partyClient) {
		this.partyClient = partyClient;
	}

	@Cacheable(value = "partyCache")
	public Optional<String> getLegalId(final String municipalityId, final String partyId) {
		final var optionalLegalId = partyClient.getLegalId(municipalityId, PartyType.PRIVATE, partyId)
			.or(() -> partyClient.getLegalId(municipalityId, PartyType.ENTERPRISE, partyId));

		return optionalLegalId.flatMap(PartyIntegration::prefixOrgNr);
	}

	/**
	 * Fetches legalIds for multiple partyIds in batch using the PRIVATE batch endpoint.
	 * For partyIds not found via PRIVATE, falls back to individual ENTERPRISE lookups.
	 * The returned map contains all input partyIds as keys, with null values for those not found.
	 *
	 * @param  municipalityId municipalityId to fetch legalIds for.
	 * @param  partyIds       list of partyIds to fetch legalIds for.
	 * @return                map of partyId to legalId (null value if not found).
	 */
	public Map<String, String> getLegalIds(final String municipalityId, final List<String> partyIds) {
		final var result = new HashMap<String, String>();

		// Batch lookup for PRIVATE party type
		final var batchResult = partyClient.getLegalIds(municipalityId, partyIds);

		// Process batch results and apply prefixOrgNr
		batchResult.forEach((partyId, legalId) -> result.put(partyId, prefixOrgNr(legalId).orElse(null)));

		// For partyIds not found in batch, try ENTERPRISE individually
		partyIds.stream()
			.filter(partyId -> !batchResult.containsKey(partyId))
			.forEach(partyId -> partyClient.getLegalId(municipalityId, PartyType.ENTERPRISE, partyId)
				.flatMap(PartyIntegration::prefixOrgNr)
				.ifPresentOrElse(
					legalId -> result.put(partyId, legalId),
					() -> result.put(partyId, null)));

		return result;
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
