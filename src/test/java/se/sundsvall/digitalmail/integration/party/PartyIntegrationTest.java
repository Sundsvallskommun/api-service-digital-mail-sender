package se.sundsvall.digitalmail.integration.party;

import generated.se.sundsvall.party.PartyType;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static se.sundsvall.digitalmail.TestObjectFactory.MUNICIPALITY_ID;

@ExtendWith(MockitoExtension.class)
class PartyIntegrationTest {

	@Mock
	private PartyClient partyClient;

	@Mock
	private PartyProperties partyProperties;

	@InjectMocks
	private PartyIntegration partyIntegration;

	@AfterEach
	void tearDown() {
		verifyNoMoreInteractions(partyClient, partyProperties);
	}

	@Test
	void getLegalIdForPrivatePersonShouldReturnLegalIdWithoutPrefix() {
		final var legalId = "201308222387"; // From skatteverket test data
		final var partyId = UUID.randomUUID().toString();

		when(partyClient.getLegalId(MUNICIPALITY_ID, PartyType.PRIVATE, partyId)).thenReturn(Optional.of(legalId));

		final var result = partyIntegration.getLegalId(MUNICIPALITY_ID, partyId);

		assertThat(result).contains(legalId);
		verify(partyClient).getLegalId(MUNICIPALITY_ID, PartyType.PRIVATE, partyId);
	}

	@Test
	void getLegalIdForEnterpriseShouldReturnPrefixedLegalId() {
		final var partyId = UUID.randomUUID().toString();
		final var legalId = "5591628136";
		final var prefixedLegalId = "165591628136";

		when(partyClient.getLegalId(MUNICIPALITY_ID, PartyType.PRIVATE, partyId)).thenReturn(Optional.empty());
		when(partyClient.getLegalId(MUNICIPALITY_ID, PartyType.ENTERPRISE, partyId)).thenReturn(Optional.of(legalId));

		final var result = partyIntegration.getLegalId(MUNICIPALITY_ID, partyId);

		assertThat(result).contains(prefixedLegalId);
		verify(partyClient).getLegalId(MUNICIPALITY_ID, PartyType.PRIVATE, partyId);
		verify(partyClient).getLegalId(MUNICIPALITY_ID, PartyType.ENTERPRISE, partyId);
	}

	@Test
	void getLegalIdNotFound() {
		final var partyId = UUID.randomUUID().toString();

		when(partyClient.getLegalId(MUNICIPALITY_ID, PartyType.PRIVATE, partyId)).thenReturn(Optional.empty());
		when(partyClient.getLegalId(MUNICIPALITY_ID, PartyType.ENTERPRISE, partyId)).thenReturn(Optional.empty());

		final var result = partyIntegration.getLegalId(MUNICIPALITY_ID, partyId);

		assertThat(result).isEmpty();
		verify(partyClient).getLegalId(MUNICIPALITY_ID, PartyType.PRIVATE, partyId);
		verify(partyClient).getLegalId(MUNICIPALITY_ID, PartyType.ENTERPRISE, partyId);
	}

	@Test
	void getLegalIdsAllFoundViaBatch() {
		final var partyId1 = UUID.randomUUID().toString();
		final var partyId2 = UUID.randomUUID().toString();
		final var legalId1 = "201308222387";
		final var legalId2 = "199001011234";
		final var partyIds = List.of(partyId1, partyId2);

		when(partyProperties.maxPartyIdsPerCall()).thenReturn(5);
		when(partyClient.getLegalIds(MUNICIPALITY_ID, partyIds)).thenReturn(Map.of(partyId1, legalId1, partyId2, legalId2));

		final var result = partyIntegration.getLegalIds(MUNICIPALITY_ID, partyIds);

		assertThat(result)
			.hasSize(2)
			.containsEntry(partyId1, legalId1)
			.containsEntry(partyId2, legalId2);
		verify(partyClient).getLegalIds(MUNICIPALITY_ID, partyIds);
		verify(partyProperties, times(2)).maxPartyIdsPerCall();
	}

