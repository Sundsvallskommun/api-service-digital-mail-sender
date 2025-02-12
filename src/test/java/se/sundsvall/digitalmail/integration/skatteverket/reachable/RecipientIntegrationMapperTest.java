package se.sundsvall.digitalmail.integration.skatteverket.reachable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static se.sundsvall.digitalmail.integration.skatteverket.reachable.RecipientIntegrationMapper.SENDER_ORG_NR;

import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.gov.minameddelanden.schema.recipient.AccountStatus;
import se.gov.minameddelanden.schema.recipient.ReachabilityStatus;
import se.gov.minameddelanden.schema.recipient.ServiceSupplier;
import se.gov.minameddelanden.schema.recipient.v3.IsReachableResponse;
import se.sundsvall.digitalmail.integration.skatteverket.SkatteverketProperties;

@ExtendWith(MockitoExtension.class)
class RecipientIntegrationMapperTest {

	@Mock
	private SkatteverketProperties mockSkatteverketProperties;

	@InjectMocks
	private RecipientIntegrationMapper mapper;

	private static Stream<Arguments> provideParametersForGetMailboxSettingsTest() {
		return Stream.of(
			Arguments.of(true, true, false),
			Arguments.of(false, false, false),
			Arguments.of(false, true, false));
	}

	@Test
	void testCreateIsRegistered() {
		final var personalNumber = "197001011234";

		final var isReachableRequest = mapper.createIsReachableRequest(List.of(personalNumber));

		assertThat(isReachableRequest.getSenderOrgNr()).isEqualTo(SENDER_ORG_NR);
		assertThat(isReachableRequest.getRecipientIds().getFirst()).isEqualTo(personalNumber);
	}

	@Test
	void testGetMailboxSettingsShouldReturnRecipientIdWhenPresent() {
		when(mockSkatteverketProperties.supportedSuppliers()).thenReturn(List.of("Kivra"));

		final var response = createIsReachableResponse(false, true, true);

		final var mailboxSettings = mapper.getMailboxSettings(response);

		assertThat(mailboxSettings).hasSize(1);
		assertThat(mailboxSettings.getFirst().serviceAddress()).isEqualTo("https://somewhere.com");
		assertThat(mailboxSettings.getFirst().recipientId()).isEqualTo("recipientId");
	}

	@ParameterizedTest
	@MethodSource("provideParametersForGetMailboxSettingsTest")
	void testGetMailboxSettingsShouldReturnEmpty(final boolean pending, final boolean shouldHaveServiceSupplier, final boolean isAccepted) {
		final var response = createIsReachableResponse(pending, shouldHaveServiceSupplier, isAccepted);

		final var mailboxSettings = mapper.getMailboxSettings(response);

		assertThat(mailboxSettings).isEmpty();
	}

	@Test
	void testFindMatchingSupplier() {
		when(mockSkatteverketProperties.supportedSuppliers()).thenReturn(List.of("supplier1", "supplier2", "supplier3"));

		assertThat(mapper.isSupportedSupplier("supplier1")).isTrue();
		assertThat(mapper.isSupportedSupplier("supplier2")).isTrue();
		assertThat(mapper.isSupportedSupplier("supplier3")).isTrue();
		assertThat(mapper.isSupportedSupplier("unknownSupplier")).isFalse();
	}

	@Test
	void testFindShortSupplierNameShouldMapAndReturnShortName() {
		when(mockSkatteverketProperties.supportedSuppliers()).thenReturn(List.of("Supplier1", "SuPPliEr2"));

		assertThat(mapper.getShortSupplierName("Supplier1")).isEqualTo("supplier1");
		assertThat(mapper.getShortSupplierName("SuPPliEr2")).isEqualTo("supplier2");
	}

	/**
	 *
	 * @param  pending                   if pending, the mailbox has not yet been created and should be interpreted as the
	 *                                   recipient not having a digital mailbox.
	 * @param  shouldHaveServiceSupplier No serviceSupplier indicates that the recipient doesn't have a digital mailbox.
	 * @param  isAccepted                is the sender accepted by the recipient
	 * @return                           a response with the given parameters
	 */
	private IsReachableResponse createIsReachableResponse(final boolean pending,
		final boolean shouldHaveServiceSupplier, final boolean isAccepted) {

		final var accountStatus = new AccountStatus();
		accountStatus.setRecipientId("recipientId");
		accountStatus.setPending(pending);

		if (shouldHaveServiceSupplier) {
			final var serviceSupplier = new ServiceSupplier();
			serviceSupplier.setId("165568402266");
			serviceSupplier.setName("Kivra");
			serviceSupplier.setServiceAdress("https://somewhere.com");

			accountStatus.setServiceSupplier(serviceSupplier);
		}

		final var reachabilityStatus = new ReachabilityStatus();
		reachabilityStatus.setSenderAccepted(isAccepted);
		reachabilityStatus.setAccountStatus(accountStatus);

		final var response = new IsReachableResponse();
		response.getReturns().add(reachabilityStatus);

		return response;
	}
}
