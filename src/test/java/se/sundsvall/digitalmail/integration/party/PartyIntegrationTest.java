package se.sundsvall.digitalmail.integration.party;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.AfterEach;
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

	@AfterEach
	void afterEach() {
		verifyNoMoreInteractions(partyClient);
	}

	@Test
	void getLegalId() {
		var partyId = "1234567890";
		var municipalityId = "2281";
		var legalId = "1234567890";

		when(partyClient.getLegalId(municipalityId, partyId)).thenReturn(legalId);

		var result = partyIntegration.getLegalId(municipalityId, partyId);

		assertThat(result).isEqualTo(legalId);
		verify(partyClient).getLegalId(municipalityId, partyId);
	}
}
