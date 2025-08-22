package se.sundsvall.digitalmail.integration.skatteverket.reachable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.stream.Stream;
import org.assertj.core.groups.Tuple;
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
import se.sundsvall.digitalmail.integration.skatteverket.MailboxDto;
import se.sundsvall.digitalmail.integration.skatteverket.SkatteverketProperties;

@ExtendWith(MockitoExtension.class)
class RecipientIntegrationMapperTest {

	@Mock
	private SkatteverketProperties mockSkatteverketProperties;

	@InjectMocks
	private RecipientIntegrationMapper mapper;

	@Test
	void testCreateIsRegistered() {
		final var personalNumber = "197001011234";
		final var organizationNumber = "2120002411";

		final var isReachableRequest = mapper.createIsReachableRequest(List.of(personalNumber), organizationNumber);

		assertThat(isReachableRequest.getSenderOrgNr()).isEqualTo(organizationNumber);
		assertThat(isReachableRequest.getRecipientIds().getFirst()).isEqualTo(personalNumber);

		verifyNoInteractions(mockSkatteverketProperties);
	}

	@Test
	void testToMailboxDtoListShouldReturnRecipientIdWhenPresent() {
		when(mockSkatteverketProperties.supportedSuppliers()).thenReturn(List.of("Kivra"));

		final var response = createIsReachableResponse(false, true, true);

		final var mailboxSettings = mapper.toMailboxDtoList(response);

		assertThat(mailboxSettings).hasSize(1);
		assertThat(mailboxSettings.getFirst().getServiceAddress()).isEqualTo("https://somewhere.com");
		assertThat(mailboxSettings.getFirst().getRecipientId()).isEqualTo("recipientId");

		verify(mockSkatteverketProperties).supportedSuppliers();
	}

	@Test
	void testFindMatchingSupplier() {
		when(mockSkatteverketProperties.supportedSuppliers()).thenReturn(List.of("supplier1", "supplier2", "supplier3"));

		assertThat(mapper.isSupportedSupplier("supplier1")).isTrue();
		assertThat(mapper.isSupportedSupplier("supplier2")).isTrue();
		assertThat(mapper.isSupportedSupplier("supplier3")).isTrue();
		assertThat(mapper.isSupportedSupplier("unknownSupplier")).isFalse();

		verify(mockSkatteverketProperties, times(4)).supportedSuppliers();
	}

	@Test
	void testFindShortSupplierNameShouldMapAndReturnShortName() {
		when(mockSkatteverketProperties.supportedSuppliers()).thenReturn(List.of("Supplier1", "SuPPliEr2"));

		assertThat(mapper.getShortSupplierName("Supplier1")).isEqualTo("supplier1");
		assertThat(mapper.getShortSupplierName("SuPPliEr2")).isEqualTo("supplier2");

		verify(mockSkatteverketProperties, times(2)).supportedSuppliers();
	}

	@Test
	void testToMailboxDtoListShouldReturnEmptyListWhenEmptyResponse() {
		final var response = new IsReachableResponse();
		final var mailboxSettings = mapper.toMailboxDtoList(response);

		assertThat(mailboxSettings).isEmpty();

		verifyNoInteractions(mockSkatteverketProperties);
	}

	@Test
	void testToMailboxDtoListWhenNotSupportedServiceSupplier() {
		when(mockSkatteverketProperties.supportedSuppliers()).thenReturn(List.of("anotherSupplier"));
		final var mailboxSettings = mapper.toMailboxDtoList(createIsReachableResponse(false, true, true));

		assertThat(mailboxSettings).extracting(MailboxDto::getRecipientId, MailboxDto::getServiceAddress, MailboxDto::getServiceName, MailboxDto::isValidMailbox)
			.containsExactlyInAnyOrder(
				Tuple.tuple("recipientId", null, null, false));

		verify(mockSkatteverketProperties).supportedSuppliers();
	}

	@ParameterizedTest
	@MethodSource("reachableResponseProvider")
	void testToMailboxSettingsWhenInvalidMailbox(IsReachableResponse response) {
		final var mailboxSettings = mapper.toMailboxDtoList(response);

		assertThat(mailboxSettings).extracting(MailboxDto::getRecipientId, MailboxDto::getServiceAddress, MailboxDto::getServiceName, MailboxDto::isValidMailbox)
			.containsExactlyInAnyOrder(
				Tuple.tuple("recipientId", null, null, false));

		verifyNoInteractions(mockSkatteverketProperties);
	}

	private static Stream<Arguments> reachableResponseProvider() {
		return Stream.of(
			Arguments.of(createIsReachableResponse(false, true, false)),   // Doesn't accept the sender
			Arguments.of(createIsReachableResponse(true, true, false)),    // Pending mailbox
			Arguments.of(createIsReachableResponse(false, false, true)));  // No service supplier
	}

	/**
	 *
	 * @param  pending                   if pending, the mailbox has not yet been created and should be interpreted as the
	 *                                   recipient not having a digital mailbox.
	 * @param  shouldHaveServiceSupplier No serviceSupplier indicates that the recipient doesn't have a digital mailbox.
	 * @param  isAccepted                is the sender accepted by the recipient
	 * @return                           a response with the given parameters
	 */
	private static IsReachableResponse createIsReachableResponse(final boolean pending,
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
