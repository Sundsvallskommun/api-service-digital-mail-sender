package se.sundsvall.digitalmail.integration.party;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static se.sundsvall.digitalmail.TestObjectFactory.MUNICIPALITY_ID;

import generated.se.sundsvall.party.PartyType;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PartyIntegrationTest {

	@Mock
	private PartyClient partyClient;

	@InjectMocks
	private PartyIntegration partyIntegration;

	@Test
	void getLegalIdForPrivatePersonShouldReturnLegalIdWithoutPrefix() {
		final var legalId = "201308222387"; // From skatteverket test data
		final var partyId = UUID.randomUUID().toString();

		when(partyClient.getLegalId(MUNICIPALITY_ID, PartyType.PRIVATE, partyId)).thenReturn(Optional.of(legalId));

		final var result = partyIntegration.getLegalId(MUNICIPALITY_ID, partyId);

		assertThat(result).contains(legalId);
		verify(partyClient).getLegalId(MUNICIPALITY_ID, PartyType.PRIVATE, partyId);
		verifyNoMoreInteractions(partyClient);
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
		verifyNoMoreInteractions(partyClient);
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
		verifyNoMoreInteractions(partyClient);
	}
}