	@Test
	void getLegalIdsMissingPartyIdFallsBackToEnterprise() {
		final var partyId1 = UUID.randomUUID().toString();
		final var partyId2 = UUID.randomUUID().toString();
		final var legalId1 = "201308222387";
		final var orgNr = "5591628136";
		final var prefixedOrgNr = "165591628136";
		final var partyIds = List.of(partyId1, partyId2);

		when(partyProperties.maxPartyIdsPerCall()).thenReturn(5);
		// partyId1 found via batch, partyId2 not
		when(partyClient.getLegalIds(MUNICIPALITY_ID, partyIds)).thenReturn(Map.of(partyId1, legalId1));
		// partyId2 found via ENTERPRISE fallback
		when(partyClient.getLegalId(MUNICIPALITY_ID, PartyType.ENTERPRISE, partyId2)).thenReturn(Optional.of(orgNr));

		final var result = partyIntegration.getLegalIds(MUNICIPALITY_ID, partyIds);

		assertThat(result)
			.hasSize(2)
			.containsEntry(partyId1, legalId1)
			.containsEntry(partyId2, prefixedOrgNr);
		verify(partyClient).getLegalIds(MUNICIPALITY_ID, partyIds);
		verify(partyClient).getLegalId(MUNICIPALITY_ID, PartyType.ENTERPRISE, partyId2);
		verify(partyProperties, times(2)).maxPartyIdsPerCall();
	}

	@Test
	void getLegalIdsNoneFound() {
		final var partyId1 = UUID.randomUUID().toString();
		final var partyId2 = UUID.randomUUID().toString();
		final var partyIds = List.of(partyId1, partyId2);

		when(partyProperties.maxPartyIdsPerCall()).thenReturn(5);
		when(partyClient.getLegalIds(MUNICIPALITY_ID, partyIds)).thenReturn(Map.of());
		when(partyClient.getLegalId(MUNICIPALITY_ID, PartyType.ENTERPRISE, partyId1)).thenReturn(Optional.empty());
		when(partyClient.getLegalId(MUNICIPALITY_ID, PartyType.ENTERPRISE, partyId2)).thenReturn(Optional.empty());

		final var result = partyIntegration.getLegalIds(MUNICIPALITY_ID, partyIds);

		assertThat(result)
			.hasSize(2)
			.containsEntry(partyId1, null)
			.containsEntry(partyId2, null);
		verify(partyClient).getLegalIds(MUNICIPALITY_ID, partyIds);
		verify(partyClient).getLegalId(MUNICIPALITY_ID, PartyType.ENTERPRISE, partyId1);
		verify(partyClient).getLegalId(MUNICIPALITY_ID, PartyType.ENTERPRISE, partyId2);
		verify(partyProperties, times(2)).maxPartyIdsPerCall();
	}

	@Test
	void getLegalIdsBatchReturnsPrefixableOrgNr() {
		final var partyId = UUID.randomUUID().toString();
		final var orgNr = "5591628136";
		final var prefixedOrgNr = "165591628136";
		final var partyIds = List.of(partyId);

		when(partyProperties.maxPartyIdsPerCall()).thenReturn(5);
		when(partyClient.getLegalIds(MUNICIPALITY_ID, partyIds)).thenReturn(Map.of(partyId, orgNr));

		final var result = partyIntegration.getLegalIds(MUNICIPALITY_ID, partyIds);

		assertThat(result)
			.hasSize(1)
			.containsEntry(partyId, prefixedOrgNr);
		verify(partyClient).getLegalIds(MUNICIPALITY_ID, partyIds);
		verify(partyProperties, times(2)).maxPartyIdsPerCall();
	}

	@Test
	void getLegalIdsChunkingWhenPartyIdsExceedMaxPerCall() {
		final var numberOfPartyIds = 2543;
		final var partyIds = IntStream.range(0, numberOfPartyIds)
			.mapToObj(i -> "partyId-" + i)
			.toList();

		when(partyProperties.maxPartyIdsPerCall()).thenReturn(1000);
		when(partyClient.getLegalIds(anyString(), anyList()))
			.thenAnswer(invocation -> {
				final var ids = invocation.<List<String>>getArgument(1);
				return ids.stream()
					.collect(Collectors.toMap(Function.identity(), id -> "legalId-" + id));
			});

		final var result = partyIntegration.getLegalIds(MUNICIPALITY_ID, partyIds);

		assertThat(result).hasSize(numberOfPartyIds);
		verify(partyClient, times(3)).getLegalIds(anyString(), anyList());
		verify(partyProperties, times(6)).maxPartyIdsPerCall();
	}
}
